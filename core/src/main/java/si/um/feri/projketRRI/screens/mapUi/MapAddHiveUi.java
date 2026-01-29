package si.um.feri.projketRRI.screens.mapUi;

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

public class MapAddHiveUi implements Disposable {

    public interface AddHiveListener {
        void onModeChanged(boolean isAddingMode);
        void onSave(String name, String type, String status, String locationDesc);
    }

    private final Stage stage;
    private final Skin skin;
    private final Table root;
    private final Table panel;

    private final TextButton toggleBtn;

    // Form Inputs
    private final TextField nameField;
    private final SelectBox<String> typeSelect;
    private final SelectBox<String> statusSelect;
    private final TextField locDescField; // For "location" string (e.g. "Orchard")

    // Read-only Coordinate displays
    private final Label latValueLbl;
    private final Label lngValueLbl;
    private final Label errorLbl;

    private boolean isAddingMode = false;
    private AddHiveListener listener;

    public MapAddHiveUi() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        stage = new Stage(new FitViewport(1200, 1200));

        root = new Table();
        root.setFillParent(true);
        root.bottom().right().pad(20);

        // Toggle Button
        toggleBtn = new TextButton("Add New Hive", skin);

        // Form Panel
        panel = new Table(skin);
        panel.background("default-round"); // Ensure this drawable exists in uiskin
        panel.pad(15);
        panel.setVisible(false);

        // --- Build Form ---
        Label title = new Label("New Hive Details", skin);
        title.setFontScale(1.1f);
        title.setColor(Color.YELLOW);

        nameField = new TextField("", skin);
        nameField.setMessageText("Hive Name");

        typeSelect = new SelectBox<>(skin);
        typeSelect.setItems("AZ", "LR", "DB");

        statusSelect = new SelectBox<>(skin);
        statusSelect.setItems("Online", "Offline");

        locDescField = new TextField("", skin);
        locDescField.setMessageText("Location Name (e.g. Garden)");

        // Coordinates (Read Only)
        latValueLbl = new Label("-", skin);
        lngValueLbl = new Label("-", skin);
        latValueLbl.setColor(Color.CYAN);
        lngValueLbl.setColor(Color.CYAN);

        errorLbl = new Label("", skin);
        errorLbl.setColor(Color.RED);
        errorLbl.setAlignment(Align.center);

        TextButton saveBtn = new TextButton("Save Hive", skin);
        TextButton cancelBtn = new TextButton("Cancel", skin);

        // Layout Form
        panel.add(title).colspan(2).center().padBottom(10).row();

        // Instruction
        Label hint = new Label("Tap map to set location", skin);
        hint.setFontScale(0.8f);
        hint.setColor(Color.LIGHT_GRAY);
        panel.add(hint).colspan(2).center().padBottom(10).row();

        addRow("Name:", nameField);
        addRow("Type:", typeSelect);
        addRow("Status:", statusSelect);
        addRow("Loc. Desc:", locDescField);
        addRow("Lat:", latValueLbl);
        addRow("Lng:", lngValueLbl);

        panel.add(errorLbl).colspan(2).padTop(5).padBottom(5).row();

        Table buttons = new Table();
        buttons.add(saveBtn).width(90).padRight(10);
        buttons.add(cancelBtn).width(90);
        panel.add(buttons).colspan(2).row();

        // Main Layout
        root.add(panel).width(300).row();
        root.add(toggleBtn).right().padTop(10);

        stage.addActor(root);

        // Event Listeners
        toggleBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setAddingMode(true);
            }
        });

        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setAddingMode(false);
            }
        });

        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                doSave();
            }
        });
    }

    private void addRow(String label, Actor widget) {
        panel.add(new Label(label, skin)).right().padRight(10);
        panel.add(widget).growX().left().row();
        panel.row().padTop(5);
    }

    private void setAddingMode(boolean active) {
        this.isAddingMode = active;
        panel.setVisible(active);
        toggleBtn.setVisible(!active); // Hide "Add" button when form is open
        errorLbl.setText("");

        if (!active) {
            // Clear fields on cancel
            nameField.setText("");
            locDescField.setText("");
            latValueLbl.setText("-");
            lngValueLbl.setText("-");
        }

        if (listener != null) listener.onModeChanged(active);
    }

    public void updateCoordinates(double lat, double lng) {
        latValueLbl.setText(String.format("%.6f", lat));
        lngValueLbl.setText(String.format("%.6f", lng));
        errorLbl.setText(""); // Clear error if they picked a point
    }

    private void doSave() {
        if (latValueLbl.getText().toString().equals("-")) {
            errorLbl.setText("Please tap the map to select a location!");
            return;
        }
        if (nameField.getText().trim().isEmpty()) {
            errorLbl.setText("Name is required!");
            return;
        }

        if (listener != null) {
            listener.onSave(
                nameField.getText(),
                typeSelect.getSelected().toLowerCase(),
                statusSelect.getSelected().toLowerCase(),
                locDescField.getText()
            );
        }
    }

    // Call this when API success to close the form
    public void reset() {
        setAddingMode(false);
    }

    public void setListener(AddHiveListener listener) {
        this.listener = listener;
    }

    public Stage getStage() { return stage; }
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }
    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }
    @Override public void dispose() { stage.dispose(); skin.dispose(); }
}
