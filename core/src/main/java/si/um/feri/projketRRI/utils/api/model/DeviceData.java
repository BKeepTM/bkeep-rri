package si.um.feri.projketRRI.utils.api.model;

public class DeviceData {
    public int id;
    public float humidity;
    public float brightness;
    public float temperature;
    public float longitude;
    public float latitude;
    public String time;     // SQL datetime
    public int userId;      // SQL user_id

    public DeviceData(int id, float humidity, float brightness, float temperature, float longitude, float latitude, String time, int userId) {
        this.id = id;
        this.humidity = humidity;
        this.brightness = brightness;
        this.temperature = temperature;
        this.longitude = longitude;
        this.latitude = latitude;
        this.time = time;
        this.userId = userId;
    }
}
