package si.um.feri.projketRRI.screens.detailScreenUi.hiveWeight;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import si.um.feri.projketRRI.api.calls.model.HiveWeight;

public class HiveWeightsGraphUI implements Disposable {

    private final Stage stage;
    private final Skin skin;

    private final HiveWeightsGraphActor graphActor;
    private final Label title;

    public HiveWeightsGraphUI() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        stage = new Stage(new FitViewport(1200, 1200));


        Table root = new Table();
        root.setFillParent(true);
        root.bottom().left().pad(12);

        Table panel = new Table(skin);
        panel.background("default-round");
        panel.pad(12);

        title = new Label("Hive weight", skin);

        graphActor = new HiveWeightsGraphActor();
        graphActor.setSize(420, 180);

        panel.add(title).left().row();
        panel.add(graphActor).width(420).height(180).left().padTop(8);

        root.add(panel).left().bottom();
        stage.addActor(root);
    }

    public Stage getStage() {
        return stage;
    }

    public void setTitle(String t) {
        title.setText(t != null ? t : "Hive weight");
    }

    public void setWeights(Array<HiveWeight> weights) {
        graphActor.setWeights(weights);
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        graphActor.dispose();
        stage.dispose();
        skin.dispose();
    }
}
