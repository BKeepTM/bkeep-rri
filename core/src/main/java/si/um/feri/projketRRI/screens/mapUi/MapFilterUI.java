package si.um.feri.projketRRI.screens.mapUi;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Value;

public class MapFilterUI implements Disposable {

    public enum StatusFilter { ALL, ONLINE, OFFLINE }
    public enum TypeFilter { ALL, AZ, LR, DB }

    public interface FilterListener {
        void onFilterChanged(StatusFilter status, TypeFilter type);
    }

    private final Skin skin;

    private final Table root;
    private final Table panel;
    private Cell<?> panelCell;

    private final TextButton toggleBtn;
    private final SelectBox<String> statusSelect;
    private final SelectBox<String> typeSelect;
    private final TextButton clearBtn;

    private boolean panelVisible = true;
    private FilterListener listener;

    public MapFilterUI(Skin skin) {
        this.skin = skin;

        root = new Table(); // component root (NOT fill parent)

        toggleBtn = new TextButton("Hide Filters", skin);

        panel = new Table(skin);
        panel.setBackground("default-round");
        panel.pad(10);

        // --- Build panel contents ---
        Label title = new Label("Map Filters", skin);
        title.setColor(Color.WHITE);
        title.setFontScale(1f);

        Label statusLbl = new Label("Status", skin);
        statusLbl.setColor(Color.WHITE);

        Label typeLbl = new Label("Type", skin);
        typeLbl.setColor(Color.WHITE);

        statusSelect = new SelectBox<>(skin);
        statusSelect.setItems("All", "Online", "Offline");
        statusSelect.setSelected("All");

        typeSelect = new SelectBox<>(skin);
        typeSelect.setItems("All", "az", "lr", "db");
        typeSelect.setSelected("All");

        clearBtn = new TextButton("Clear", skin);

        panel.add(title).center().colspan(2).row();
        panel.row().padTop(8);

        panel.add(statusLbl).right().padRight(10);
        panel.add(statusSelect).right().width(160).row();
        panel.row().padTop(6);

        panel.add(typeLbl).right().padRight(10);
        panel.add(typeSelect).right().width(160).row();
        panel.row().padTop(10);

        panel.add(clearBtn).right().colspan(2).row();

        root.add(toggleBtn).right().row();
        root.row().padTop(8);

        panelCell = root.add(panel).right();

        panel.setVisible(true);
        panel.setTouchable(Touchable.enabled);
        panelCell.height(Value.prefHeight);
        panelCell.padTop(8);

        wireEvents();
    }

    private void wireEvents() {
        toggleBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                panelVisible = !panelVisible;

                panel.setVisible(panelVisible);
                panel.setTouchable(panelVisible ? Touchable.enabled : Touchable.disabled);

                panelCell.height(panelVisible ? Value.prefHeight : Value.zero);
                panelCell.padTop(panelVisible ? 8 : 0);

                root.invalidateHierarchy();
                toggleBtn.setText(panelVisible ? "Hide Filters" : "Show Filters");
            }
        });

        ChangeListener notify = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                fireFilterChanged();
            }
        };

        statusSelect.addListener(notify);
        typeSelect.addListener(notify);

        clearBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                statusSelect.setSelected("All");
                typeSelect.setSelected("All");
                fireFilterChanged();
            }
        });
    }

    private void fireFilterChanged() {
        if (listener == null) return;
        listener.onFilterChanged(getStatusFilter(), getTypeFilter());
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    public StatusFilter getStatusFilter() {
        String s = statusSelect.getSelected();
        if ("Online".equalsIgnoreCase(s)) return StatusFilter.ONLINE;
        if ("Offline".equalsIgnoreCase(s)) return StatusFilter.OFFLINE;
        return StatusFilter.ALL;
    }

    public TypeFilter getTypeFilter() {
        String t = typeSelect.getSelected();
        if ("az".equalsIgnoreCase(t)) return TypeFilter.AZ;
        if ("lr".equalsIgnoreCase(t)) return TypeFilter.LR;
        if ("db".equalsIgnoreCase(t)) return TypeFilter.DB;
        return TypeFilter.ALL;
    }

    public Table getRoot() {
        return root;
    }

    @Override
    public void dispose() {
    }
}
