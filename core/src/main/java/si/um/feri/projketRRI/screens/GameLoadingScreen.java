package si.um.feri.projketRRI.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import si.um.feri.projketRRI.Projekt;
import si.um.feri.projketRRI.utils.Geolocation;
import si.um.feri.projketRRI.utils.MapRasterTiles;
import si.um.feri.projketRRI.utils.ZoomXY;

public class GameLoadingScreen extends ScreenAdapter {

    private final Projekt game;
    private final Runnable onDone;
    private final Geolocation center;
    private final int zoom;
    private final int numTiles;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture white;

    private volatile float progress = 0f;
    private volatile String status = "Preparing...";
    private volatile boolean finished = false;

    private float groundY = 230f;
    private float beeX = 120f;
    private float beeY = groundY;
    private float beeW = 60f;
    private float beeH = 80f;

    private float velY = 0f;
    private boolean onGround = true;

    private float gravity = -1800f;
    private float jumpVel = 720f;

    private float speed = 340f;
    private float spawnEvery = 2f;

    private float score = 0f;

    private static class Obstacle {
        float x, y, w, h;
        ObstacleType type;
    }


    private final Array<Obstacle> obstacles = new Array<>();

    private Texture bgTex;
    private Texture beeTex;
    private float bgScrollX = 0f;
    private float groundScrollX = 0f;
    // UI
    private Stage uiStage;
    private Skin skin;
    private ProgressBar progressBar;
    private Label statusLabel;
    private TextButton continueButton;

    private boolean gameOver = false;
    private Label gameOverLabel;

    private final Vector2 tmp = new Vector2();

    private int scoreInt = 0;
    private int highScore = 0;
    private Label scoreLabel;

    private float spawnCooldown = 0f;
    private float minSpawnGap = 0.25f;
    private Texture rockTex, waspTex;
    private Viewport viewport;

    static final float WORLD_WIDTH  = 1200;
    static final float WORLD_HEIGHT = 1200;



    public GameLoadingScreen(Projekt game, Geolocation center, int zoom, int numTiles, Runnable onDone) {
        this.game = game;
        this.center = center;
        this.zoom = zoom;
        this.numTiles = numTiles;
        this.onDone = onDone;
    }

    private enum Lane { GROUND, AIR }

    private enum ObstacleType {
        ROCK(Lane.GROUND, 80, 60, 0.6f),
        WASP(Lane.AIR,    60, 60, 0.4f);

        final Lane lane;
        final float w, h;
        final float weight;

        ObstacleType(Lane lane, float w, float h, float weight) {
            this.lane = lane;
            this.w = w;
            this.h = h;
            this.weight = weight;
        }
    }


    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        viewport.apply(true);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        batch = new SpriteBatch();
        white = makeWhiteTexture();

        bgTex = new Texture(Gdx.files.internal("Images/bg2.png"));
        beeTex = new Texture(Gdx.files.internal("Images/beePlayer.png"));
        waspTex = new Texture(Gdx.files.internal("Images/wasp.png"));
        rockTex   = new Texture(Gdx.files.internal("Images/rock.png"));


        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        uiStage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));

        Table top = new Table();
        top.setFillParent(true);
        top.top().padTop(20);

        statusLabel = new Label("Loading tiles...", skin);
        statusLabel.setAlignment(Align.center);

        progressBar = new ProgressBar(0f, 1f, 0.01f, false, skin);
        progressBar.setAnimateDuration(0.15f);

        top.add(statusLabel).padTop(240).width(420).padBottom(8).row();
        top.add(progressBar).width(420).height(20);

        uiStage.addActor(top);

        Table center = new Table();
        center.setFillParent(true);
        center.center();

        gameOverLabel = new Label("You crashed!\nPress SPACE to try again", skin);
        gameOverLabel.setAlignment(Align.center);
        gameOverLabel.setVisible(false);

        center.add(gameOverLabel).padTop(120).pad(10);
        uiStage.addActor(center);

        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().right().padTop(700).padRight(40);

        scoreLabel = new Label("HI 00000  00000", skin);
        scoreLabel.setAlignment(Align.right);

        hud.add(scoreLabel);
        uiStage.addActor(hud);


        Table bottom = new Table();
        bottom.setFillParent(true);
        bottom.bottom().padBottom(20);

        continueButton = new TextButton("Go to map", skin);
        continueButton.setDisabled(true);

        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!continueButton.isDisabled() && onDone != null) {
                    onDone.run();
                }
            }
        });

        bottom.add(continueButton).padBottom(100).width(220).height(48);

        continueButton.setVisible(false);
        continueButton.setDisabled(true);

        uiStage.addActor(bottom);

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(uiStage);
        Gdx.input.setInputProcessor(mux);

        startPrefetchThread();
    }

    private Texture makeWhiteTexture() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private void startPrefetchThread() {
        new Thread(() -> {
            try {
                status = "Calculating tiles...";
                progress = 0.05f;

                ZoomXY centerTile = MapRasterTiles.getTileNumber(center.lat, center.lng, zoom);

                status = "Downloading tiles...";
                progress = 0.10f;

                int half = (numTiles - 1) / 2;
                int total = numTiles * numTiles;
                int done = 0;

                for (int dy = -half; dy <= half; dy++) {
                    for (int dx = -half; dx <= half; dx++) {
                        MapRasterTiles.prefetchRasterTile(centerTile.zoom, centerTile.x + dx, centerTile.y + dy);
                        done++;
                        progress = 0.10f + 0.90f * ((float) done / (float) total);
                    }
                }

                progress = 1f;
                status = "Tiles ready!";
                finished = true;

            } catch (Exception e) {
                status = "Failed: " + e.getMessage();
                finished = false;
            }
        }).start();
    }

    @Override
    public void render(float delta) {
        viewport.apply();
        delta = Math.min(delta, 1f / 30f);

        updateGame(delta);

        ScreenUtils.clear(0, 0, 0, 1);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        drawGame();
        batch.end();


        progressBar.setValue(progress);
        statusLabel.setText(status);

        if (finished && !continueButton.isVisible()) {
            continueButton.setVisible(true);
            continueButton.setDisabled(false);
        }

        if (scoreLabel != null) {
            scoreLabel.setText("HI " + pad5(highScore) + "  " + pad5(scoreInt));
        }

        uiStage.act(delta);
        uiStage.draw();
    }

    private String pad5(int n) {
        if (n < 0) n = 0;
        if (n > 99999) n = 99999;
        return String.format("%05d", n);
    }

    private void updateGame(float dt) {

        if (gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || (Gdx.input.justTouched() && uiStage.hit(tmp.x, tmp.y, true) == null)) {

                resetRun();
                setGameOver(false);
            }
            return;
        }

        boolean jumpPressed =
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.UP);

        if (Gdx.input.justTouched() && uiStage != null) {
            tmp.set(Gdx.input.getX(), Gdx.input.getY());
            uiStage.screenToStageCoordinates(tmp);
            Actor hit = uiStage.hit(tmp.x, tmp.y, true);

            if (hit == null) {
                jumpPressed = true;
            }
        }

        if (jumpPressed && onGround) {
            velY = jumpVel;
            onGround = false;
        }

        // Physics
        velY += gravity * dt;
        beeY += velY * dt;

        if (beeY <= groundY) {
            beeY = groundY;
            velY = 0f;
            onGround = true;
        }

        float difficulty = 1f + (score / 1200f);

        bgScrollX += (speed * 0.15f) * difficulty * dt;
        groundScrollX += speed * difficulty * dt;

        if (bgTex != null) bgScrollX %= bgTex.getWidth();


        spawnCooldown -= dt;
        if (spawnCooldown <= 0f) {
            spawnObstacle(difficulty);
        }

        // Move obstacles
        float move = speed * difficulty * dt;
        for (int i = obstacles.size - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            o.x -= move;

            if (o.x + o.w < 0) obstacles.removeIndex(i);
        }

        // Collision
        for (Obstacle o : obstacles) {
            if (rectsOverlap(beeX, beeY, beeW, beeH, o.x, o.y, o.w, o.h)) {
                setGameOver(true);
                break;
            }
        }

        score += 60f * dt;
        scoreInt = (int) score;

        if (scoreInt > highScore) highScore = scoreInt;
    }

    private void spawnObstacle(float difficulty) {

        ObstacleType type = pickType(difficulty);

        Obstacle last = obstacles.size > 0 ? obstacles.peek() : null;
        if (last != null) {
            boolean lastStillNearSpawn = last.x > Gdx.graphics.getWidth() - 120f;
            if (lastStillNearSpawn) {
                if ((last.type.lane == Lane.GROUND && type.lane == Lane.AIR) ||
                    (last.type.lane == Lane.AIR && type.lane == Lane.GROUND)) {

                    type = pickTypeSameLane(last.type.lane, difficulty);
                }
            }
        }

        Obstacle o = new Obstacle();
        o.type = type;
        o.w = type.w;
        o.h = type.h;
        o.x = Gdx.graphics.getWidth() + 20f;

        if (type.lane == Lane.AIR) {
            float[] heights = { groundY + 40f, groundY + 90f, groundY + 140f };
            o.y = heights[MathUtils.random(0, heights.length - 1)];
        } else {
            o.y = groundY;
        }

        obstacles.add(o);

        float next = MathUtils.clamp(spawnEvery / difficulty, 0.35f, 1.5f);
        next *= MathUtils.random(0.8f, 1.2f);
        spawnCooldown = Math.max(next, minSpawnGap);
    }

    private ObstacleType pickTypeSameLane(Lane lane, float difficulty) {
        float total = 0f;
        for (ObstacleType t : ObstacleType.values()) {
            if (t.lane != lane) continue;
            total += t.weight;
        }

        float r = MathUtils.random() * total;
        float acc = 0f;

        for (ObstacleType t : ObstacleType.values()) {
            if (t.lane != lane) continue;
            acc += t.weight;
            if (r <= acc) return t;
        }

        return (lane == Lane.AIR) ? ObstacleType.WASP : ObstacleType.ROCK;
    }

    private ObstacleType pickType(float difficulty) {
        boolean allowAir = true;

        float total = 0f;
        for (ObstacleType t : ObstacleType.values()) {
            total += t.weight;
        }

        float r = MathUtils.random() * total;
        float acc = 0f;

        for (ObstacleType t : ObstacleType.values()) {
            acc += t.weight;
            if (r <= acc) return t;
        }

        return ObstacleType.ROCK;
    }

    private boolean rectsOverlap(float ax, float ay, float aw, float ah,
                                 float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void setGameOver(boolean over) {
        gameOver = over;

        if (gameOverLabel != null) {
            gameOverLabel.setVisible(over);
        }

        if (over) {
            velY = 0f;
        }
    }

    private void resetRun() {
        obstacles.clear();
        score = 0f;

        beeY = groundY;
        velY = 0f;
        onGround = true;
        score = 0f;
        scoreInt = 0;
    }

    private void drawGame() {

        // --- background ---
        if (bgTex != null) {
            float screenW = Gdx.graphics.getWidth();
            float screenH = Gdx.graphics.getHeight();

            // draw bg twice for seamless scroll
            float x1 = -bgScrollX;
            batch.draw(bgTex, x1, 0, bgTex.getWidth(), screenH);
            batch.draw(bgTex, x1 + bgTex.getWidth(), 0, bgTex.getWidth(), screenH);

            // if screen wider than 2x texture, draw extra (rare)
            if (x1 + 2 * bgTex.getWidth() < screenW) {
                batch.draw(bgTex, x1 + 2 * bgTex.getWidth(), 0, bgTex.getWidth(), screenH);
            }
        }


        if (beeTex != null) {
            batch.draw(beeTex, beeX, beeY, beeW, beeH);
        } else {
            drawRect(beeX, beeY, beeW, beeH);
        }

        for (Obstacle o : obstacles) {
            Texture tex = null;

            if (o.type == ObstacleType.ROCK) tex = rockTex;
            else if (o.type == ObstacleType.WASP) tex = waspTex;

            if (tex != null) batch.draw(tex, o.x, o.y, o.w, o.h);
            else drawRect(o.x, o.y, o.w, o.h);
        }
    }

    private void drawRect(float x, float y, float w, float h) {
        batch.draw(white, x, y, w, h);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (white != null) white.dispose();

        if (uiStage != null) uiStage.dispose();
        if (skin != null) skin.dispose();

        if (bgTex != null) bgTex.dispose();
        if (beeTex != null) beeTex.dispose();
    }

}
