package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class DetailPanel implements Disposable {
    private final SpriteBatch hudBatch = new SpriteBatch();
    private Texture[] tiles;
    private boolean loading = false;

    private final int zoom = 15;
    private final int numTiles = 3; // 3x3

    public void request(Geolocation g) {
        if (loading) return;
        loading = true;

        // IMPORTANT: must run on render thread if MapRasterTiles creates Textures
        Gdx.app.postRunnable(() -> {
            try {
                ZoomXY center = MapRasterTiles.getTileNumber(g.lat, g.lng, zoom);
                tiles = MapRasterTiles.getRasterTileZone(center, numTiles);
            } catch (Exception e) {
                Gdx.app.log("DETAIL", "Failed to load detail tiles", e);
            } finally {
                loading = false;
            }
        });
    }

    public void draw(int screenW, int screenH) {
        if (tiles == null) return;

        hudBatch.begin();

        // panel position on screen
        float panelSize = 256 * numTiles; // if your tiles are 256
        float x0 = screenW - panelSize - 20;
        float y0 = 20;

        int idx = 0;
        for (int row = numTiles - 1; row >= 0; row--) {
            for (int col = 0; col < numTiles; col++) {
                Texture t = tiles[idx++];
                if (t == null) continue;
                hudBatch.draw(t, x0 + col * 256, y0 + row * 256, 256, 256);
            }
        }

        hudBatch.end();
    }

    @Override
    public void dispose() {
        if (tiles != null) {
            for (Texture t : tiles) if (t != null) t.dispose();
            tiles = null;
        }
        hudBatch.dispose();
    }
}
