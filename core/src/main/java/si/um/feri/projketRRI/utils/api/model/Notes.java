package si.um.feri.projketRRI.utils.api.model;

public class Notes {
    public int id;
    public String content;
    public String time;
    public int idHive;

    public Notes(int id, String content, String time, int idHive) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.idHive = idHive;
    }
}
