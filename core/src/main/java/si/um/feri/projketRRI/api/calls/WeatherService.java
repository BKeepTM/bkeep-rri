package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import si.um.feri.projketRRI.api.calls.model.Weather;
import si.um.feri.projketRRI.utils.Constants;

public class WeatherService {

    private static final String API_URL = Constants.API_URL;
    private static final String PREFS = "auth";

    public interface WeatherListCallback {
        void onSuccess(Array<Weather> list);
        void onError(String message);
    }

    public interface WeatherSingleCallback {
        void onSuccess(Weather weather);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public static void loadAll(WeatherListCallback callback) {
        String token = getTokenOrFail(callback);
        if (token == null) return;

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.GET)
            .url(API_URL + "/weather/getAll")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int status = response.getStatus().getStatusCode();
                String body = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (status >= 200 && status < 300) {
                        callback.onSuccess(parseWeatherList(body));
                    } else {
                        callback.onError("HTTP " + status + ": " + body);
                    }
                });
            }

            @Override public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    public static void loadById(int id, WeatherSingleCallback callback) {
        String token = getTokenOrFail(callback);
        if (token == null) return;

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.GET)
            .url(API_URL + "/weather/" + id)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int status = response.getStatus().getStatusCode();
                String body = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (status >= 200 && status < 300) {
                        callback.onSuccess(parseWeather(body));
                    } else {
                        callback.onError("HTTP " + status + ": " + body);
                    }
                });
            }

            @Override public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    public static void create(Weather w, WeatherSingleCallback callback) {
        String token = getTokenOrFail(callback);
        if (token == null) return;

        String body = weatherToJson(w, false);

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.POST)
            .url(API_URL + "/weather")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .content(body)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int status = response.getStatus().getStatusCode();
                String resp = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (status >= 200 && status < 300) {
                        callback.onSuccess(parseWeather(resp));
                    } else {
                        callback.onError("HTTP " + status + ": " + resp);
                    }
                });
            }

            @Override public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    public static void update(Weather w, SimpleCallback callback) {
        String token = getTokenOrFail(callback);
        if (token == null) return;

        String body = weatherToJson(w, true); // include "id"

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.POST)
            .url(API_URL + "/weather/update")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .content(body)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int status = response.getStatus().getStatusCode();
                String resp = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (status >= 200 && status < 300) {
                        callback.onSuccess(resp != null ? resp : "OK");
                    } else {
                        callback.onError("HTTP " + status + ": " + resp);
                    }
                });
            }

            @Override public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    public static void remove(int id, SimpleCallback callback) {
        String token = getTokenOrFail(callback);
        if (token == null) return;

        JsonValue bodyJson = new JsonValue(JsonValue.ValueType.object);
        bodyJson.addChild("id", new JsonValue(id));
        String body = bodyJson.toJson(JsonWriter.OutputType.json);

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.POST)
            .url(API_URL + "/weather/remove")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "text/plain")
            .header("Content-Type", "application/json")
            .content(body)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int status = response.getStatus().getStatusCode();
                String resp = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (status >= 200 && status < 300) {
                        callback.onSuccess(resp != null ? resp : "OK");
                    } else {
                        callback.onError("HTTP " + status + ": " + resp);
                    }
                });
            }

            @Override public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }


    private static String weatherToJson(Weather w, boolean includeId) {
        JsonValue bodyJson = new JsonValue(JsonValue.ValueType.object);

        if (includeId) bodyJson.addChild("id", new JsonValue(w.id));

        if (w.report_date != null) bodyJson.addChild("report_date", new JsonValue(w.report_date));
        if (w.location != null) bodyJson.addChild("location", new JsonValue(w.location));

        bodyJson.addChild("temperature", new JsonValue(w.temperature));
        bodyJson.addChild("air_pressure", new JsonValue(w.air_pressure));
        bodyJson.addChild("humidity", new JsonValue(w.humidity));
        bodyJson.addChild("wind_speed", new JsonValue(w.wind_speed));
        bodyJson.addChild("precipitation", new JsonValue(w.precipitation));

        bodyJson.addChild("id_location", new JsonValue(w.id_location));

        return bodyJson.toJson(JsonWriter.OutputType.json);
    }

    private static Array<Weather> parseWeatherList(String json) {
        Array<Weather> out = new Array<>();
        JsonValue root = new JsonReader().parse(json);

        // if backend returns { data: [...] }, uncomment:
        // root = root.get("data");

        for (JsonValue w : root) {
            out.add(parseWeatherValue(w));
        }
        return out;
    }

    private static Weather parseWeather(String json) {
        JsonValue v = new JsonReader().parse(json);
        return parseWeatherValue(v);
    }

    private static Weather parseWeatherValue(JsonValue w) {
        Weather out = new Weather();
        out.id = w.getInt("id", 0);
        out.report_date = w.getString("report_date", null);
        out.location = w.getString("location", null);

        out.temperature = (float) w.getDouble("temperature", 0);
        out.air_pressure = (float) w.getDouble("air_pressure", 0);
        out.humidity = (float) w.getDouble("humidity", 0);
        out.wind_speed = (float) w.getDouble("wind_speed", 0);
        out.precipitation = (float) w.getDouble("precipitation", 0);

        out.id_location = w.getInt("id_location", 0);
        return out;
    }

    private static String getTokenOrFail(WeatherListCallback cb) {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");
        if (token.isEmpty()) cb.onError("No auth token");
        return token.isEmpty() ? null : token;
    }

    private static String getTokenOrFail(WeatherSingleCallback cb) {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");
        if (token.isEmpty()) cb.onError("No auth token");
        return token.isEmpty() ? null : token;
    }

    private static String getTokenOrFail(SimpleCallback cb) {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");
        if (token.isEmpty()) cb.onError("No auth token");
        return token.isEmpty() ? null : token;
    }
}
