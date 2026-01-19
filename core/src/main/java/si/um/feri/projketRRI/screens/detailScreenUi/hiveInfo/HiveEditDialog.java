package si.um.feri.projketRRI.screens.detailScreenUi.hiveInfo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import si.um.feri.projketRRI.api.calls.model.Hive;

public class HiveEditDialog {

    public static class Result {
        public final String name;
        public final String location;
        public final String type;
        public final String status;
        public Result(String name, String location, String type, String status) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.status = status;
        }
    }

    public interface Callback {
        void onSave(Result result);
        void onCancel();
    }

    public static void show(Stage stage, Skin skin, Hive hive, Callback callback) {
        Dialog dialog = new Dialog("Edit Hive", skin);
        dialog.setModal(true);
        dialog.setMovable(true);
        dialog.setResizable(false);

        Table content = dialog.getContentTable();
        content.pad(10);

        final TextField nameField = new TextField(hive.name != null ? hive.name : "", skin);
        final TextField locationField = new TextField(hive.location != null ? hive.location : "", skin);

        final SelectBox<String> typeSelect = new SelectBox<>(skin);
        typeSelect.setItems("az", "lr", "db");
        if (hive.type != null) {
            String t = hive.type.trim().toLowerCase();
            if (t.equals("az") || t.equals("lr") || t.equals("db")) typeSelect.setSelected(t);
        }

        final CheckBox onlineCheck = new CheckBox(" Online", skin);
        onlineCheck.getLabel().setColor(Color.WHITE);
        onlineCheck.setChecked(isOnlineStatus(hive.status));

        content.add(new Label("Name", skin)).left().padBottom(6); content.row();
        content.add(nameField).width(420).left().padBottom(10); content.row();

        content.add(new Label("Location", skin)).left().padBottom(6); content.row();
        content.add(locationField).width(420).left().padBottom(10); content.row();

        content.add(new Label("Type (az/lr/db)", skin)).left().padBottom(6); content.row();
        content.add(typeSelect).width(220).left().padBottom(10); content.row();

        content.add(new Label("Status", skin)).left().padBottom(6); content.row();
        content.add(onlineCheck).left().padBottom(10); content.row();

        TextButton cancelBtn = new TextButton("Cancel", skin);
        TextButton saveBtn   = new TextButton("Save", skin);

        dialog.getButtonTable().add(cancelBtn);
        dialog.getButtonTable().add(saveBtn);

        saveBtn.pad(3, 6, 3, 6);
        cancelBtn.pad(3, 6, 3, 6);


        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
                if (callback != null) callback.onCancel();
            }
        });

        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String newName = nameField.getText().trim();
                String newLoc  = locationField.getText().trim();
                String newType = typeSelect.getSelected();
                String newStatus = onlineCheck.isChecked() ? "online" : "offline";

                if (callback != null) {
                    callback.onSave(new Result(newName, newLoc, newType, newStatus));
                }

                dialog.hide();
            }
        });

        dialog.show(stage);
    }

    private static boolean isOnlineStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("online") || s.equals("on") || s.equals("1") || s.equals("true");
    }
}
