package si.um.feri.projketRRI.screens.detailScreenUi.hiveNotes;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

class ConfirmDialog {
        interface Handler {
            void onYes();
            void onNo();
        }

        private final Dialog dialog;
        private final Label body;

        ConfirmDialog(Skin skin) {
            dialog = new Dialog("Confirm", skin);
            dialog.setModal(true);
            dialog.setMovable(true);

            body = new Label("", skin);
            body.setWrap(true);

            dialog.getContentTable().pad(12);
            dialog.getContentTable().add(body).width(420).left().row();
        }

        void open(Stage stage, String title, String message, Handler handler) {
            dialog.getTitleLabel().setText(title != null ? title : "Confirm");
            body.setText(message != null ? message : "Are you sure?");

            dialog.getButtonTable().clearChildren();

            TextButton no = new TextButton("No", dialog.getSkin());
            TextButton yes = new TextButton("Yes", dialog.getSkin());

            no.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    dialog.hide();
                    if (handler != null) handler.onNo();
                }
            });

            yes.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    dialog.hide();
                    if (handler != null) handler.onYes();
                }
            });

            dialog.getButtonTable().add(no).padRight(8);
            dialog.getButtonTable().add(yes);

            dialog.show(stage);
        }
    }
