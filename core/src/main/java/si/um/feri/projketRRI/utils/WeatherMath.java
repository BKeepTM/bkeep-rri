package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

import si.um.feri.projketRRI.api.calls.model.Location;
import si.um.feri.projketRRI.api.calls.model.Weather;

public class WeatherMath {

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static Weather findClosestWeather(Location hiveLoc, Array<Weather> weatherList, IntMap<Location> weatherLocById) {
        Weather best = null;
        double bestKm = Double.MAX_VALUE;

        for (Weather w : weatherList) {
            Location wl = weatherLocById.get(w.id_location);
            if (wl == null) continue;

            double km = haversineKm(hiveLoc.latitude, hiveLoc.longitude, wl.latitude, wl.longitude);
            if (km < bestKm) {
                bestKm = km;
                best = w;
            }
        }
        return best;
    }

    public static double distanceKm(Location a, Location b) {
        return haversineKm(a.latitude, a.longitude, b.latitude, b.longitude);
    }

    public static void logNearestStations(Location hiveLoc,
                                          Array<Weather> weathers,
                                          com.badlogic.gdx.utils.IntMap<Location> stationLocs,
                                          int topN) {

        class Cand {
            Weather w; Location loc; double km;
        }

        com.badlogic.gdx.utils.Array<Cand> list = new com.badlogic.gdx.utils.Array<>();

        for (Weather w : weathers) {
            Location sl = stationLocs.get(w.id_location);
            if (sl == null) continue;

            Cand c = new Cand();
            c.w = w;
            c.loc = sl;
            c.km = haversineKm(hiveLoc.latitude, hiveLoc.longitude, sl.latitude, sl.longitude);
            list.add(c);
        }

        list.sort((a, b) -> Double.compare(a.km, b.km));

        int n = Math.min(topN, list.size);
        for (int i = 0; i < n; i++) {
            Cand c = list.get(i);
            Gdx.app.log("WEATHER_CAND",
                "#" + (i+1) + " km=" + c.km +
                    " weatherId=" + c.w.id +
                    " stationLocId=" + c.loc.id +
                    " stationLat=" + c.loc.latitude +
                    " stationLon=" + c.loc.longitude +
                    " weatherName=" + c.w.location
            );
        }
    }

}
