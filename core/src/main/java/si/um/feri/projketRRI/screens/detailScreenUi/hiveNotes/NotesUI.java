package si.um.feri.projketRRI.screens.detailScreenUi.hiveNotes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import si.um.feri.projketRRI.api.calls.NotesService;
import si.um.feri.projketRRI.api.calls.model.Hive;
import si.um.feri.projketRRI.api.calls.model.Notes;

public class NotesUI implements Disposable {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SQL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Hive hive;

    public interface NotesActions {
        void requestReloadNotes();
        void showMessage(String msg);
        void showError(String msg);
    }

    private final Stage stage;
    private final Skin skin;

    private final Label titleLabel;
    private final Label statusLabel;

    private final Table listTable;
    private final ScrollPane scrollPane;

    private final TextButton addNoteButton;

    private boolean loading = false;

    private List<Notes> currentNotes = new ArrayList<>();

    private final DateTimeFormatter fmt =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    // dialogs
    private final ViewNoteDialog viewDialog;
    private final EditNoteDialog editDialog;
    private final AddNoteDialog addDialog;

    private final NotesActions actions;

    public NotesUI(Hive hive, NotesActions actions) {
        this.actions = actions;
        this.hive = hive;

        skin = new Skin(Gdx.files.internal("uiskin.json"));
        stage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        root.top().right().pad(12);

        Table panel = new Table(skin);
        panel.background("default-round");
        panel.pad(12);

        titleLabel = new Label("Notes:", skin);
        titleLabel.setColor(Color.WHITE);
        titleLabel.setFontScale(1.3f);

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.LIGHT_GRAY);
        statusLabel.setWrap(true);

        addNoteButton = new TextButton("+ Add Note", skin);

        listTable = new Table(skin);
        listTable.top().left();

        scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // Layout
        Table headerRow = new Table();
        headerRow.add(titleLabel).left().expandX();
        headerRow.add(addNoteButton).right();

        panel.add(headerRow).width(360).left().row();
        panel.add(statusLabel).width(360).left().padTop(6).row();
        panel.add(scrollPane).width(360).height(240).left().padTop(8).row();

        root.add(panel);
        stage.addActor(root);

        viewDialog = new ViewNoteDialog(skin);
        editDialog = new EditNoteDialog(skin);
        addDialog = new AddNoteDialog(skin);

        wireEvents();

        setLoading(false);
        setNotes(null, hive.id);
    }

    private void wireEvents() {
        addNoteButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (hive.id <= 0) {
                    showErrorLocal("No hive selected.");
                    return;
                }
                addDialog.open(stage, hive.id, new AddNoteDialog.AddHandler() {
                    @Override
                    public void onSubmit(String content, String isoTime) {
                        setLoading(true);
                        NotesService.createNoteAsync(content, isoTime, hive.id , new NotesService.CreateNoteCallback() {
                            @Override
                            public void onSuccess(int insertedId) {
                                setLoading(false);
                                if (actions != null) actions.showMessage("Note added.");
                                if (actions != null) actions.requestReloadNotes();
                            }

                            @Override
                            public void onError(String message) {
                                setLoading(false);
                                showErrorLocal(message);
                                if (actions != null) actions.showError(message);
                            }
                        });
                    }
                });
            }
        });
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

    public Hive getHive() {
        return hive;
    }


    public void setLoading(boolean loading) {
        this.loading = loading;
        if (loading) statusLabel.setText("Loading notes...");
    }

    public void setError(String msg) {
        this.loading = false;
        statusLabel.setText(msg != null ? msg : "Failed to load notes.");
        clearList();
    }

    public void setNotes(List<Notes> notes, int hiveId) {
        this.loading = false;
        this.hive.id = hiveId;

        clearList();

        currentNotes.clear();
        if (notes != null) currentNotes.addAll(notes);

        if (notes == null || notes.isEmpty()) {
            statusLabel.setText("No notes yet.");
            return;
        }

        statusLabel.setText("");

        // newest-first
        for (int i = notes.size() - 1; i >= 0; i--) {
            Notes n = notes.get(i);

            String time = formatTime(n.time);
            String content = (n.content != null) ? n.content : "";

            Table noteCard = new Table(skin);
            noteCard.background("default-rect");
            noteCard.pad(8);

            noteCard.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    openViewDialog(n);
                    return true;
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
        scrollPane.setScrollY(0);
    }

    private void openViewDialog(Notes note) {
        viewDialog.open(stage, note, new ViewNoteDialog.NoteActions() {
            @Override
            public void onEdit(Notes n) {
                editDialog.open(stage, n, new EditNoteDialog.EditHandler() {
                    @Override
                    public void onSubmit(String newContent, String newIsoTime) {
                        setLoading(true);

                        NotesService.updateNoteAsync(n.id, newContent, newIsoTime, hive.id, new NotesService.SimpleCallback() {
                            @Override
                            public void onSuccess(String message) {
                                setLoading(false);
                                if (actions != null) actions.showMessage("Note updated.");
                                if (actions != null) actions.requestReloadNotes();
                            }

                            @Override
                            public void onError(String message) {
                                setLoading(false);
                                showErrorLocal(message);
                                if (actions != null) actions.showError(message);
                            }
                        });
                    }
                });
            }

            @Override
            public void onRemove(Notes n) {
                ConfirmDialog confirm = new ConfirmDialog(skin);
                confirm.open(stage, "Remove note?", "Are you sure you want to delete this note?", new ConfirmDialog.Handler() {
                    @Override
                    public void onYes() {
                        setLoading(true);
                        NotesService.removeNoteAsync(n.id, new NotesService.SimpleCallback() {
                            @Override
                            public void onSuccess(String message) {
                                setLoading(false);
                                if (actions != null) actions.showMessage("Note removed.");
                                if (actions != null) actions.requestReloadNotes();
                            }

                            @Override
                            public void onError(String message) {
                                setLoading(false);
                                showErrorLocal(message);
                                if (actions != null) actions.showError(message);
                            }
                        });
                    }

                    @Override
                    public void onNo() {
                        // nothing
                    }
                });
            }
        });
    }

    private void clearList() {
        listTable.clearChildren();
    }

    private String formatTime(String value) {
        if (value == null || value.isEmpty()) return "-";
        try {
            Instant instant = Instant.parse(value);
            return DISPLAY_FMT.withZone(ZoneId.systemDefault())
                .format(instant);
        } catch (Exception ignored) {}
        try {
            LocalDateTime ldt = LocalDateTime.parse(value, SQL_FMT);
            return ldt.format(DISPLAY_FMT);
        } catch (Exception ignored) {}

        return value;
    }


    public boolean isMouseOverScroll() {
        Vector2 stageCoords = stage.screenToStageCoordinates(
            new Vector2(Gdx.input.getX(), Gdx.input.getY())
        );

        Actor hit = stage.hit(stageCoords.x, stageCoords.y, true);
        if (hit == null) return false;

        return hit.isDescendantOf(scrollPane);
    }

    private void showErrorLocal(String msg) {
        statusLabel.setText(msg != null ? msg : "Error");
        statusLabel.setColor(Color.SALMON);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
