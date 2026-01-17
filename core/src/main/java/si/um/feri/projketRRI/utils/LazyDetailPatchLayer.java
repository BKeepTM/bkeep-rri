package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Queue;

import java.io.IOException;

public class LazyDetailPatchLayer implements Disposable {

    private static class Patch {
        int zoom;
        int tiles;
        ZoomXY beginTile;
        Texture[] textures;
        boolean ready;
        boolean loading;
    }

    private static class Request {
        int key;
        Geolocation center;
    }

    private final SpriteBatch batch = new SpriteBatch();

    private final int patchZoom;
    private final int patchTiles;

    // cache: key -> patch
    private final IntMap<Patch> patches = new IntMap<>();

    // queue of patch requests (marker-based)
    private final Queue<Request> queue = new Queue<>();

    // control how many patches to load concurrently
    private int maxInFlight = 1;
    private int inFlight = 0;

    public LazyDetailPatchLayer(int patchZoom, int patchTiles) {
        this.patchZoom = patchZoom;
        this.patchTiles = patchTiles;
    }

    /** Clear all patches & cancel queue. Call when markers change a lot. */
    public void reset() {
        // dispose textures
        for (Patch p : patches.values()) disposePatch(p);
        patches.clear();
        queue.clear();
        inFlight = 0;
    }

    /** Enqueue patch for a marker if not already cached. */
    public void requestPatchIfNeeded(Geolocation marker) {
        int key = keyForMarker(marker);
        Patch existing = patches.get(key);
        if (existing != null) return; // already cached or loading

        Patch p = new Patch();
        p.zoom = patchZoom;
        p.tiles = patchTiles;
        p.ready = false;
        p.loading = false;
        patches.put(key, p);

        Request r = new Request();
        r.key = key;
        r.center = marker;
        queue.addLast(r);
    }

    /** Call from render/update. Starts background loads. */
    public void updateLoading() {
        if (inFlight >= maxInFlight || queue.isEmpty()) return;

        Request r = queue.removeFirst();
        Patch p = patches.get(r.key);
        if (p == null || p.ready || p.loading) return;

        p.loading = true;
        inFlight++;

        // IMPORTANT: run on GL/render thread
        Gdx.app.postRunnable(() -> {
            try {
                ZoomXY centerTile = MapRasterTiles.getTileNumber(
                    r.center.lat, r.center.lng, patchZoom
                );
                int half = (patchTiles - 1) / 2;
                p.beginTile = new ZoomXY(
                    patchZoom,
                    centerTile.x - half,
                    centerTile.y - half
                );

                p.textures = MapRasterTiles.getRasterTileZone(centerTile, patchTiles);
                p.ready = true;
            } catch (Exception e) {
                Gdx.app.log("PATCH", "Failed to load patch", e);
            } finally {
                p.loading = false;
                inFlight--;
            }
        });
    }


    public void draw(OrthographicCamera camera, ZoomXY bgBeginTile, int bgZoom) {
        if (bgBeginTile == null) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (IntMap.Entry<Patch> entry : patches.entries()) {
            Patch p = entry.value;
            if (p == null || !p.ready || p.textures == null) continue;
            drawPatch(p, bgBeginTile, bgZoom);
        }

        batch.end();
    }

    private void drawPatch(Patch p, ZoomXY bgBeginTile, int bgZoom) {
        int zp = p.zoom;
        int scalePow = zp - bgZoom;
        if (scalePow <= 0) return;

        float scale = (float) Math.pow(2, scalePow);
        float tileSizeBg = MapRasterTiles.TILE_SIZE / scale;

        int idx = 0;
        for (int row = p.tiles - 1; row >= 0; row--) {
            for (int col = 0; col < p.tiles; col++) {
                Texture tex = p.textures[idx++];
                if (tex == null) continue;

                int tileX = p.beginTile.x + col;
                int tileY = p.beginTile.y + row;

                float pxPatch = tileX * MapRasterTiles.TILE_SIZE;
                float pyPatch = tileY * MapRasterTiles.TILE_SIZE;

                float bgBeginX_patchZoom = (bgBeginTile.x * MapRasterTiles.TILE_SIZE) * scale;
                float bgBeginY_patchZoom = (bgBeginTile.y * MapRasterTiles.TILE_SIZE) * scale;

                float relPatchX = pxPatch - bgBeginX_patchZoom;
                float relPatchY = pyPatch - bgBeginY_patchZoom;

                float xBg = relPatchX / scale;
                float yBg = relPatchY / scale;

                batch.draw(tex, xBg, yBg, tileSizeBg, tileSizeBg);
            }
        }
    }

    private int keyForMarker(Geolocation g) {
        // stable-ish key: quantize to patch tile at patchZoom
        ZoomXY t = MapRasterTiles.getTileNumber(g.lat, g.lng, patchZoom);
        // pack zoom not needed because patchZoom is fixed here
        return (t.x * 73856093) ^ (t.y * 19349663);
    }

    private void disposePatch(Patch p) {
        if (p.textures != null) {
            for (Texture t : p.textures) if (t != null) t.dispose();
            p.textures = null;
        }
    }

    @Override
    public void dispose() {
        reset();
        batch.dispose();
    }
}
