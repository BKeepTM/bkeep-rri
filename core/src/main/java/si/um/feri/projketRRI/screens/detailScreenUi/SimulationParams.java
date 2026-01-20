package si.um.feri.projketRRI.screens.detailScreenUi;

public class SimulationParams {
    public int days;
    public float maxDailyYield = 0.32F;//kg/day

    public float dailyConsumption  = 0.1F;
    public float baseTemp;           // from weather
    public float baseHumidity;       // from weather
    public float tempSwing;          // +/- °C
    public float humiditySwing;      // +/- %
    public float lightFactor;        // 0..1
    public float populationFactor;   // 0..1
    public float weatherFactor;      // 0..1

    @Override
    public String toString() {
        return "SimulationParams{" +
            "days=" + days +
            ", maxDailyYield=" + maxDailyYield +
            ", baseTemp=" + baseTemp +
            ", baseHumidity=" + baseHumidity +
            ", tempSwing=" + tempSwing +
            ", humiditySwing=" + humiditySwing +
            ", lightFactor=" + lightFactor +
            ", populationFactor=" + populationFactor +
            ", weatherFactor=" + weatherFactor +
            '}';
    }
}
