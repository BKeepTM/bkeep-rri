package si.um.feri.projketRRI.screens.detailScreenUi;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import si.um.feri.projketRRI.api.calls.model.Weather;

public class HiveWeatherView implements Disposable {

    private final Stage stage;
    private final Skin skin;

    private final Label stationValue;
    private final Label dateValue;
    private final Label tempValue;
    private final Label humidityValue;
    private final Label windValue;
    private final Label precipValue;
    private final Label pressureValue;
    private final Label feedbackLabel;

    private final Table panel;
    private boolean visible = true;
    private TextButton toggleButton;

    private Table buttonRow;

    public HiveWeatherView() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        stage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        root.top().left().pad(12);
        root.padTop(240);

        toggleButton = new TextButton("Hide weather", skin);
        toggleButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setPanelVisible(!visible);
            }
        });

        TextButton simulationButton = new TextButton("Simulation", skin);

        simulationButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (simulationClickListener != null) simulationClickListener.onSimulationClick();
            }
        });

        Table buttonsRow = new Table();
        buttonsRow.add(toggleButton).left();
        buttonsRow.add(simulationButton).left().padLeft(8);

        root.add(buttonsRow).left().padBottom(6).row();

        panel = new Table(skin);
        panel.background("default-round");
        panel.pad(12);

        Label.LabelStyle keyStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.LIGHT_GRAY);
        Label.LabelStyle valStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE);

        stationValue = new Label("-", valStyle);
        dateValue = new Label("-", valStyle);
        tempValue = new Label("-", valStyle);
        humidityValue = new Label("-", valStyle);
        windValue = new Label("-", valStyle);
        precipValue = new Label("-", valStyle);
        pressureValue = new Label("-", valStyle);

        feedbackLabel = new Label("", valStyle);
        feedbackLabel.setColor(Color.LIGHT_GRAY);
        feedbackLabel.setWrap(true);

        buttonRow = new Table();

        Label title = new Label("Closest weather station", skin);
        buttonRow.add(title).left().expandX();

        panel.add(buttonRow).growX().left().row();
        panel.row().padTop(8);

        addRow(panel, keyStyle, "Station:", stationValue);
        addRow(panel, keyStyle, "Report:", dateValue);
        addRow(panel, keyStyle, "Temp:", tempValue);
        addRow(panel, keyStyle, "Humidity:", humidityValue);
        addRow(panel, keyStyle, "Wind:", windValue);
        addRow(panel, keyStyle, "Precip:", precipValue);
        addRow(panel, keyStyle, "Pressure:", pressureValue);

        panel.add(feedbackLabel).colspan(2).width(360).padTop(10).left().row();

        root.add(panel).left().top();

        stage.addActor(root);

        setPanelVisible(false);
    }

    private void addRow(Table t, Label.LabelStyle keyStyle, String key, Label valueLabel) {
        Label k = new Label(key, keyStyle);
        valueLabel.setWrap(true);

        t.add(k).left().padRight(10);
        t.add(valueLabel).left().width(240).row();
        t.row().padTop(6);
    }

    public Stage getStage() { return stage; }

    public void setLoading(String msg) {
        feedbackLabel.setColor(Color.LIGHT_GRAY);
        feedbackLabel.setText(msg != null ? msg : "Loading...");
    }

    public void setError(String msg) {
        feedbackLabel.setColor(Color.SALMON);
        feedbackLabel.setText(msg != null ? msg : "Failed.");
    }

    public void clearStatus() {
        feedbackLabel.setText("");
    }

    public void setWeather(Weather w) {
        if (w == null) {
            stationValue.setText("-");
            dateValue.setText("-");
            tempValue.setText("-");
            humidityValue.setText("-");
            windValue.setText("-");
            precipValue.setText("-");
            pressureValue.setText("-");
            setError("No weather available.");
            return;
        }

        stationValue.setText(w.location != null ? w.location : ("id_location=" + w.id_location));
        dateValue.setText(si.um.feri.projketRRI.utils.WeatherFormat.formatReportDate(w.report_date));

        tempValue.setText(String.valueOf(w.temperature));
        humidityValue.setText(String.valueOf(w.humidity));
        windValue.setText(String.valueOf(w.wind_speed));
        precipValue.setText(String.valueOf(w.precipitation));
        pressureValue.setText(String.valueOf(w.air_pressure));

        clearStatus();
    }

    public void setVisible(boolean show) {
        visible = show;
        panel.setVisible(show);
    }

    private void setPanelVisible(boolean show) {
        visible = show;
        panel.setVisible(show);
        panel.setTouchable(show ? Touchable.enabled : Touchable.disabled);

        toggleButton.setText(show ? "Hide weather" : "Show weather");
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    public void addTopButton(Actor a) { buttonRow.add(a).padLeft(8); }

    public interface SimulationClickListener {
        void onSimulationClick();
    }

    private SimulationClickListener simulationClickListener;

    public void setOnSimulationClick(SimulationClickListener l) {
        this.simulationClickListener = l;
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
