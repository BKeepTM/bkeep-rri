package si.um.feri.projketRRI.screens.mapUi;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Value;

public class MapAddHiveUi implements Disposable {

    public interface AddHiveListener {
        void onModeChanged(boolean isAddingMode);
        void onSave(String name, String type, String status, String locationDesc);
    }

    private final Skin skin;

    private final Table root;
    private final Table panel;
    private Cell<?> panelCell;

    private final TextButton toggleBtn;

    // Form Inputs
    private final TextField nameField;
    private final SelectBox<String> typeSelect;
    private final SelectBox<String> statusSelect;
    private final TextField locDescField;

    // Read-only Coordinate displays
    private final Label latValueLbl;
    private final Label lngValueLbl;
    private final Label errorLbl;

    private boolean isAddingMode = false;
    private AddHiveListener listener;


    public MapAddHiveUi(Skin skin) {
        this.skin = skin;

        root = new Table();

        toggleBtn = new TextButton("Add New Hive", skin);

        // Form Panel
        panel = new Table(skin);
        panel.setBackground("default-round");
        panel.pad(15);
        panel.setVisible(false);
        panel.setTouchable(Touchable.disabled);

        // --- Build Form ---
        Label title = new Label("New Hive Details", skin);
        title.setFontScale(1f);
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

        Label hint = new Label("Tap map to set location", skin);
        hint.setFontScale(1f);
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

        root.add(toggleBtn).right().row();
        root.row().padTop(8);

        panelCell = root.add(panel).width(300).right(); // keep cell reference

        // Start collapsed (so it doesn't reserve space when hidden)
        panelCell.height(0);
        panelCell.padTop(0);

        // --- Event Listeners ---
        toggleBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setAddingMode(!isAddingMode);
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

        panel.clearActions();

        panel.setVisible(active);
        panel.setTouchable(active ? Touchable.enabled : Touchable.disabled);

        panelCell.height(active ? Value.prefHeight : Value.zero);
        panelCell.padTop(active ? 8 : 0);

        root.invalidateHierarchy();

        if (active) {
            panel.getColor().a = 0f;
            panel.addAction(Actions.fadeIn(0.2f));
        }

        toggleBtn.setText(active ? "Close" : "Add New Hive");
        errorLbl.setText("");

        if (!active) {
            // Clear fields on close/cancel
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
        errorLbl.setText("");
    }

    private void doSave() {
        if ("-".equals(latValueLbl.getText().toString())) {
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

    public Table getRoot() {
        return root;
    }

    @Override
    public void dispose() {
    }
}
