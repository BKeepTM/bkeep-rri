package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

public class TilePrefetcher {
    public static void prefetchAroundMarkersAsync(Array<Geolocation> markers, int zoom, int numTiles) {
        new Thread(() -> {
            for (Geolocation g : markers) {
                try {
                    ZoomXY center = MapRasterTiles.getTileNumber(g.lat, g.lng, zoom);
                    MapRasterTiles.prefetchRasterTileZone(center, numTiles);
                } catch (Exception e) {
                    Gdx.app.log("PREFETCH", "Failed prefetch at zoom " + zoom, e);
                }
            }
        }).start();
    }
}
