package si.um.feri.projketRRI.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.VisUI;

import si.um.feri.projketRRI.Projekt;
import si.um.feri.projketRRI.api.calls.ApiClient;
import si.um.feri.projketRRI.api.calls.HiveService;
import si.um.feri.projketRRI.api.calls.HiveWeightService;
import si.um.feri.projketRRI.api.calls.LocationService;
import si.um.feri.projketRRI.api.calls.model.Hive;
import si.um.feri.projketRRI.api.calls.model.Location;
import si.um.feri.projketRRI.screens.mapUi.MapAddHiveUi;
import si.um.feri.projketRRI.screens.mapUi.MapFilterUI;
import si.um.feri.projketRRI.utils.CameraInputController;
import si.um.feri.projketRRI.utils.Constants;
import si.um.feri.projketRRI.utils.Geolocation;
import si.um.feri.projketRRI.utils.MapRasterTiles;
import si.um.feri.projketRRI.utils.MarkerLayer;
import si.um.feri.projketRRI.utils.RasterTileMap;
import si.um.feri.projketRRI.api.calls.model.HiveWeight;

public class RasterMapScreen extends ScreenAdapter implements GestureDetector.GestureListener {

    private final Projekt game;

    private OrthographicCamera camera;
    private MapAddHiveUi addHiveUI;

    // center geolocation
    private Geolocation centerGeolocation = new Geolocation(46.4845641435028, 15.649055286737594);

    // data
    private final Array<Hive> hives = new Array<>();
    private final IntMap<Location> locationsById = new IntMap<>();
    private final Array<Geolocation> markers = new Array<>();
    private int pendingLocations = 0;
    private boolean loading = false;

    private RasterTileMap tileMap;
    private MarkerLayer markerLayer;
    private CameraInputController cameraController;

    private final int ZOOM_BG = Constants.ZOOM;

    private Geolocation selectedMarker = null;

    private IntMap<Array<HiveWeight>> weightsByHiveId = new IntMap<>();
    private boolean weightsLoading = false;

    private MapFilterUI filterUI;

    private MapFilterUI.StatusFilter statusFilter = MapFilterUI.StatusFilter.ALL;
    private MapFilterUI.TypeFilter typeFilter = MapFilterUI.TypeFilter.ALL;

    private MarkerLayer layerAZ;
    private MarkerLayer layerLR;
    private MarkerLayer layerDB;

    private MarkerLayer particlesOnlineLayer;

    private final Array<Geolocation> markersAZ = new Array<>();
    private final Array<Geolocation> markersLR = new Array<>();
    private final Array<Geolocation> markersDB = new Array<>();
    private final Array<Geolocation> markersOnline = new Array<>();
    private MapAddHiveUi addHiveUi;
    private boolean isAddMode = false;
    private Geolocation tempMarker = null;
    private MarkerLayer previewLayer;
    private Viewport viewport;
    private Stage uiStage;
    private Skin uiSkin;



    public RasterMapScreen(Projekt game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();

        viewport = new FitViewport(Constants.MAP_WIDTH, Constants.MAP_HEIGHT, camera);

        viewport.apply(true);
        camera.position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, 0);
        camera.zoom = 2f;
        camera.update();

        tileMap = new RasterTileMap();
        tileMap.setTileZoom(ZOOM_BG);

        layerAZ = new MarkerLayer("Images/hive.png", null);
        layerLR = new MarkerLayer("Images/hive.png", null);
        layerDB = new MarkerLayer("Images/hive.png", null);

        previewLayer = new MarkerLayer("Images/hive.png", null);
        previewLayer.setMapParams(ZOOM_BG, Constants.NUM_TILES);

        layerAZ.setMapParams(ZOOM_BG, Constants.NUM_TILES);
        layerLR.setMapParams(ZOOM_BG, Constants.NUM_TILES);
        layerDB.setMapParams(ZOOM_BG, Constants.NUM_TILES);

        particlesOnlineLayer = new MarkerLayer("Images/hive.png", "Particles/beeSmall.p");
        particlesOnlineLayer.setMapParams(ZOOM_BG, Constants.NUM_TILES);

        cameraController = new CameraInputController(camera);

        tileMap.rebuild(centerGeolocation);

        InputMultiplexer mux = new InputMultiplexer();

        if (!VisUI.isLoaded()) VisUI.load();

        //uiSkin = VisUI.getSkin();
        uiSkin = new Skin(Gdx.files.internal("uiskin.json"));

        uiStage = new Stage(new ScreenViewport());
        filterUI = new MapFilterUI(uiSkin);
        addHiveUi = new MapAddHiveUi(uiSkin);

        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().right().pad(20);
        hud.padTop(120);

        hud.add(filterUI.getRoot()).right().row();
        hud.row().padTop(10);
        hud.add(addHiveUi.getRoot()).right().row();

        uiStage.addActor(hud);


        filterUI.setFilterListener((status, type) -> {
            statusFilter = status;
            typeFilter = type;

            rebuildMarkersByTypeAndOnline();

            if (particlesOnlineLayer != null) {
                particlesOnlineLayer.syncParticlesToMarkers(markersOnline.size);
            }
        });

        addHiveUi.setListener(new MapAddHiveUi.AddHiveListener() {
            @Override
            public void onModeChanged(boolean mode) {
                isAddMode = mode;
                if (!mode) tempMarker = null;
            }

            @Override
            public void onSave(String name, String type, String status, String locationDesc) {
                if (tempMarker == null) return;

                Gdx.app.log("API", "Creating hive...");
                ApiClient.createHive(name, type, status, locationDesc, tempMarker.lat, tempMarker.lng,
                    new ApiClient.HiveCallback() {
                        @Override
                        public void onSuccess() {
                            Gdx.app.postRunnable(() -> {
                                Gdx.app.log("API", "Success!");
                                addHiveUi.reset();
                                tempMarker = null;
                                loadHivesAndLocations();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            Gdx.app.postRunnable(() -> {
                                Gdx.app.log("API", "Error: " + message);
                            });
                        }
                    }
                );
            }
        });
        mux.addProcessor(uiStage);

        mux.addProcessor(new GestureDetector(this));
        mux.addProcessor(new GestureDetector(cameraController));

        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom += amountY * 0.2f;
                return true;
            }
        });

        Gdx.input.setInputProcessor(mux);

        loadHivesAndLocations();
    }

    @Override
    public void render(float delta) {
        viewport.apply();
        ScreenUtils.clear(0, 0, 0, 1);

        clampCameraToMap();
        camera.update();

        tileMap.render(camera);
        layerAZ.draw(camera, tileMap.getBeginTile(), markersAZ, delta);
        layerLR.draw(camera, tileMap.getBeginTile(), markersLR, delta);
        layerDB.draw(camera, tileMap.getBeginTile(), markersDB, delta);
        particlesOnlineLayer.draw(camera, tileMap.getBeginTile(), markersOnline, delta);


        if (isAddMode && tempMarker != null) {
            Array<Geolocation> tempArr = new Array<>();
            tempArr.add(tempMarker);

            previewLayer.draw(camera, tileMap.getBeginTile(), tempArr, delta);
        }
        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();
    }

    private void loadHivesAndLocations() {
        loading = true;
        hives.clear();
        locationsById.clear();
        markers.clear();

        HiveService.loadHives(new HiveService.HiveCallback() {
            @Override
            public void onSuccess(Array<Hive> result) {
                hives.addAll(result);

                if (hives.size == 0) {
                    loading = false;
                    Gdx.app.log("HIVES", "No hives returned.");
                    return;
                }

                pendingLocations = 0;

                for (Hive hive : hives) {
                    final int locId = hive.id_location;

                    if (locationsById.containsKey(locId)) continue;

                    pendingLocations++;

                    LocationService.loadLocation(locId, new LocationService.LocationCallback() {
                        @Override
                        public void onSuccess(Location location) {
                            locationsById.put(location.id, location);
                            pendingLocations--;
                            if (pendingLocations == 0) onAllLocationsLoaded();
                        }

                        @Override
                        public void onError(String message) {
                            pendingLocations--;
                            Gdx.app.log("LOCATIONS", "Failed for id=" + locId + " : " + message);
                            if (pendingLocations == 0) onAllLocationsLoaded();
                        }
                    });
                }

                if (pendingLocations == 0) {
                    onAllLocationsLoaded();
                }
            }

            @Override
            public void onError(String message) {
                loading = false;
                Gdx.app.log("HIVES", "Load hives failed: " + message);
            }
        });
    }

    private void loadHiveWeightsAsync() {
        weightsLoading = true;

        new Thread(() -> {
            try {
                IntMap<Array<HiveWeight>> grouped = HiveWeightService.loadGroupedByHive();

                Gdx.app.postRunnable(() -> {
                    weightsByHiveId.clear();
                    weightsByHiveId.putAll(grouped);
                    weightsLoading = false;

                    Gdx.app.log("WEIGHTS", "Loaded weights for hives: " + weightsByHiveId.size);
                });

            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    weightsLoading = false;
                    Gdx.app.log("WEIGHTS", "Failed to load hive weights: " + e.getMessage(), e);
                });
            }
        }).start();
    }


    private void onAllLocationsLoaded() {
        loading = false;
        rebuildMarkersByTypeAndOnline();

        if (markersAZ.size + markersLR.size + markersDB.size > 0) {
            centerOnVisibleMarkersAverage();
            tileMap.setTileZoom(ZOOM_BG);
            tileMap.rebuild(centerGeolocation);
        }

        particlesOnlineLayer.syncParticlesToMarkers(markersOnline.size);

        loadHiveWeightsAsync();

        Gdx.app.log("MAP", "Markers on map: " + markers.size);
    }


    private void rebuildMarkersByTypeAndOnline() {
        markersAZ.clear();
        markersLR.clear();
        markersDB.clear();
        markersOnline.clear();

        for (Hive hive : hives) {
            if (!matchesStatus(hive, statusFilter)) continue;
            if (!matchesType(hive, typeFilter)) continue;

            Location loc = locationsById.get(hive.id_location);
            if (loc == null) continue;

            Geolocation g = new Geolocation(loc.latitude, loc.longitude);

            String type = hive.type != null ? hive.type.trim().toLowerCase() : "";
            if (type.equals("az")) markersAZ.add(g);
            else if (type.equals("lr")) markersLR.add(g);
            else if (type.equals("db")) markersDB.add(g);

            if (isOnline(hive.status)) markersOnline.add(g);
        }

        particlesOnlineLayer.syncParticlesToMarkers(markersOnline.size);
    }

    private boolean isOnline(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("online") || s.equals("true") || s.equals("1") || s.equals("on");
    }

    private boolean matchesType(Hive hive, MapFilterUI.TypeFilter filter) {
        if (filter == MapFilterUI.TypeFilter.ALL) return true;

        String t = hive.type != null ? hive.type.trim().toLowerCase() : "";
        if (filter == MapFilterUI.TypeFilter.AZ) return t.equals("az");
        if (filter == MapFilterUI.TypeFilter.LR) return t.equals("lr");
        if (filter == MapFilterUI.TypeFilter.DB) return t.equals("db");
        return true;
    }

    private boolean matchesStatus(Hive hive, MapFilterUI.StatusFilter filter) {
        if (filter == MapFilterUI.StatusFilter.ALL) return true;

        if (hive == null || hive.status == null) return false;

        String s = hive.status.trim().toLowerCase();

        boolean online =
            s.equals("online") ||
                s.equals("on") ||
                s.equals("1") ||
                s.equals("true");

        if (filter == MapFilterUI.StatusFilter.ONLINE) {
            return online;
        }
        return !online;
    }



    private void centerOnVisibleMarkersAverage() {
        int count = markersAZ.size + markersLR.size + markersDB.size;
        if (count == 0) return;

        double sumLat = 0;
        double sumLng = 0;

        for (Geolocation m : markersAZ) { sumLat += m.lat; sumLng += m.lng; }
        for (Geolocation m : markersLR) { sumLat += m.lat; sumLng += m.lng; }
        for (Geolocation m : markersDB) { sumLat += m.lat; sumLng += m.lng; }

        centerGeolocation = new Geolocation(sumLat / count, sumLng / count);
    }

    private void clampCameraToMap() {
        camera.zoom = MathUtils.clamp(camera.zoom, 0.2f, 1f);

        float mapPixelWidth = Constants.NUM_TILES * si.um.feri.projketRRI.utils.MapRasterTiles.TILE_SIZE;
        float mapPixelHeight = Constants.NUM_TILES * si.um.feri.projketRRI.utils.MapRasterTiles.TILE_SIZE;

        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        camera.position.x = MathUtils.clamp(
            camera.position.x,
            effectiveViewportWidth / 2f,
            mapPixelWidth - effectiveViewportWidth / 2f
        );

        camera.position.y = MathUtils.clamp(
            camera.position.y,
            effectiveViewportHeight / 2f,
            mapPixelHeight - effectiveViewportHeight / 2f
        );
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        Vector3 world = new Vector3(x, y, 0);
        camera.unproject(world);

        if (isAddMode) {
            Geolocation g = getGeolocationFromPixel(
                world.x, world.y
            );

            addHiveUi.updateCoordinates(g.lat, g.lng);

            tempMarker = g;

            return true;
        }

        float hitRadius = 40f;

        for (Hive hive : hives) {
            Location loc = locationsById.get(hive.id_location);
            if (loc == null) continue;

            Geolocation g = new Geolocation(loc.latitude, loc.longitude);

            Vector2 p = MapRasterTiles.getPixelPosition(
                g.lat, g.lng,
                tileMap.getBeginTile().x,
                tileMap.getBeginTile().y
            );

            if (p.dst(world.x, world.y) <= hitRadius) {
                openHiveDetail(hive, loc);
                return true;
            }
        }
        return false;
    }

    private void openHiveDetail(Hive hive, Location loc) {
        Array<HiveWeight> weights = weightsByHiveId.get(hive.id);

        if (weights == null) {
            weights = new Array<>();
        }

        game.setScreen(
            new HiveDetailMapScreen(game, hive, loc,markers, weights)
        );
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.update();

        if (uiStage != null) {
            uiStage.getViewport().update(width, height, true);
        }
    }


    @Override public boolean touchDown(float x, float y, int pointer, int button) { return false; }
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float velocityX, float velocityY, int button) { return false; }
    @Override public boolean pan(float x, float y, float deltaX, float deltaY) { return false; }
    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
    @Override public boolean zoom(float initialDistance, float distance) { return false; }
    @Override public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) { return false; }
    @Override public void pinchStop() { }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (filterUI != null) filterUI.dispose();
        if (markerLayer != null) markerLayer.dispose();
        if (addHiveUi != null) addHiveUi.dispose();
        if (tileMap != null) tileMap.dispose();
        if (uiStage != null) uiStage.dispose();
        if (uiSkin != null) uiSkin.dispose();
    }

    private Geolocation getGeolocationFromPixel(float worldX, float worldY) {
        double tileSize = si.um.feri.projketRRI.utils.MapRasterTiles.TILE_SIZE;
        double zoomFactor = Math.pow(2, Constants.ZOOM);

        double globalPixelX = (tileMap.getBeginTile().x * tileSize) + worldX;

        double mapHeightTiles = Constants.NUM_TILES;
        double globalPixelY = ((tileMap.getBeginTile().y + mapHeightTiles) * tileSize) - worldY;

        double mapSize = tileSize * zoomFactor;
        double normalizedX = globalPixelX / mapSize;
        double normalizedY = globalPixelY / mapSize;

        double lon = normalizedX * 360.0 - 180.0;
        double n = Math.PI - 2.0 * Math.PI * normalizedY;
        double lat = Math.toDegrees(Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));

        return new Geolocation(lat, lon);
    }
}
