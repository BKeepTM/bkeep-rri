package si.um.feri.projketRRI.screens.detailScreenUi;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class HiveInfoUI implements Disposable {

    private final Stage stage;
    private final Skin skin;

    private final Label nameValue;
    private final Label locationValue;
    private final Label typeValue;
    private final Label statusValue;

    public HiveInfoUI() {
        // You must have assets/uiskin.json (or change this path to your skin)
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        this.stage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        root.top().left().pad(12);

        Table panel = new Table(skin);
        panel.background("default-round");
        panel.pad(12);

        Label.LabelStyle keyStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.LIGHT_GRAY);
        Label.LabelStyle valStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE);

        nameValue = new Label("-", valStyle);
        locationValue = new Label("-", valStyle);
        typeValue = new Label("-", valStyle);
        statusValue = new Label("-", valStyle);

        // Row helper
        addRow(panel, keyStyle, "Name:", nameValue);
        addRow(panel, keyStyle, "Location:", locationValue);
        addRow(panel, keyStyle, "Type:", typeValue);
        addRow(panel, keyStyle, "Status:", statusValue);

        root.add(panel).left().top();
        stage.addActor(root);
    }

    private void addRow(Table t, Label.LabelStyle keyStyle, String key, Label valueLabel) {
        Label k = new Label(key, keyStyle);
        k.setWrap(false);

        valueLabel.setWrap(true);

        t.add(k).left().padRight(10);
        t.add(valueLabel).left().width(420).row(); // width controls wrapping
        t.row().padTop(6);
    }

    public Stage getStage() {
        return stage;
    }

    public void setHiveData(String name, String location, String type, String status) {
        nameValue.setText(name != null ? name : "-");
        locationValue.setText(location != null ? location : "-");
        typeValue.setText(type != null ? type : "-");
        statusValue.setText(status != null ? status : "-");
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
        stage.dispose();
        skin.dispose();
    }
}
