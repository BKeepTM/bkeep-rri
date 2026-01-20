package si.um.feri.projketRRI.api.calls.model;

public class Notification {
    public int id;
    public String summary;
    public String description;
    public String href;
    public int severity;
    public int idUser;

    public Notification(int id, String summary, String description, String href, int severity, int idUser) {
        this.id = id;
        this.summary = summary;
        this.description = description;
        this.href = href;
        this.severity = severity;
        this.idUser = idUser;
    }
}
