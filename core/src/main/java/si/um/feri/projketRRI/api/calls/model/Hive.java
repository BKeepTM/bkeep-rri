package si.um.feri.projketRRI.api.calls.model;

public class Hive {

    public int id;
    public String name;
    public String location;
    public String type;
    public String status;
    public int id_location;
    public int id_user;

    public Hive() {}

    @Override
    public String toString() {
        return "Hive{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", location='" + location + '\'' +
            ", type='" + type + '\'' +
            ", status='" + status + '\'' +
            '}';
    }
}
