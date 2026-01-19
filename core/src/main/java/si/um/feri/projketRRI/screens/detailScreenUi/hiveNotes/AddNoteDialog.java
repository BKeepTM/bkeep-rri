package si.um.feri.projketRRI.screens.detailScreenUi.hiveNotes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class AddNoteDialog {
        interface AddHandler {
            void onSubmit(String content, String isoTime);
        }

        private final Dialog dialog;
        private final TextArea contentArea;
        private final TextField timeField;
        private final Label errorLabel;

        AddNoteDialog(Skin skin) {
            dialog = new Dialog("Add Note", skin);
            dialog.setModal(true);
            dialog.setMovable(true);

            contentArea = new TextArea("", skin);
            contentArea.setPrefRows(6);

            timeField = new TextField("", skin);
            timeField.setMessageText("Time e.g. yyyy-MM-dd HH:mm:ss");

            errorLabel = new Label("", skin);
            errorLabel.setColor(Color.SALMON);
            errorLabel.setWrap(true);

            Table c = dialog.getContentTable();
            c.pad(12);

            c.add(new Label("Content", skin)).left().row();
            c.add(contentArea).width(440).height(160).left().padTop(6).row();

            c.add(new Label("Time", skin)).left().padTop(10).row();
            c.add(timeField).width(440).left().padTop(6).row();

            c.add(errorLabel).width(440).left().padTop(10).row();
        }

        void open(Stage stage, int hiveId, AddHandler handler) {
            contentArea.setText("");
            timeField.setText(defaultNowSql());
            errorLabel.setText("");

            dialog.getButtonTable().clearChildren();

            TextButton cancel = new TextButton("Cancel", dialog.getSkin());
            TextButton add = new TextButton("Add", dialog.getSkin());

            cancel.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) { dialog.hide(); }
            });

            add.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String content = contentArea.getText() != null ? contentArea.getText().trim() : "";
                    String iso = timeField.getText() != null ? timeField.getText().trim() : "";

                    if (content.isEmpty()) {
                        errorLabel.setText("Content cannot be empty.");
                        return;
                    }
                    if (iso.isEmpty()) {
                        errorLabel.setText("Time cannot be empty.");
                        return;
                    }

                    try {
                        LocalDateTime.parse(iso,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        );
                    } catch (Exception e) {
                        errorLabel.setText("Invalid time. Example: yyyy-MM-dd HH:mm:ss");
                        return;
                    }

                    dialog.hide();
                    if (handler != null) handler.onSubmit(content, iso);
                }
            });

            dialog.getButtonTable().add(cancel).padRight(8);
            dialog.getButtonTable().add(add);

            dialog.show(stage);
        }

    private static String defaultNowSql() {
        return LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    }
