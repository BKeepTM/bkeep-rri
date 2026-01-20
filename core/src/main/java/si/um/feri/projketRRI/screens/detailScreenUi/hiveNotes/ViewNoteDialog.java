package si.um.feri.projketRRI.screens.detailScreenUi.hiveNotes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import si.um.feri.projketRRI.api.calls.model.Notes;

class ViewNoteDialog {
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SQL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    interface NoteActions {
        void onEdit(Notes n);
        void onRemove(Notes n);
    }

    private final Dialog dialog;
    private final Label timeLabel;
    private final Label contentLabel;
    private Notes note;

    ViewNoteDialog(Skin skin) {
        timeLabel = new Label("", skin);
        timeLabel.setColor(Color.LIGHT_GRAY);

        contentLabel = new Label("", skin);
        contentLabel.setWrap(true);
        contentLabel.setColor(Color.WHITE);

        dialog = new Dialog("Note", skin);
        dialog.setModal(true);
        dialog.setMovable(true);

        dialog.getContentTable().pad(12);
        dialog.getContentTable().add(timeLabel).left().row();
        dialog.getContentTable().add(contentLabel).width(420).left().padTop(10).row();
    }

    void open(Stage stage, Notes note, NoteActions actions) {
        this.note = note;

        timeLabel.setText(formatTime(note.time));
        contentLabel.setText(note.content != null ? note.content : "");

        dialog.getButtonTable().clearChildren();

        TextButton editBtn = new TextButton("Edit", dialog.getSkin());
        editBtn.pad(3, 6, 3, 6);
        TextButton removeBtn = new TextButton("Remove", dialog.getSkin());
        removeBtn.pad(3, 6, 3, 6);
        TextButton closeBtn = new TextButton("Close", dialog.getSkin());
        closeBtn.pad(3, 6, 3, 6);

        editBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
                if (actions != null) actions.onEdit(note);
            }
        });

        removeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
                if (actions != null) actions.onRemove(note);
            }
        });

        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        dialog.getButtonTable().add(editBtn).padRight(8);
        dialog.getButtonTable().add(removeBtn).padRight(8);
        dialog.getButtonTable().add(closeBtn);

        dialog.show(stage);
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
}


