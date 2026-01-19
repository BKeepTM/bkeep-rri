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
import si.um.feri.projketRRI.screens.detailScreenUi.hiveNotes.NotesUI;
import si.um.feri.projketRRI.screens.detailScreenUi.hiveInfo.HiveInfoController;
import si.um.feri.projketRRI.screens.detailScreenUi.hiveInfo.HiveInfoView;
import si.um.feri.projketRRI.utils.CameraInputController;
import si.um.feri.projketRRI.utils.Geolocation;
import si.um.feri.projketRRI.screens.detailScreenUi.hiveWeight.HiveWeightsGraphUI;
import si.um.feri.projketRRI.utils.MapRasterTiles;
import si.um.feri.projketRRI.utils.MarkerLayer;
import si.um.feri.projketRRI.utils.RasterTileMap;

public class HiveDetailMapScreen extends ScreenAdapter {

    private final Projekt game;
    private final Array<Geolocation> markers = new Array<>();

    private final Array<HiveWeight> hiveWeights;
    private final Hive hive;
    private final Location location;

    private OrthographicCamera camera;
    private RasterTileMap tileMap;
    private MarkerLayer markerLayer;
    private CameraInputController cameraController;
    private static final int ZOOM_DETAIL = 17;
    private static final int NUM_TILES_DETAIL = 7;

    private HiveInfoView hiveInfoView;
    private HiveInfoController hiveInfoController;
    private HiveWeightsGraphUI weightsGraphUI;

    private NotesUI notesUI;
    private boolean lastOnline = false;


    public HiveDetailMapScreen(Projekt game, Hive hive, Location location, Array<Geolocation> markers, Array<HiveWeight> hiveWeights) {
        this.game = game;
        this.hive = hive;
        this.location = location;
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


        String particlePath = null;

        if ("online".equalsIgnoreCase(hive.status)) {
            particlePath = "Particles/beeSmall.p";
        }

        lastOnline = "online".equalsIgnoreCase(hive.status);

        markerLayer = new MarkerLayer("Images/hive.png", particlePath);
        markerLayer.setMapParams(ZOOM_DETAIL, NUM_TILES_DETAIL);

        markers.clear();
        markers.add(new Geolocation(location.latitude, location.longitude));

        markerLayer.syncParticlesToMarkers(markers.size);

        hiveInfoView = new HiveInfoView();
        hiveInfoController = new HiveInfoController(hiveInfoView);
        hiveInfoController.setHive(hive);

        weightsGraphUI = new HiveWeightsGraphUI();
        weightsGraphUI.setTitle(hive.name + " weight");
        weightsGraphUI.setWeights(hiveWeights);

        notesUI = new NotesUI(hive, new NotesUI.NotesActions() {
            @Override public void requestReloadNotes() {
                notesUI.setLoading(true);
                NotesService.loadNotesForHive(hive.id, new NotesService.NotesCallback() {
                    @Override public void onSuccess(List<Notes> notes) {
                        notesUI.setNotes(notes, hive.id);
                    }
                    @Override public void onError(String message) {
                        notesUI.setError(message);
                    }
                });
            }
            @Override public void showMessage(String msg) { System.out.println(msg); }
            @Override public void showError(String msg) { System.err.println(msg); }
        });

        notesUI.setLoading(true);
        NotesService.loadNotesForHive(hive.id, new NotesService.NotesCallback() {
            @Override public void onSuccess(List<Notes> notes) {
                notesUI.setNotes(notes, hive.id);
            }
            @Override public void onError(String message) {
                notesUI.setError(message);
            }
        });

        InputMultiplexer mux = new InputMultiplexer();

        mux.addProcessor(notesUI.getStage());
        mux.addProcessor(weightsGraphUI.getStage());
        mux.addProcessor(hiveInfoView.getStage());

        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom = MathUtils.clamp(camera.zoom + amountY * 0.1f, 0.2f, 1.0f);
                return true;
            }
        });

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

    private void refreshMarkerParticlesForStatus() {
        boolean online = "online".equalsIgnoreCase(hive.status);

        int count = markers.size;

        if (markerLayer != null) {
            markerLayer.dispose();
        }

        markerLayer = new MarkerLayer(
            "Images/hive.png",
            online ? "Particles/beeSmall.p" : null
        );
        markerLayer.setMapParams(ZOOM_DETAIL, NUM_TILES_DETAIL);
        markerLayer.syncParticlesToMarkers(online ? count : 0);
    }




    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        clampCameraToMap();
        camera.update();
        tileMap.render(camera);

        boolean online = "online".equalsIgnoreCase(hive.status);

        if (online != lastOnline) {
            markerLayer.setParticlesEnabled(online);

            if (online) markerLayer.syncParticlesToMarkers(markers.size);
            else markerLayer.syncParticlesToMarkers(0);

            lastOnline = online;
        }

        markerLayer.draw(camera, tileMap.getBeginTile(), markers, delta);
        hiveInfoView.render();
        weightsGraphUI.render();
        notesUI.render();
    }

    @Override
    public void resize(int width, int height) {
        if (hiveInfoView != null) hiveInfoView.resize(width, height);
        if (weightsGraphUI != null) weightsGraphUI.resize(width, height);
        if (notesUI != null) notesUI.resize(width, height);
    }

    @Override
    public void dispose() {
        if (notesUI != null) notesUI.dispose();
        if (weightsGraphUI != null) weightsGraphUI.dispose();
        if (hiveInfoView != null) hiveInfoView.dispose();
        if (markerLayer != null) markerLayer.dispose();
        if (tileMap != null) tileMap.dispose();
    }


}
