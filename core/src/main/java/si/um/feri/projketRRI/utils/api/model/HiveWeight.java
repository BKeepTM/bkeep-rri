package si.um.feri.projketRRI.utils.api.model;

public class HiveWeight {
    public int id;
    public float weight;
    public String timeWeight; // SQL Datetime
    public int idHive;

    public HiveWeight(int id, float weight, String timeWeight, int idHive) {
        this.id = id;
        this.weight = weight;
        this.timeWeight = timeWeight;
        this.idHive = idHive;
    }
}
