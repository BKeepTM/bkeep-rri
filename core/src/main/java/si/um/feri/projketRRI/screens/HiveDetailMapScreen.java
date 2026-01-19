package si.um.feri.projketRRI.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.List;

import si.um.feri.projketRRI.api.calls.ApiClient; // Ensure ApiClient is imported
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

    // --- NEW UI FOR DELETE ---
    private Stage uiStage;
    private Skin skin;
    // -------------------------

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

        // --- Info View ---
        hiveInfoView = new HiveInfoView();
        hiveInfoController = new HiveInfoController(hiveInfoView);
        hiveInfoController.setHive(hive);

        // --- Weights Graph ---
        weightsGraphUI = new HiveWeightsGraphUI();
        weightsGraphUI.setTitle(hive.name + " weight");
        weightsGraphUI.setWeights(hiveWeights);

        // --- Notes UI ---
        notesUI = new NotesUI(hive, new NotesUI.NotesActions() {
            @Override public void requestReloadNotes() {
                loadNotes();
            }
            @Override public void showMessage(String msg) { System.out.println(msg); }
            @Override public void showError(String msg) { System.err.println(msg); }
        });
        loadNotes();

        // --- NEW: DELETE UI SETUP ---
        setupDeleteUi();
        // ----------------------------

        InputMultiplexer mux = new InputMultiplexer();

        // Add the Delete UI stage to input
        mux.addProcessor(uiStage);

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

    // --- Helper to setup Delete Button ---
    private void setupDeleteUi() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        uiStage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        root.bottom().right().pad(10); // Position Top Right

        TextButton deleteBtn = new TextButton("Remove Hive", skin);
        deleteBtn.setColor(Color.RED); // Make it red for warning

        deleteBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                confirmDelete();
            }
        });

        root.add(deleteBtn).width(120);
        uiStage.addActor(root);
    }

    private void confirmDelete() {
        Dialog dialog = new Dialog("Remove Hive?", skin) {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    performDelete();
                }
            }
        };
        dialog.text("Are you sure you want to delete this hive?\nThis cannot be undone.");
        dialog.button("Cancel", false);
        dialog.button("Delete", true); // Returns true when clicked
        dialog.show(uiStage);
    }

    private void performDelete() {
        // Run network call on background thread
        new Thread(() -> {
            try {
                boolean success = ApiClient.removeHive(hive.id);

                Gdx.app.postRunnable(() -> {
                    if (success) {
                        Gdx.app.log("HIVE", "Hive removed successfully.");
                        // Navigate back to the main map
                        game.setScreen(new RasterMapScreen(game));
                    } else {
                        // Show error if needed
                        Gdx.app.log("HIVE", "Failed to remove hive.");
                    }
                });
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    Gdx.app.log("HIVE", "Error removing hive: " + e.getMessage());
                });
            }
        }).start();
    }
    // ------------------------------------

    private void loadNotes() {
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

        boolean online = "online".equalsIgnoreCase(hive.status);

        if (online != lastOnline) {
            if (markerLayer != null) markerLayer.dispose();

            markerLayer = new MarkerLayer(
                "Images/hive.png",
                online ? "Particles/beeSmall.p" : null
            );
            markerLayer.setMapParams(ZOOM_DETAIL, NUM_TILES_DETAIL);
            markerLayer.syncParticlesToMarkers(online ? markers.size : 0);

            lastOnline = online;
        }

        markerLayer.draw(camera, tileMap.getBeginTile(), markers, delta);
        hiveInfoView.render();
        weightsGraphUI.render();
        notesUI.render();

        // --- Render the Delete UI ---
        if (uiStage != null) {
            uiStage.act(delta);
            uiStage.draw();
        }
        // ----------------------------
    }

    @Override
    public void resize(int width, int height) {
        if (hiveInfoView != null) hiveInfoView.resize(width, height);
        if (weightsGraphUI != null) weightsGraphUI.resize(width, height);
        if (notesUI != null) notesUI.resize(width, height);
        if (uiStage != null) uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (notesUI != null) notesUI.dispose();
        if (weightsGraphUI != null) weightsGraphUI.dispose();
        if (hiveInfoView != null) hiveInfoView.dispose();
        if (markerLayer != null) markerLayer.dispose();
        if (tileMap != null) tileMap.dispose();
        if (uiStage != null) uiStage.dispose();
        if (skin != null) skin.dispose();
    }
}
