package si.um.feri.projketRRI.screens.mapUi;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MapFilterUI implements Disposable {

    public enum StatusFilter { ALL, ONLINE, OFFLINE }
    public enum TypeFilter { ALL, AZ, LR, DB }

    public interface FilterListener {
        void onFilterChanged(StatusFilter status, TypeFilter type);
    }

    private final Stage stage;
    private final Skin skin;

    private final Table root;
    private final Table panel;

    private final TextButton toggleBtn;
    private final SelectBox<String> statusSelect;
    private final SelectBox<String> typeSelect;
    private final TextButton clearBtn;

    private boolean panelVisible = true;

    private FilterListener listener;

    public MapFilterUI() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        stage = new Stage(new ScreenViewport());

        root = new Table();
        root.setFillParent(true);
        root.top().right().pad(12);
        root.padTop(120);

        toggleBtn = new TextButton("Hide Filters", skin);

        panel = new Table(skin);
        panel.background("default-round");
        panel.pad(10);

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

        // Layout
        root.add(toggleBtn).right().row();

        panel.add(title).center().colspan(2).row();
        panel.row().padTop(8);

        panel.add(statusLbl).right().padRight(10);
        panel.add(statusSelect).right().width(160).row();
        panel.row().padTop(6);

        panel.add(typeLbl).right().padRight(10);
        panel.add(typeSelect).right().width(160).row();
        panel.row().padTop(10);

        panel.add(clearBtn).right().colspan(2).row();

        root.row().padTop(8);
        root.add(panel).right();

        stage.addActor(root);

        wireEvents();
    }

    private void wireEvents() {
        toggleBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                panelVisible = !panelVisible;
                panel.setVisible(panelVisible);
                toggleBtn.setText(panelVisible ? "Hide Filters" : "Show Filters");
            }
        });

        ChangeListener notify = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                fireFilterChanged();
            }
        };

        statusSelect.addListener(notify);
        typeSelect.addListener(notify);

        clearBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
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

    public Stage getStage() {
        return stage;
    }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
