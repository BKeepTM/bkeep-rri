package si.um.feri.projketRRI.screens.detailScreenUi.hiveInfo;

import si.um.feri.projketRRI.api.calls.HiveService;
import si.um.feri.projketRRI.api.calls.model.Hive;

public class HiveInfoController {

    private final HiveInfoView view;
    private Hive currentHive;

    public HiveInfoController(HiveInfoView view) {
        this.view = view;

        view.setOnStatusToggle(this::onStatusToggled);
        view.setOnEditClick(this::onEditClicked);
    }

    public void setHive(Hive hive) {
        this.currentHive = hive;
        view.setHive(hive);
        view.clearFeedback();
    }

    private void onStatusToggled(boolean online) {
        if (currentHive == null) {
            view.showError("No hive selected.");
            view.setStatusSwitchChecked(false);
            return;
        }

        String newStatus = online ? "online" : "offline";
        currentHive.status = newStatus;

        view.setBusy("Saving status...");
        HiveService.updateHive(currentHive, new HiveService.HiveSingleCallback() {
            @Override
            public void onSuccess(Hive hiveFromServer) {
                if (hiveFromServer != null && hiveFromServer.id != 0) {
                    if (hiveFromServer.name == null) hiveFromServer.name = currentHive.name;
                    if (hiveFromServer.location == null) hiveFromServer.location = currentHive.location;
                    if (hiveFromServer.type == null) hiveFromServer.type = currentHive.type;
                    if (hiveFromServer.status == null) hiveFromServer.status = currentHive.status;
                    if (hiveFromServer.id_location == 0) hiveFromServer.id_location = currentHive.id_location;
                    if (hiveFromServer.id_user == 0) hiveFromServer.id_user = currentHive.id_user;

                    currentHive = hiveFromServer;
                }

                view.setHive(currentHive);
                view.showOk("Successfully updated.");
            }

            @Override
            public void onError(String message) {
                view.setStatusSwitchChecked(!online);
                currentHive.status = !online ? "online" : "offline";
                view.setHive(currentHive);
                view.showError(message);
            }
        });
    }

    private void onEditClicked() {
        if (currentHive == null) {
            view.showError("No hive selected.");
            return;
        }

        HiveEditDialog.show(view.getStage(), view.getSkin(), currentHive, new HiveEditDialog.Callback() {
            @Override
            public void onSave(HiveEditDialog.Result result) {

                if (result.name == null || result.name.trim().isEmpty()) {
                    view.showError("Name cannot be empty.");
                    return;
                }
                if (result.type == null || !(result.type.equals("az") || result.type.equals("lr") || result.type.equals("db"))) {
                    view.showError("Type must be az, lr, or db.");
                    return;
                }

                currentHive.name = result.name.trim();
                currentHive.location = result.location != null ? result.location.trim() : "";
                currentHive.type = result.type;
                currentHive.status = result.status;

                view.setBusy("Saving changes...");
                HiveService.updateHive(currentHive, new HiveService.HiveSingleCallback() {
                    @Override
                    public void onSuccess(Hive hiveFromServer) {
                        if (hiveFromServer != null && hiveFromServer.id != 0) {
                            if (hiveFromServer.name == null) hiveFromServer.name = currentHive.name;
                            if (hiveFromServer.location == null) hiveFromServer.location = currentHive.location;
                            if (hiveFromServer.type == null) hiveFromServer.type = currentHive.type;
                            if (hiveFromServer.status == null) hiveFromServer.status = currentHive.status;
                            if (hiveFromServer.id_location == 0) hiveFromServer.id_location = currentHive.id_location;
                            if (hiveFromServer.id_user == 0) hiveFromServer.id_user = currentHive.id_user;

                            currentHive = hiveFromServer;
                        }

                        view.setHive(currentHive);
                        view.showOk("Successfully updated.");
                    }

                    @Override
                    public void onError(String message) {
                        view.showError(message);
                    }
                });
            }

            @Override
            public void onCancel() {
            }
        });
    }
}
