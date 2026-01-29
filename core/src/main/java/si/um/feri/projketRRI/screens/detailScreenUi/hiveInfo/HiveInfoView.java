package si.um.feri.projketRRI.screens.detailScreenUi.hiveInfo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import si.um.feri.projketRRI.api.calls.model.Hive;

public class HiveInfoView implements Disposable {

    public interface StatusToggleListener {
        void onToggle(boolean online);
    }

    public interface EditClickListener {
        void onEditClick();
    }

    private final Stage stage;
    private final Skin skin;

    private final Label nameValue;
    private final Label locationValue;
    private final Label typeValue;

    private final Label statusValue;
    private final Button statusToggle;
    private final Label statusText;

    private final TextButton editButton;

    private final Label feedbackLabel;

    private boolean suppressStatusSwitchEvent = false;

    private StatusToggleListener statusToggleListener;
    private EditClickListener editClickListener;

    public HiveInfoView() {
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        this.stage = new Stage(new FitViewport(1200, 1200));


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

        statusToggle = new Button(skin);
        statusToggle.setChecked(false);

        statusText = new Label("OFFLINE", skin);
        statusText.setColor(Color.WHITE);
        statusText.setAlignment(Align.center);

        Container<Label> toggleContent = new Container<>(statusText);
        toggleContent.pad(3, 6, 3, 6);

        statusToggle.add(toggleContent);
        updateToggleVisual(false);

        editButton = new TextButton("Edit", skin);
        editButton.pad(3, 6, 3, 6);

        feedbackLabel = new Label("", valStyle);
        feedbackLabel.setColor(Color.SALMON);
        feedbackLabel.setWrap(true);

        addRow(panel, keyStyle, "Name:", nameValue);
        addRow(panel, keyStyle, "Location:", locationValue);
        addRow(panel, keyStyle, "Type:", typeValue);
        addStatusRow(panel, keyStyle, "Status:", statusToggle);

        Table actionRow = new Table();

        actionRow.add(editButton)
            .padRight(12)
            .left();

        feedbackLabel.setWrap(true);

        actionRow.add(feedbackLabel)
            .left()
            .width(320)
            .padLeft(12);

        panel.add(actionRow)
            .padTop(20)
            .colspan(2)
            .left()
            .row();


        root.add(panel).left().top();
        stage.addActor(root);

        wireUiEvents();
    }

    private void wireUiEvents() {
        statusToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (suppressStatusSwitchEvent) return;

                boolean online = statusToggle.isChecked();
                updateToggleVisual(online);

                if (statusToggleListener != null) {
                    statusToggleListener.onToggle(online);
                }
            }
        });


        editButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (editClickListener != null) editClickListener.onEditClick();
            }
        });
    }

    private void updateToggleVisual(boolean online) {
        statusText.setText(online ? "ONLINE" : "OFFLINE");

        Color bg = online
            ? new Color(0.15f, 0.6f, 0.25f, 1f)   // green
            : new Color(0.35f, 0.35f, 0.35f, 1f); // gray

        statusToggle.setColor(bg);
    }

    private void addRow(Table t, Label.LabelStyle keyStyle, String key, Label valueLabel) {
        Label k = new Label(key, keyStyle);
        k.setWrap(false);

        valueLabel.setWrap(true);

        t.add(k).left().padRight(10);
        t.add(valueLabel).left().width(330).row();
        t.row().padTop(6);
    }

    private void addStatusRow(Table t, Label.LabelStyle keyStyle, String key, Actor toggle) {
        Label k = new Label(key, keyStyle);
        k.setWrap(false);
        Table right = new Table();
        right.add(toggle).left();

        t.add(k).left().padRight(10);
        t.add(right).left().row();
        t.row().padTop(6);
    }

    public Stage getStage() { return stage; }
    public Skin getSkin() { return skin; }

    public void setOnStatusToggle(StatusToggleListener listener) { this.statusToggleListener = listener; }
    public void setOnEditClick(EditClickListener listener) { this.editClickListener = listener; }

    public void setHive(Hive hive) {
        String name = hive != null ? hive.name : "-";
        String location = hive != null ? hive.location : "-";
        String type = hive != null ? hive.type : "-";
        String status = hive != null ? hive.status : "-";

        nameValue.setText(name != null ? name : "-");
        locationValue.setText(location != null ? location : "-");
        typeValue.setText(type != null ? type : "-");

        statusValue.setText(status != null ? status : "-");

        boolean online = isOnlineStatus(status);
        suppressStatusSwitchEvent = true;
        statusToggle.setChecked(online);
        updateToggleVisual(online);
        suppressStatusSwitchEvent = false;

    }

    public void setBusy(String msg) {
        feedbackLabel.setColor(Color.LIGHT_GRAY);
        feedbackLabel.setText(msg != null ? msg : "");
    }

    public void showOk(String msg) {
        feedbackLabel.setColor(Color.GREEN);
        feedbackLabel.setText(msg != null ? msg : "");
    }

    public void showError(String msg) {
        feedbackLabel.setColor(Color.SALMON);
        feedbackLabel.setText(msg != null ? msg : "Error");
    }

    public void clearFeedback() {
        feedbackLabel.setText("");
    }

    public void setStatusSwitchChecked(boolean online) {
        suppressStatusSwitchEvent = true;
        statusToggle.setChecked(online);
        updateToggleVisual(online);
        suppressStatusSwitchEvent = false;
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    private boolean isOnlineStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("online") || s.equals("on") || s.equals("1") || s.equals("true");
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
