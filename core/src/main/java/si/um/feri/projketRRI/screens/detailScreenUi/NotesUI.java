package si.um.feri.projketRRI.screens.detailScreenUi;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import si.um.feri.projketRRI.api.calls.model.Notes;

public class NotesUI implements Disposable {

    private final Stage stage;
    private final Skin skin;

    private final Label titleLabel;
    private final Label statusLabel;   // loading/error/empty

    private final Table listTable;
    private final ScrollPane scrollPane;

    private boolean loading = false;

    private final Dialog dialog;
    private final Label dialogContent;



    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    public NotesUI() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        stage = new Stage(new ScreenViewport());

        dialogContent = new Label("", skin);
        dialogContent.setWrap(true);

        dialog = new com.badlogic.gdx.scenes.scene2d.ui.Dialog("Note", skin) {
            @Override
            protected void result(Object object) {
                hide();
            }
        };
        dialog.getContentTable().add(dialogContent).width(420).pad(12);
        dialog.button("Close");


        Table root = new Table();
        root.setFillParent(true);

        // Panel on the right side (adjust as you like)
        root.top().right().pad(12);

        Table panel = new Table(skin);
        // If your skin doesn't have this drawable, remove this line
        panel.background("default-round");
        panel.pad(12);

        titleLabel = new Label("Notes:", skin);
        titleLabel.setColor(Color.WHITE);

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.LIGHT_GRAY);
        statusLabel.setWrap(true);

        listTable = new Table(skin);
        listTable.top().left();

        scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // only vertical

        // Layout
        panel.add(titleLabel).left().row();
        panel.add(statusLabel).width(320).left().padTop(6).row();
        panel.add(scrollPane).width(360).height(220).left().padTop(8).row();

        root.add(panel);
        stage.addActor(root);

        setLoading(false);
        setNotes(null);
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

    public void setLoading(boolean loading) {
        this.loading = loading;
        if (loading) {
            statusLabel.setText("Loading notes...");
        } else {
            // keep whatever state setNotes/setError sets
        }
    }

    public void setError(String msg) {
        this.loading = false;
        statusLabel.setText(msg != null ? msg : "Failed to load notes.");
        clearList();
    }

    public void setNotes(List<Notes> notes) {
        this.loading = false;

        clearList();

        if (notes == null || notes.isEmpty()) {
            statusLabel.setText("No notes yet.");
            return;
        }

        statusLabel.setText(""); // hide status

        // Display newest-first (optional). If you want oldest-first, remove loop reverse.
        for (int i = notes.size() - 1; i >= 0; i--) {
            Notes n = notes.get(i);

            String time = formatTime(n.time);
            String content = (n.content != null) ? n.content : "";

            Table noteCard = new Table(skin);
            // If skin doesn't have it, remove background line
            noteCard.background("default-rect");
            noteCard.pad(8);

            final String fullText = content;
            final String title = time; // or "Note" or hive name

            noteCard.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                         float x, float y, int pointer, int button) {
                    showNotePopup(title, fullText);
                    return true; // consume
                }
            });

            Label timeLabel = new Label(time, skin);
            timeLabel.setColor(Color.GRAY);

            Label contentLabel = new Label(content, skin);
            contentLabel.setWrap(true);
            contentLabel.setColor(Color.WHITE);

            noteCard.add(timeLabel).left().row();
            noteCard.add(contentLabel).width(330).left().padTop(4).row();

            listTable.add(noteCard).width(350).left().padBottom(8).row();
        }

        listTable.invalidateHierarchy();
        scrollPane.layout();
        scrollPane.setScrollY(0); // top
    }

    private void showNotePopup(String title, String content) {
        dialog.getTitleLabel().setText(title != null ? title : "Note");
        dialogContent.setText(content != null ? content : "");
        dialog.show(stage);
    }

    private void clearList() {
        listTable.clearChildren();
    }

    private String formatTime(String iso) {
        if (iso == null || iso.isEmpty()) return "-";
        try {
            return fmt.format(Instant.parse(iso));
        } catch (Exception e) {
            return iso; // fallback raw
        }
    }

    public boolean isMouseOverScroll() {
        Vector2 stageCoords = stage.screenToStageCoordinates(
            new Vector2(Gdx.input.getX(), Gdx.input.getY())
        );

        Actor hit = scrollPane.hit(stageCoords.x, stageCoords.y, true);
        return hit != null;
    }


    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
