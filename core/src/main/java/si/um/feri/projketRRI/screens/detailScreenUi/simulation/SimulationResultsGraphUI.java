package si.um.feri.projketRRI.screens.detailScreenUi.simulation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.List;

public class SimulationResultsGraphUI implements Disposable {

    private final Stage stage;
    private final Skin skin;

    private final SimulationResultsGraphActor graphActor;
    private final Label title;

    public SimulationResultsGraphUI() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        stage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);

        root.bottom().left().pad(12);
        root.padBottom(280);

        Table panel = new Table(skin);
        panel.background("default-round");
        panel.pad(12);

        title = new Label("Simulation", skin);
        title.setAlignment(Align.left);

        graphActor = new SimulationResultsGraphActor();
        graphActor.setSize(420, 180);

        panel.add(title).left().row();
        panel.add(graphActor).width(420).height(180).left().padTop(8);

        root.add(panel).right().bottom();
        stage.addActor(root);

        stage.getRoot().setVisible(false);
    }

    public Stage getStage() {
        return stage;
    }

    public void setVisible(boolean visible) {
        stage.getRoot().setVisible(visible);
    }

    public boolean isVisible() {
        return stage.getRoot().isVisible();
    }

    public void setTitle(String t) {
        title.setText(t != null ? t : "Simulation");
    }

    public void setResults(List<SimulationCalculator.DayResult> results) {
        graphActor.setResults(results);
        setVisible(results != null && results.size() >= 2);
    }

    public void render() {
        if (!isVisible()) return;
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
