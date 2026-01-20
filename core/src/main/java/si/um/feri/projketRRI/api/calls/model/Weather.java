package si.um.feri.projketRRI.api.calls.model;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

public class Weather {

    public int id;
    public String report_date;
    public String location;

    public float temperature;
    public float air_pressure;
    public float humidity;
    public float wind_speed;
    public float precipitation;

    public int id_location;

    public Weather() {}

    @Override
    public String toString() {
        return "Weather{" +
            "id=" + id +
            ", report_date='" + report_date + '\'' +
            ", location='" + location + '\'' +
            ", temperature=" + temperature +
            ", air_pressure=" + air_pressure +
            ", humidity=" + humidity +
            ", wind_speed=" + wind_speed +
            ", precipitation=" + precipitation +
            ", id_location=" + id_location +
            '}';
    }
}
