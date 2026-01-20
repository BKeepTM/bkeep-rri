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

import si.um.feri.projketRRI.api.calls.ApiClient;
import si.um.feri.projketRRI.api.calls.LocationService;
import si.um.feri.projketRRI.api.calls.NotesService;
import si.um.feri.projketRRI.api.calls.WeatherService;
import si.um.feri.projketRRI.api.calls.model.Hive;
import si.um.feri.projketRRI.api.calls.model.Location;
import si.um.feri.projketRRI.Projekt;
import si.um.feri.projketRRI.api.calls.model.HiveWeight;
import si.um.feri.projketRRI.api.calls.model.Notes;
import si.um.feri.projketRRI.api.calls.model.Weather;
import si.um.feri.projketRRI.screens.detailScreenUi.HiveWeatherView;
import si.um.feri.projketRRI.screens.detailScreenUi.simulation.SimulationCalculator;
import si.um.feri.projketRRI.screens.detailScreenUi.simulation.SimulationDialog;
import si.um.feri.projketRRI.screens.detailScreenUi.simulation.SimulationResultsGraphUI;
import si.um.feri.projketRRI.screens.detailScreenUi.hiveNotes.NotesUI;
import si.um.feri.projketRRI.screens.detailScreenUi.hiveInfo.HiveInfoController;
import si.um.feri.projketRRI.screens.detailScreenUi.hiveInfo.HiveInfoView;
import si.um.feri.projketRRI.utils.CameraInputController;
import si.um.feri.projketRRI.utils.Geolocation;
import si.um.feri.projketRRI.screens.detailScreenUi.hiveWeight.HiveWeightsGraphUI;
import si.um.feri.projketRRI.utils.MapRasterTiles;
import si.um.feri.projketRRI.utils.MarkerLayer;
import si.um.feri.projketRRI.utils.RasterTileMap;
import si.um.feri.projketRRI.utils.WeatherMath;
import si.um.feri.projketRRI.screens.detailScreenUi.simulation.SimulationResultsGraphActor;

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

    private Stage uiStage;
    private Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
    private HiveWeatherView weatherView;
    private Weather closestWeather;

    private SimulationDialog simDialog;
    private SimulationResultsGraphActor simulationGraphActor;

    private SimulationResultsGraphUI simulationGraphUI;


    public HiveDetailMapScreen(Projekt game, Hive hive, Location location, Array<Geolocation> markers, Array<HiveWeight> hiveWeights) {
        this.game = game;
        this.hive = hive;
        this.location = location;
        this.hiveWeights = hiveWeights;
    }

    @Override
    public void show() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        uiStage = new Stage(new ScreenViewport());

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
        if ("online".equalsIgnoreCase(hive.status)) particlePath = "Particles/beeSmall.p";
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

        weatherView = new HiveWeatherView();
        weatherView.setLoading("Loading closest weather...");

        simulationGraphUI = new SimulationResultsGraphUI();
        simulationGraphUI.setTitle(hive.name + " simulation");

        simDialog = new SimulationDialog(skin,hiveWeights);

        simDialog.setBaseWeather(
            closestWeather != null ? closestWeather.temperature : null,
            closestWeather != null ? (float) closestWeather.humidity : null
        );

        simDialog = new SimulationDialog(skin, hiveWeights);

        simDialog.setBaseWeather(
            closestWeather != null ? closestWeather.temperature : null,
            closestWeather != null ? (float) closestWeather.humidity : null
        );

        simDialog.setListener(new SimulationDialog.Listener() {
            @Override
            public void onStart(SimulationCalculator.Params params) {
                List<SimulationCalculator.DayResult> results =
                    SimulationCalculator.run(params);

                for (SimulationCalculator.DayResult r : results) System.out.println(r);

                simulationGraphUI.setResults(results);
            }

            @Override
            public void onCancel() {
                System.out.println("[SIM] Simulation canceled");
            }
        });

        weatherView.setOnSimulationClick(() -> simDialog.show(uiStage));


        notesUI = new NotesUI(hive, new NotesUI.NotesActions() {
            @Override
            public void requestReloadNotes() {
                loadNotes();
            }

            @Override
            public void showMessage(String msg) {
                System.out.println(msg);
            }

            @Override
            public void showError(String msg) {
                System.err.println(msg);
            }
        });
        loadNotes();

        setupDeleteUi();

        InputMultiplexer mux = new InputMultiplexer();

        mux.addProcessor(uiStage);

        mux.addProcessor(weatherView.getStage());
        mux.addProcessor(notesUI.getStage());
        mux.addProcessor(weightsGraphUI.getStage());
        mux.addProcessor(hiveInfoView.getStage());
        mux.addProcessor(simulationGraphUI.getStage());

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


        loadClosestWeatherForHive();
    }


    private void setupDeleteUi() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        uiStage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        root.bottom().right().pad(10);

        TextButton deleteBtn = new TextButton("Remove Hive", skin);
        deleteBtn.setColor(Color.RED);

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
        dialog.button("Delete", true);
        dialog.show(uiStage);
    }

    private void performDelete() {
        new Thread(() -> {
            try {
                boolean success = ApiClient.removeHive(hive.id);

                Gdx.app.postRunnable(() -> {
                    if (success) {
                        Gdx.app.log("HIVE", "Hive removed successfully.");
                        game.setScreen(new RasterMapScreen(game));
                    } else {
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

    private void loadNotes() {
        notesUI.setLoading(true);
        NotesService.loadNotesForHive(hive.id, new NotesService.NotesCallback() {
            @Override
            public void onSuccess(List<Notes> notes) {
                notesUI.setNotes(notes, hive.id);
            }

            @Override
            public void onError(String message) {
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

        camera.position.x = MathUtils.clamp(camera.position.x, vw / 2f, mapW - vw / 2f);
        camera.position.y = MathUtils.clamp(camera.position.y, vh / 2f, mapH - vh / 2f);
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
        weatherView.render();
        notesUI.render();
        simulationGraphUI.render();

        if (uiStage != null) {
            uiStage.act(delta);
            uiStage.draw();
        }
    }

    private void loadClosestWeatherForHive() {
        weatherView.setLoading("Loading closest weather...");

        LocationService.loadLocation(hive.id_location, new LocationService.LocationCallback() {
            @Override
            public void onSuccess(Location hiveLoc) {

                WeatherService.loadAll(new WeatherService.WeatherListCallback() {
                    @Override
                    public void onSuccess(com.badlogic.gdx.utils.Array<Weather> weathers) {

                        if (weathers == null || weathers.size == 0) {
                            weatherView.setError("No weather stations.");
                            return;
                        }

                        com.badlogic.gdx.utils.IntMap<Location> stationLocs = new com.badlogic.gdx.utils.IntMap<>();

                        com.badlogic.gdx.utils.IntSet ids = new com.badlogic.gdx.utils.IntSet();
                        for (Weather w : weathers) ids.add(w.id_location);

                        final int total = ids.size;
                        final int[] remaining = {total};

                        if (total == 0) {
                            weatherView.setError("No station locations.");
                            return;
                        }

                        for (com.badlogic.gdx.utils.IntSet.IntSetIterator it = ids.iterator(); it.hasNext; ) {
                            int id = it.next();

                            LocationService.loadLocation(id, new LocationService.LocationCallback() {
                                @Override
                                public void onSuccess(Location loc) {
                                    stationLocs.put(loc.id, loc);
                                    remaining[0]--;

                                    if (remaining[0] == 0) {
                                        Weather closest = WeatherMath.findClosestWeather(hiveLoc, weathers, stationLocs);

                                        WeatherMath.logNearestStations(hiveLoc, weathers, stationLocs, 5);

                                        closestWeather = closest;
                                        weatherView.setWeather(closestWeather);

                                        if (simDialog != null && closestWeather != null) {
                                            simDialog.setBaseWeather(
                                                closestWeather.temperature,
                                                (float) closestWeather.humidity
                                            );
                                        }
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    remaining[0]--;

                                    if (remaining[0] == 0) {
                                        Weather closest = WeatherMath.findClosestWeather(hiveLoc, weathers, stationLocs);

                                        closestWeather = closest;
                                        weatherView.setWeather(closestWeather);

                                        if (simDialog != null && closestWeather != null) {
                                            simDialog.setBaseWeather(
                                                closestWeather.temperature,
                                                (float) closestWeather.humidity
                                            );
                                        }
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String message) {
                        weatherView.setError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                weatherView.setError("Hive location failed: " + message);
            }
        });
    }


    @Override
    public void resize(int width, int height) {
        if (hiveInfoView != null) hiveInfoView.resize(width, height);
        if (weightsGraphUI != null) weightsGraphUI.resize(width, height);
        if (notesUI != null) notesUI.resize(width, height);
        if (uiStage != null) uiStage.getViewport().update(width, height, true);
        if (weatherView != null) weatherView.resize(width, height);
        simulationGraphUI.resize(width, height);

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
        if (weatherView != null) weatherView.dispose();
        simulationGraphUI.dispose();
    }
}
