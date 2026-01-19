package si.um.feri.projketRRI.utils;

import static si.um.feri.projketRRI.utils.MapRasterTiles.TILE_SIZE;

import com.badlogic.gdx.Gdx;

public class Constants {
    public static final int NUM_TILES =5;//5
    public static int ZOOM = 9;//9
    public static final int MAP_WIDTH = TILE_SIZE * NUM_TILES;
    public static final int MAP_HEIGHT = TILE_SIZE * NUM_TILES;
    public static final int HUD_WIDTH = Gdx.graphics.getWidth();
    public static final int HUD_HEIGHT = Gdx.graphics.getHeight();
    public static final String API_URL = "https://pi.darkosever.si";
    //public static final String API_URL = "http://localhost:3000";

}
