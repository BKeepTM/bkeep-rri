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
import si.um.feri.projketRRI.utils.LazyDetailPatchLayer;
import si.um.feri.projketRRI.utils.Geolocation;
import si.um.feri.projketRRI.utils.MapRasterTiles;
import si.um.feri.projketRRI.utils.MarkerLayer;
import si.um.feri.projketRRI.utils.RasterTileMap;
import si.um.feri.projketRRI.utils.api.ApiClient;

public class RasterMapScreen extends ScreenAdapter {

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

    private LazyDetailPatchLayer detailLayer;
    private final int ZOOM_BG = 9;
    private final int ZOOM_PATCH = 15;
    private final int PATCH_TILES = 3;

    private Geolocation selectedMarker = null;
    private DetailPanel detailPanel;

    @Override
    public void show() {
        // camera

        detailLayer = new LazyDetailPatchLayer(ZOOM_PATCH, PATCH_TILES);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.MAP_WIDTH, Constants.MAP_HEIGHT);
        camera.position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, 0);
        camera.viewportWidth = Constants.MAP_WIDTH / 2f;
        camera.viewportHeight = Constants.MAP_HEIGHT / 2f;
        camera.zoom = 2f;
        camera.update();

        // helpers
        tileMap = new RasterTileMap();
        markerLayer = new MarkerLayer("Images/hive.png", "Particles/beeSmall.p");
        cameraController = new CameraInputController(camera);

        // initial tiles
        tileMap.rebuild(centerGeolocation);

        // input
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(new GestureDetector(cameraController));
        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom += amountY * 0.1f;
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

        for (int i = 0; i < markers.size; i++) {
            Geolocation g = markers.get(i);
            if (isMarkerNearView(g)) {
                detailLayer.requestPatchIfNeeded(g);
            }
        }

        detailLayer.updateLoading();

        detailLayer.draw(camera, tileMap.getBeginTile(), ZOOM_BG);

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

                    // avoid duplicate calls
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

                // if all were duplicates / already cached
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

        detailLayer.reset();

        Gdx.app.log("MAP", "Markers on map: " + markers.size);
    }

    private boolean trySelectMarker(float screenX, float screenY) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        camera.unproject(v);

        float hitRadius = 40f; // world pixels
        for (Geolocation g : markers) {
            Vector2 p = MapRasterTiles.getPixelPosition(g.lat, g.lng, tileMap.getBeginTile().x, tileMap.getBeginTile().y);
            if (p.dst(v.x, v.y) <= hitRadius) {
                selectedMarker = g;
                detailPanel.request(selectedMarker);  // load zoom15 tiles
                return true;
            }
        }
        return false;
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
        camera.zoom = MathUtils.clamp(camera.zoom, 0.5f, 2f);

        // IMPORTANT: clamp against actual pixel map size (numTiles * tileSize)
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

    private boolean isMarkerNearView(Geolocation g) {
        // Convert geo to background pixel coords (same as you do for markers)
        Vector2 p = MapRasterTiles.getPixelPosition(g.lat, g.lng, tileMap.getBeginTile().x, tileMap.getBeginTile().y);

        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        // add margin so patches load slightly before marker enters view
        float margin = 200f;

        return p.x >= camera.position.x - halfW - margin &&
            p.x <= camera.position.x + halfW + margin &&
            p.y >= camera.position.y - halfH - margin &&
            p.y <= camera.position.y + halfH + margin;
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (markerLayer != null) markerLayer.dispose();
        if (tileMap != null) tileMap.dispose();
        if (detailLayer != null) detailLayer.dispose();
    }
}
