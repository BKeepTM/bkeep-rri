package si.um.feri.projketRRI.utils.api.model;

public class User {
    public int id;
    public String username;
    public String password;
    public String mail;
    public String settings; // JSON string

    public User(int id, String username, String password, String mail, String settings) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.mail = mail;
        this.settings = settings;
    }
}
