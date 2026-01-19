package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Disposable;

import java.io.IOException;

public class RasterTileMap implements Disposable {

    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;

    private Texture[] mapTiles;
    private ZoomXY beginTile; // top-left tile

    private int tileZoom = 9;

    private int numTiles = Constants.NUM_TILES;
    public void setNumTiles(int n) { this.numTiles = n; }

    public void rebuild(Geolocation centerGeolocation) {
        // Dispose previous resources (IMPORTANT: dispose renderer too!)
        disposeInternal();

        try {
            ZoomXY centerTile = MapRasterTiles.getTileNumber(centerGeolocation.lat, centerGeolocation.lng, tileZoom);
            mapTiles = MapRasterTiles.getRasterTileZone(centerTile, numTiles);

            beginTile = new ZoomXY(
                tileZoom,
                centerTile.x - ((numTiles - 1) / 2),
                centerTile.y - ((numTiles - 1) / 2)
            );
        } catch (IOException e) {
            Gdx.app.log("MAP", "Failed to load tiles", e);
            return;
        }

        tiledMap = new TiledMap();
        MapLayers layers = tiledMap.getLayers();

        TiledMapTileLayer layer = new TiledMapTileLayer(
            numTiles, numTiles,
            MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE
        );

        int index = 0;
        for (int j = numTiles - 1; j >= 0; j--) {
            for (int i = 0; i < numTiles; i++) {
                Texture tex = mapTiles[index++];

                if (tex == null) continue;

                TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
                cell.setTile(new StaticTiledMapTile(new TextureRegion(tex)));
                layer.setCell(i, j, cell);
            }
        }

        Gdx.app.log("CACHE", "Path: " + Gdx.files.local("tilecache").file().getAbsolutePath());


        layers.add(layer);
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
    }

    public void render(OrthographicCamera camera) {
        if (tiledMapRenderer == null) return;
        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();
    }

    public ZoomXY getBeginTile() {
        return beginTile;
    }

    private void disposeInternal() {
        if (tiledMapRenderer != null) {
            // OrthogonalTiledMapRenderer is Disposable
            ((OrthogonalTiledMapRenderer) tiledMapRenderer).dispose();
            tiledMapRenderer = null;
        }

        if (tiledMap != null) {
            tiledMap.dispose();
            tiledMap = null;
        }

        if (mapTiles != null) {
            for (Texture t : mapTiles) if (t != null) t.dispose();
            mapTiles = null;
        }
    }

    public void setTileZoom(int tileZoom) {
        this.tileZoom = tileZoom;
    }

    public int getTileZoom() {
        return tileZoom;
    }

    @Override
    public void dispose() {
        disposeInternal();
    }
}
