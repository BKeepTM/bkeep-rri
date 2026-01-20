package si.um.feri.projketRRI.screens.detailScreenUi;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class SimulationCalculator {

    public static final float MAX_DAILY_YIELD = 0.32f;      // kg/day
    public static final float DAILY_CONSUMPTION = 0.10f;    // kg/day

    public enum PopulationLevel { WEAK, MEDIUM, STRONG }

    public enum WeatherType {
        SUNNY,
        CLOUDY,
        LIGHT_RAIN,
        MEDIUM_RAIN,
        HEAVY_RAIN
    }

    public static class DayResult {
        public int day;
        public float fTemp;
        public float fHumidity;
        public float fLight;
        public float fPopulation;
        public float fWeather;

        public float fStress;
        public float yieldKg;
        public float lossKg;
        public float deltaKg;
        public float weightKg;

        @Override public String toString() {
            return "Day " + day +
                " | F(temp)=" + fTemp +
                " F(hum)=" + fHumidity +
                " F(light)=" + fLight +
                " F(pop)=" + fPopulation +
                " F(weather)=" + fWeather +
                " | F(stress)=" + fStress +
                " | yield=" + yieldKg +
                " loss=" + lossKg +
                " delta=" + deltaKg +
                " => weight=" + weightKg;
        }
    }

    public static class Params {
        public int days = 30;

        public float baseTempC = 20f;
        public float baseHumidityPct = 60f;

        public float tempSwingC = 0f;
        public float humiditySwingPct = 0f;

        // NEW: light range
        public float lightLuxMin = 20000f;
        public float lightLuxMax = 20000f;

        public PopulationLevel population = PopulationLevel.MEDIUM;

        // Multi-select weather set
        public EnumSet<WeatherType> weathers = EnumSet.of(WeatherType.SUNNY);

        public float startWeightKg = 0f;

        // Existing
        public boolean randomizeDaily = true;

        // NEW: if multiple weather selected, pick one per day (instead of average)
        public boolean randomizeWeatherDaily = false;
    }

    private SimulationCalculator() {}

    public static List<DayResult> run(Params p) {
        int days = Math.max(1, p.days);

        float weight = p.startWeightKg;
        List<DayResult> out = new ArrayList<>(days);

        float luxMin = Math.min(p.lightLuxMin, p.lightLuxMax);
        float luxMax = Math.max(p.lightLuxMin, p.lightLuxMax);

        for (int d = 1; d <= days; d++) {
            float temp = p.baseTempC;
            float hum = p.baseHumidityPct;

            if (p.randomizeDaily) {
                temp += MathUtils.random(-p.tempSwingC, p.tempSwingC);
                hum  += MathUtils.random(-p.humiditySwingPct, p.humiditySwingPct);
            }

            float lux = luxMax <= 0 ? 0 : luxMin;
            if (p.randomizeDaily) {
                lux = MathUtils.random(luxMin, luxMax);
            }

            float fTemp = factorTemp(temp);
            float fHum  = factorHumidity(hum);
            float fLight = factorLight(lux);
            float fPop  = factorPopulation(p.population);

            float fWea;
            if (p.randomizeWeatherDaily) {
                fWea = factorWeather(randomWeatherFromSet(p.weathers));
            } else {
                fWea = factorWeatherAverage(p.weathers);
            }

            float avg = (fTemp + fHum + fLight + fWea) / 4f;
            float fStress = 1f + (1f - clamp01(avg));
            fStress = MathUtils.clamp(fStress, 1f, 2f);

            float yieldKg = MAX_DAILY_YIELD * fTemp * fHum * fLight * fPop * fWea;
            float lossKg  = DAILY_CONSUMPTION * fPop * fStress;
            float deltaKg = yieldKg - lossKg;

            weight += deltaKg;

            DayResult r = new DayResult();
            r.day = d;
            r.fTemp = fTemp;
            r.fHumidity = fHum;
            r.fLight = fLight;
            r.fPopulation = fPop;
            r.fWeather = fWea;
            r.fStress = fStress;
            r.yieldKg = yieldKg;
            r.lossKg = lossKg;
            r.deltaKg = deltaKg;
            r.weightKg = weight;

            out.add(r);

            System.out.println("[SIM] " + r);
        }

        return out;
    }

    private static WeatherType randomWeatherFromSet(EnumSet<WeatherType> set) {
        if (set == null || set.isEmpty()) return WeatherType.CLOUDY;
        int idx = MathUtils.random(0, set.size() - 1);
        int i = 0;
        for (WeatherType t : set) {
            if (i == idx) return t;
            i++;
        }
        return WeatherType.CLOUDY;
    }

    public static float factorTemp(float tempC) {
        if (tempC < 9.2f) return 0f;
        if (tempC <= 11.0f) return 0.2f;
        if (tempC <= 16.2f) return 0.5f;
        if (tempC <= 28.5f) return 1.0f;
        if (tempC <= 35.0f) return 0.5f;
        if (tempC <= 39.9f) return 0.2f;
        return 0f;
    }

    public static float factorHumidity(float humidityPct) {
        if (humidityPct < 45.0f) return 0.0f;
        if (humidityPct <= 50.4f) return 0.3f;
        if (humidityPct <= 71.6f) return 1.0f;
        if (humidityPct <= 79.9f) return 0.3f;
        return 0.0f;
    }

    public static float factorLight(float lux) {
        if (lux < 1000f) return 0.1f;
        if (lux < 10000f) return 0.4f;
        if (lux < 50000f) return 0.8f;
        return 1.0f;
    }

    public static float factorPopulation(PopulationLevel pop) {
        if (pop == null) return 0.7f;
        switch (pop) {
            case WEAK:   return 0.5f;
            case MEDIUM: return 0.7f;
            case STRONG: return 1.0f;
            default:     return 0.7f;
        }
    }

    public static float factorWeather(WeatherType t) {
        if (t == null) return 0.7f;
        switch (t) {
            case SUNNY:       return 1.0f;
            case CLOUDY:      return 0.7f;
            case LIGHT_RAIN:  return 1.0f;
            case MEDIUM_RAIN: return 0.4f;
            case HEAVY_RAIN:  return 0.2f;
            default:          return 0.7f;
        }
    }

    public static float factorWeatherAverage(EnumSet<WeatherType> set) {
        if (set == null || set.isEmpty()) return 0.7f;
        float sum = 0f;
        int n = 0;
        for (WeatherType t : set) {
            sum += factorWeather(t);
            n++;
        }
        return sum / (float) n;
    }

    private static float clamp01(float x) {
        return MathUtils.clamp(x, 0f, 1f);
    }
}
