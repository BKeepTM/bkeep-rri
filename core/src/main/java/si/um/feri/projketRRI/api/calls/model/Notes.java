package si.um.feri.projketRRI.api.calls.model;

import com.google.gson.annotations.SerializedName;

public class Notes {
    public int id;
    public String content;
    public String time;

    @SerializedName("id_hive")
    public int idHive;

    public Notes() {} // needed for Gson

    public Notes(int id, String content, String time, int idHive) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.idHive = idHive;
    }
}
