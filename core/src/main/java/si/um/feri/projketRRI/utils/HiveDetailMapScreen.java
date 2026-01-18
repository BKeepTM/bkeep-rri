package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import si.um.feri.projketRRI.Projekt;
import si.um.feri.projketRRI.RasterMapScreen;

public class HiveDetailMapScreen extends ScreenAdapter {

    private final Projekt game;              // your main Game class
    private final Geolocation center;
    private final Array<Geolocation> markers; // optional: show all or just this one

    private OrthographicCamera camera;
    private RasterTileMap tileMap;
    private MarkerLayer markerLayer;
    private CameraInputController cameraController;
    private static final int ZOOM_DETAIL = 17;
    private static final int NUM_TILES_DETAIL = 6;

    public HiveDetailMapScreen(Projekt game, Geolocation center, Array<Geolocation> markers) {
        this.game = game;
        this.center = center;
        this.markers = markers;
    }

    @Override
    public void show() {

        tileMap = new RasterTileMap();
        tileMap.setTileZoom(ZOOM_DETAIL);
        tileMap.setNumTiles(NUM_TILES_DETAIL); // add setter or constructor param
        tileMap.rebuild(center);


        camera = new OrthographicCamera();
        camera.setToOrtho(false,
                MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL,
                MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL);
        camera.position.set(
                (MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL) / 2f,
                (MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL) / 2f,
                0);
        camera.zoom = 1.0f;
        camera.update();


        markerLayer = new MarkerLayer("Images/hive.png", "Particles/beeSmall.p");
        markerLayer.setMapParams(ZOOM_DETAIL, NUM_TILES_DETAIL);
        markerLayer.syncParticlesToMarkers(markers.size);

        // Add input: back button / ESC to return
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    game.setScreen(new RasterMapScreen(game)); // or store previous
                    return true;
                }
                return false;
            }
        });

        cameraController = new CameraInputController(camera);

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
    }

    private void clampCameraToMap() {
        camera.zoom = MathUtils.clamp(camera.zoom, 0.2f, 1.0f);

        float mapW = NUM_TILES_DETAIL * MapRasterTiles.TILE_SIZE;
        float mapH = NUM_TILES_DETAIL * MapRasterTiles.TILE_SIZE;

        float vw = camera.viewportWidth * camera.zoom;
        float vh = camera.viewportHeight * camera.zoom;

        camera.position.x = MathUtils.clamp(camera.position.x, vw/2f, mapW - vw/2f);
        camera.position.y = MathUtils.clamp(camera.position.y, vh/2f, mapH - vh/2f);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        clampCameraToMap();
        camera.update();
        tileMap.render(camera);
        markerLayer.draw(camera, tileMap.getBeginTile(), markers, delta);
    }

    @Override
    public void dispose() {
        if (markerLayer != null) markerLayer.dispose();
        if (tileMap != null) tileMap.dispose();
    }
}
