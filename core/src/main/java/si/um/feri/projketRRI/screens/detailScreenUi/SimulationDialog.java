package si.um.feri.projketRRI.screens.detailScreenUi;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import si.um.feri.projketRRI.api.calls.model.HiveWeight;
import si.um.feri.projketRRI.utils.HiveWeightUtils;

public class SimulationDialog extends Dialog {

    public interface Listener {
        void onStart(SimulationCalculator.Params params);
        void onCancel();
    }

    private Listener listener;

    // Inputs
    private final TextField daysField;

    private final TextField baseTempField;
    private final TextField baseHumidityField;

    private final TextField tempSwingField;
    private final TextField humiditySwingField;

    private final TextField lightMinLuxField;
    private final TextField lightMaxLuxField;

    private final SelectBox<SimulationCalculator.PopulationLevel> populationSelect;

    private final TextField startWeightField;

    private final CheckBox randomizeDailyCheck;
    private final CheckBox randomizeWeatherDailyCheck;

    // Weather multi-select
    private final EnumMap<SimulationCalculator.WeatherType, CheckBox> weatherChecks =
        new EnumMap<>(SimulationCalculator.WeatherType.class);

    // Info / errors
    private final Label infoLabel;
    private final Label errorLabel;

    public SimulationDialog(Skin skin, Array<HiveWeight> hiveWeights) {
        super("Simulation", skin);


        HiveWeight latest = HiveWeightUtils.latest(hiveWeights);
        if (latest != null) {
            float w = latest.weight;
        }
        daysField = new TextField("30", skin);

        baseTempField = new TextField("-", skin);
        baseHumidityField = new TextField("-", skin);

        //baseTempField.setDisabled(true);
        //baseHumidityField.setDisabled(true);

        tempSwingField = new TextField("0", skin);
        humiditySwingField = new TextField("0", skin);

        lightMinLuxField = new TextField("20000", skin);
        lightMaxLuxField = new TextField("20000", skin);

        populationSelect = new SelectBox<>(skin);
        populationSelect.setItems(SimulationCalculator.PopulationLevel.values());
        populationSelect.setSelected(SimulationCalculator.PopulationLevel.MEDIUM);

        assert latest != null;
        startWeightField = new TextField(String.valueOf(latest.weight), skin);

        randomizeDailyCheck = new CheckBox(" Randomize daily temp/hum/light", skin);
        randomizeDailyCheck.setChecked(true);

        randomizeWeatherDailyCheck = new CheckBox(" Randomize weather per day (from selected)", skin);
        randomizeWeatherDailyCheck.setChecked(false);

        infoLabel = new Label(
            "Calculator will compute factors from your inputs:\n" +
                "• Temp and humidity use threshold tables\n" +
                "• Light uses lux thresholds\n" +
                "• Weather factor is average (or daily random if enabled)\n" +
                "• Stress is derived from average of temp/hum/light/weather (1.0–2.0)",
            skin
        );
        infoLabel.setWrap(true);
        infoLabel.setColor(Color.LIGHT_GRAY);

        errorLabel = new Label("", skin);
        errorLabel.setColor(Color.SALMON);
        errorLabel.setWrap(true);

        buildLayout(skin);
        wireTooltips(skin);

        // Buttons
        button("Cancel", false);
        button("Start", true);

        getTitleLabel().setAlignment(Align.center);
        getContentTable().pad(10);
        padTop(40);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setBaseWeather(Float temp, Float humidity) {
        baseTempField.setText(temp == null ? "-" : String.valueOf(temp));
        baseHumidityField.setText(humidity == null ? "-" : String.valueOf(humidity));
    }

    public void setBaseWeatherEditable(boolean editable) {
        baseTempField.setDisabled(!editable);
        baseHumidityField.setDisabled(!editable);
    }

    private void buildLayout(Skin skin) {
        Table c = getContentTable();
        c.defaults().left();

        Table grid = new Table(skin);
        grid.defaults().padBottom(6).left();

        addRow(grid, "Days:", daysField, 160);

        grid.add(new Label("Base weather (auto):", skin)).colspan(2).padTop(6).row();
        addRow(grid, "Temp (°C):", baseTempField, 160);
        addRow(grid, "Humidity (%):", baseHumidityField, 160);

        addRow(grid, "Temp swing ± (°C):", tempSwingField, 160);
        addRow(grid, "Humidity swing ± (%):", humiditySwingField, 160);

        grid.add(new Label("Light (lux range):", skin)).colspan(2).padTop(6).row();
        addRow(grid, "Light min (lux):", lightMinLuxField, 160);
        addRow(grid, "Light max (lux):", lightMaxLuxField, 160);

        grid.add(new Label("Hive / population:", skin)).colspan(2).padTop(6).row();
        addRow(grid, "Population:", populationSelect, 160);

        grid.add(new Label("Start:", skin)).colspan(2).padTop(6).row();
        addRow(grid, "Start weight (kg):", startWeightField, 160);

        grid.add(new Label("Weather (multi-select):", skin)).colspan(2).padTop(6).row();
        grid.add(buildWeatherGrid(skin)).colspan(2).left().row();

        grid.row().padTop(6);
        grid.add(randomizeDailyCheck).colspan(2).left().row();
        grid.add(randomizeWeatherDailyCheck).colspan(2).left().row();

        c.add(grid).width(560).row();
        c.add(infoLabel).width(560).padTop(8).row();
        c.add(errorLabel).width(560).padTop(6).row();
    }

    private Table buildWeatherGrid(Skin skin) {
        Table w = new Table(skin);
        w.defaults().left().padRight(12).padBottom(4);

        int col = 0;
        for (SimulationCalculator.WeatherType t : SimulationCalculator.WeatherType.values()) {
            CheckBox cb = new CheckBox(" " + t.name(), skin);

            // default like old logic: SUNNY selected
            cb.setChecked(t == SimulationCalculator.WeatherType.SUNNY);

            weatherChecks.put(t, cb);

            w.add(cb);
            col++;
            if (col % 2 == 0) w.row();
        }
        return w;
    }

    private void wireTooltips(Skin skin) {
        addTooltip(daysField, "How many days to simulate.");

        addTooltip(baseTempField,
            "Base temperature (°C) from real weather.\n" +
                "Enable editing if you want user override.");
        addTooltip(baseHumidityField,
            "Base humidity (%) from real weather.\n" +
                "Enable editing if you want user override.");

        addTooltip(tempSwingField,
            "Daily temperature swing range. If randomization is ON,\n" +
                "each day temp = base ± swing.");
        addTooltip(humiditySwingField,
            "Daily humidity swing range. If randomization is ON,\n" +
                "each day humidity = base ± swing.");

        addTooltip(lightMinLuxField,
            "Minimum lux. If randomization is ON,\n" +
                "each day lux is picked between min and max.\n" +
                "Light factor thresholds:\n" +
                "<1000=0.1, <10000=0.4, <50000=0.8, >=50000=1.0");
        addTooltip(lightMaxLuxField,
            "Maximum lux. Can be equal to min for constant light.");

        addTooltip(populationSelect,
            "Hive size affects yield and consumption:\nWEAK=0.5, MEDIUM=0.7, STRONG=1.0");

        addTooltip(startWeightField, "Initial weight (kg) at day 0.");

        addTooltip(randomizeDailyCheck,
            "If ON, temp/humidity/light vary each day (within swings and lux range).");

        addTooltip(randomizeWeatherDailyCheck,
            "If OFF: uses AVERAGE factor across selected weather.\n" +
                "If ON: picks ONE selected weather type per day.");

        for (Map.Entry<SimulationCalculator.WeatherType, CheckBox> e : weatherChecks.entrySet()) {
            SimulationCalculator.WeatherType t = e.getKey();
            addTooltip(e.getValue(), "Weather factor: " + t.name() + " => " + SimulationCalculator.factorWeather(t));
        }
    }

    private void addTooltip(Actor a, String text) {
        TextTooltip tooltip = new TextTooltip(text, getSkin());
        tooltip.getContainer().pad(8);
        a.addListener(tooltip);
    }

    @Override
    protected void result(Object object) {
        boolean start = Boolean.TRUE.equals(object);

        if (!start) {
            if (listener != null) listener.onCancel();
            hide();
            return;
        }

        SimulationCalculator.Params params = readParamsOrNull();
        if (params == null) return;

        if (listener != null) listener.onStart(params);
        hide();
    }

    private SimulationCalculator.Params readParamsOrNull() {
        errorLabel.setText("");

        Integer days = parseInt(daysField.getText());
        Float baseTemp = parseFloatNullable(baseTempField.getText());
        Float baseHum = parseFloatNullable(baseHumidityField.getText());

        Float tSwing = parseFloat(tempSwingField.getText());
        Float hSwing = parseFloat(humiditySwingField.getText());

        Float luxMin = parseFloat(lightMinLuxField.getText());
        Float luxMax = parseFloat(lightMaxLuxField.getText());

        Float startW = parseFloat(startWeightField.getText());

        if (days == null || days <= 0) return fail("Days must be a positive integer.");
        if (tSwing == null || tSwing < 0f) return fail("Temp swing must be >= 0.");
        if (hSwing == null || hSwing < 0f) return fail("Humidity swing must be >= 0.");
        if (baseTemp == null) return fail("Base temperature missing (no weather data?).");
        if (baseHum == null) return fail("Base humidity missing (no weather data?).");

        if (luxMin == null || luxMin < 0f) return fail("Light min must be >= 0.");
        if (luxMax == null || luxMax < 0f) return fail("Light max must be >= 0.");

        if (startW == null || startW < 0f) return fail("Start weight must be >= 0.");

        EnumSet<SimulationCalculator.WeatherType> set = EnumSet.noneOf(SimulationCalculator.WeatherType.class);
        for (Map.Entry<SimulationCalculator.WeatherType, CheckBox> e : weatherChecks.entrySet()) {
            if (e.getValue().isChecked()) set.add(e.getKey());
        }
        if (set.isEmpty()) return fail("Select at least one weather type.");

        SimulationCalculator.Params p = new SimulationCalculator.Params();
        p.days = days;

        p.baseTempC = baseTemp;
        p.baseHumidityPct = baseHum;

        p.tempSwingC = tSwing;
        p.humiditySwingPct = hSwing;

        p.lightLuxMin = luxMin;
        p.lightLuxMax = luxMax;

        p.population = populationSelect.getSelected();

        p.weathers = set;

        p.startWeightKg = startW;

        p.randomizeDaily = randomizeDailyCheck.isChecked();
        p.randomizeWeatherDaily = randomizeWeatherDailyCheck.isChecked();

        return p;
    }

    private SimulationCalculator.Params fail(String msg) {
        errorLabel.setText(msg);
        return null;
    }

    private void addRow(Table t, String key, Actor field, float fieldW) {
        Label k = new Label(key, getSkin());
        k.setColor(Color.LIGHT_GRAY);
        t.add(k).left().padRight(10);
        t.add(field).width(fieldW).left().row();
    }

    private static Integer parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private static Float parseFloat(String s) {
        try { return Float.parseFloat(s.trim().replace(",", ".")); } catch (Exception e) { return null; }
    }

    private static Float parseFloatNullable(String s) {
        if (s == null) return null;
        String x = s.trim();
        if (x.isEmpty() || x.equals("-")) return null;
        return parseFloat(x);
    }
}
