package si.um.feri.projketRRI.api.calls.model;

import com.google.gson.annotations.SerializedName;

public class HiveWeight {
    public int id;
    public float weight;

    @SerializedName("time_weight")
    public String timeWeight;

    @SerializedName("id_hive")
    public int idHive;

    public HiveWeight() {} // Gson needs no-arg constructor
}
