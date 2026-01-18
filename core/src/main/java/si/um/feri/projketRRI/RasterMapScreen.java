package si.um.feri.projketRRI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;

import si.um.feri.projketRRI.ApiCalls.HiveService;
import si.um.feri.projketRRI.ApiCalls.LocationService;
import si.um.feri.projketRRI.Models.Hive;
import si.um.feri.projketRRI.Models.Location;
import si.um.feri.projketRRI.utils.CameraInputController;
import si.um.feri.projketRRI.utils.Constants;
import si.um.feri.projketRRI.utils.DetailPanel;
import si.um.feri.projketRRI.utils.HiveDetailMapScreen;
import si.um.feri.projketRRI.utils.Geolocation;
import si.um.feri.projketRRI.utils.MapRasterTiles;
import si.um.feri.projketRRI.utils.MarkerLayer;
import si.um.feri.projketRRI.utils.RasterTileMap;

public class RasterMapScreen extends ScreenAdapter implements GestureDetector.GestureListener {

    private final Projekt game;

    private OrthographicCamera camera;

    // center geolocation
    private Geolocation centerGeolocation = new Geolocation(46.4845641435028, 15.649055286737594);

    // data
    private final Array<Hive> hives = new Array<>();
    private final IntMap<Location> locationsById = new IntMap<>();
    private final Array<Geolocation> markers = new Array<>();
    private int pendingLocations = 0;
    private boolean loading = false;

    // helpers
    private RasterTileMap tileMap;
    private MarkerLayer markerLayer;
    private CameraInputController cameraController;

    private final int ZOOM_BG = 9;

    private Geolocation selectedMarker = null;

    public RasterMapScreen(Projekt game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.MAP_WIDTH, Constants.MAP_HEIGHT);
        camera.position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, 0);
        camera.viewportWidth = Constants.MAP_WIDTH / 2f;
        camera.viewportHeight = Constants.MAP_HEIGHT / 2f;
        camera.zoom = 2f;
        camera.update();

        tileMap = new RasterTileMap();
        tileMap.setTileZoom(ZOOM_BG);

        markerLayer = new MarkerLayer("Images/hive.png", "Particles/beeSmall.p");
        markerLayer.setMapParams(ZOOM_BG, Constants.NUM_TILES);
        cameraController = new CameraInputController(camera);

        tileMap.rebuild(centerGeolocation);

        InputMultiplexer mux = new InputMultiplexer();

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
        ScreenUtils.clear(0, 0, 0, 1);

        clampCameraToMap();
        camera.update();

        tileMap.render(camera);
        markerLayer.draw(camera, tileMap.getBeginTile(), markers, delta);
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

    private void onAllLocationsLoaded() {
        loading = false;
        rebuildMarkersFromLocations();

        if (markers.size > 0) {
            centerOnMarkersAverage();
            tileMap.rebuild(centerGeolocation);
        }

        // update particles count in one go
        markerLayer.syncParticlesToMarkers(markers.size);

        tileMap.setTileZoom(ZOOM_BG);
        tileMap.rebuild(centerGeolocation);


        Gdx.app.log("MAP", "Markers on map: " + markers.size);
    }

    private void rebuildMarkersFromLocations() {
        markers.clear();

        for (Hive hive : hives) {
            Location loc = locationsById.get(hive.id_location);
            if (loc == null) continue;
            markers.add(new Geolocation(loc.latitude, loc.longitude));
        }
    }

    private void centerOnMarkersAverage() {
        double sumLat = 0, sumLng = 0;
        for (Geolocation m : markers) { sumLat += m.lat; sumLng += m.lng; }
        centerGeolocation = new Geolocation(sumLat / markers.size, sumLng / markers.size);
    }

    private void clampCameraToMap() {
        camera.zoom = MathUtils.clamp(camera.zoom, 0.3f, 2f);

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

        float hitRadius = 40f;

        for (Geolocation g : markers) {
            Vector2 p = MapRasterTiles.getPixelPosition(
                g.lat, g.lng,
                tileMap.getBeginTile().x,
                tileMap.getBeginTile().y
            );

            if (p.dst(world.x, world.y) <= hitRadius) {
                openHiveDetail(g);
                return true;
            }
        }
        return false;
    }

    private void openHiveDetail(Geolocation hiveLocation) {
        game.setScreen(new HiveDetailMapScreen(game, hiveLocation, markers));
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
        if (markerLayer != null) markerLayer.dispose();
        if (tileMap != null) tileMap.dispose();
    }
}
