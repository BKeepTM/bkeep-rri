package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class MarkerLayer implements Disposable {

    private final SpriteBatch batch;
    private final Texture markerTexture;

    private final Array<ParticleEffect> beeEffects = new Array<>();
    private ParticleEffect template;
    private boolean particlesEnabled = false;

    private int tileZoom;
    private int numTiles;

    public MarkerLayer(String markerTexturePath, String particlePath) {
        batch = new SpriteBatch();
        markerTexture = new Texture(Gdx.files.internal(markerTexturePath));

        if (particlePath != null && !particlePath.isEmpty()) {
            particlesEnabled = true;
            template = new ParticleEffect();
            template.load(Gdx.files.internal(particlePath), Gdx.files.internal(""));
        } else {
            particlesEnabled = false;
            template = null;
        }
    }

    public void setMapParams(int tileZoom, int numTiles) {
        this.tileZoom = tileZoom;
        this.numTiles = numTiles;
    }

    public void syncParticlesToMarkers(int markerCount) {
        if (!particlesEnabled) {
            while (beeEffects.size > 0) {
                ParticleEffect e = beeEffects.pop();
                e.dispose();
            }
            return;
        }

        while (beeEffects.size > markerCount) {
            ParticleEffect e = beeEffects.pop();
            e.dispose();
        }
        while (beeEffects.size < markerCount) {
            ParticleEffect e = new ParticleEffect(template);
            e.start();
            beeEffects.add(e);
        }
    }

    public void draw(OrthographicCamera camera, ZoomXY beginTile, Array<Geolocation> markers, float dt) {
        if (beginTile == null) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float w = 64, h = 64;
        int mapHeightPx = numTiles * MapRasterTiles.TILE_SIZE;

        for (int i = 0; i < markers.size; i++) {
            Geolocation g = markers.get(i);
            Vector2 p = MapRasterTiles.getPixelPosition(g.lat, g.lng, beginTile.x, beginTile.y, tileZoom, mapHeightPx);

            batch.draw(markerTexture, p.x - w / 2f, p.y - h / 2f, w, h);

            if (particlesEnabled && i < beeEffects.size) {
                ParticleEffect e = beeEffects.get(i);
                e.setPosition(p.x, p.y);
                e.draw(batch, dt);
                if (e.isComplete()) e.reset();
            }
        }

        batch.end();
    }

    public void setParticlesEnabled(boolean enabled) {
        if (template == null) {
            template = new ParticleEffect();
            template.load(Gdx.files.internal("Particles/beeSmall.p"), Gdx.files.internal(""));
        }

        this.particlesEnabled = enabled;

        if (!enabled) {
            for (ParticleEffect e : beeEffects) e.dispose();
            beeEffects.clear();
        }
    }

    @Override
    public void dispose() {
        for (ParticleEffect e : beeEffects) e.dispose();
        beeEffects.clear();

        if (template != null) template.dispose();
        markerTexture.dispose();
        batch.dispose();
    }
}
