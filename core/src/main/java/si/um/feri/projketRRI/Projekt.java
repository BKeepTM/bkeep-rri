package si.um.feri.projketRRI;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.Gdx;

import si.um.feri.projketRRI.screens.LoginScreen;

public class Projekt extends com.badlogic.gdx.Game {
    public Skin skin;

    @Override
    public void create() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        setScreen(new LoginScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (skin != null) skin.dispose();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }
}
