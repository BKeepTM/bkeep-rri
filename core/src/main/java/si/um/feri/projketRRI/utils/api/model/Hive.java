package si.um.feri.projketRRI.utils.api.model;

public class Hive {
    public int id;
    public String name;
    public String location; // The text description (e.g. "Backyard")
    public String type;     // "lr", "az", or "db"
    public String status;
    public float longitude;
    public float latitude;// "offline" or "online"
    public transient int idLocation;  // FK to Location table
    public transient int idUser;      // FK to User table

    public Hive(int id, String name, String location, String type, String status,float longitude, float latitude, int idLocation, int idUser) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.type = type;
        this.status = status;
        this.longitude = longitude;
        this.latitude = latitude;
        this.idLocation = idLocation;
        this.idUser = idUser;
    }
}
