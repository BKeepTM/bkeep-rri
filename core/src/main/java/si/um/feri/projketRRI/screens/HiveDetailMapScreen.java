package si.um.feri.projketRRI.screens;

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

import java.util.List;

import si.um.feri.projketRRI.api.calls.NotesService;
import si.um.feri.projketRRI.api.calls.model.Hive;
import si.um.feri.projketRRI.api.calls.model.Location;
import si.um.feri.projketRRI.Projekt;
import si.um.feri.projketRRI.api.calls.model.HiveWeight;
import si.um.feri.projketRRI.api.calls.model.Notes;
import si.um.feri.projketRRI.screens.detailScreenUi.NotesUI;
import si.um.feri.projketRRI.utils.CameraInputController;
import si.um.feri.projketRRI.utils.Geolocation;
import si.um.feri.projketRRI.screens.detailScreenUi.HiveInfoUI;
import si.um.feri.projketRRI.screens.detailScreenUi.HiveWeightsGraphUI;
import si.um.feri.projketRRI.utils.MapRasterTiles;
import si.um.feri.projketRRI.utils.MarkerLayer;
import si.um.feri.projketRRI.utils.RasterTileMap;

public class HiveDetailMapScreen extends ScreenAdapter {

    private final Projekt game;
    private final Array<Geolocation> markers;

    private final Array<HiveWeight> hiveWeights;
    private final Hive hive;
    private final Location location;

    private OrthographicCamera camera;
    private RasterTileMap tileMap;
    private MarkerLayer markerLayer;
    private CameraInputController cameraController;
    private static final int ZOOM_DETAIL = 17;
    private static final int NUM_TILES_DETAIL = 7;

    private HiveInfoUI hiveInfoUI;
    private HiveWeightsGraphUI weightsGraphUI;

    private NotesUI notesUI;


    public HiveDetailMapScreen(Projekt game, Hive hive, Location location, Array<Geolocation> markers, Array<HiveWeight> hiveWeights) {
        this.game = game;
        this.hive = hive;
        this.location = location;
        this.markers = markers;
        this.hiveWeights = hiveWeights;
    }

    @Override
    public void show() {
        Geolocation center = new Geolocation(location.latitude, location.longitude);

        tileMap = new RasterTileMap();
        tileMap.setTileZoom(ZOOM_DETAIL);
        tileMap.setNumTiles(NUM_TILES_DETAIL);
        tileMap.rebuild(center);

        camera = new OrthographicCamera();
        camera.setToOrtho(false,
            MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL,
            MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL
        );
        camera.position.set(
            (MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL) / 2f,
            (MapRasterTiles.TILE_SIZE * NUM_TILES_DETAIL) / 2f,
            0
        );
        camera.zoom = 1.0f;
        camera.update();

        cameraController = new CameraInputController(camera);

        markerLayer = new MarkerLayer("Images/hive.png", "Particles/beeSmall.p");
        markerLayer.setMapParams(ZOOM_DETAIL, NUM_TILES_DETAIL);
        markerLayer.syncParticlesToMarkers(markers.size);

        hiveInfoUI = new HiveInfoUI();
        hiveInfoUI.setHiveData(
            hive.name,
            hive.location,
            hive.type,
            hive.status
        );

        weightsGraphUI = new HiveWeightsGraphUI();
        weightsGraphUI.setTitle(hive.name + " weight"); // if you pass Hive
        weightsGraphUI.setWeights(hiveWeights);

        notesUI = new NotesUI();
        notesUI.setLoading(true);

        NotesService.loadNotesForHive(hive.id, new NotesService.NotesCallback() {
            @Override
            public void onSuccess(List<Notes> notes) {
                notesUI.setLoading(false);
                notesUI.setNotes(notes);
            }

            @Override
            public void onError(String message) {
                notesUI.setLoading(false);
                notesUI.setError("Failed to load notes");
                Gdx.app.log("NOTES", "Failed: " + message);
            }
        });

        InputMultiplexer mux = new InputMultiplexer();

        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom = MathUtils.clamp(camera.zoom + amountY * 0.1f, 0.2f, 1.0f);
                return true;
            }
        });

        mux.addProcessor(hiveInfoUI.getStage());
        mux.addProcessor(weightsGraphUI.getStage());
        mux.addProcessor(notesUI.getStage());

        mux.addProcessor(new GestureDetector(cameraController));
        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    game.setScreen(new RasterMapScreen(game));
                    return true;
                }
                return false;
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
        hiveInfoUI.render();
        weightsGraphUI.render();
        notesUI.render();
    }

    @Override
    public void resize(int width, int height) {
        if (hiveInfoUI != null) hiveInfoUI.resize(width, height);
        if (weightsGraphUI != null) weightsGraphUI.resize(width, height);
        if (notesUI != null) notesUI.resize(width, height);
    }

    @Override
    public void dispose() {
        if (notesUI != null) notesUI.dispose();
        if (weightsGraphUI != null) weightsGraphUI.dispose();
        if (hiveInfoUI != null) hiveInfoUI.dispose();
        if (markerLayer != null) markerLayer.dispose();
        if (tileMap != null) tileMap.dispose();
    }


}
