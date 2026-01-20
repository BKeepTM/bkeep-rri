package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import si.um.feri.projketRRI.api.calls.model.Hive;
import si.um.feri.projketRRI.utils.Constants;

public class HiveService {

    private static final String API_URL = Constants.API_URL;
    private static final String PREFS = "auth";

    public interface HiveCallback {
        void onSuccess(Array<Hive> hives);
        void onError(String message);
    }

    public interface HiveSingleCallback {
        void onSuccess(Hive hive);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public static void loadHives(HiveCallback callback) {

        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            callback.onError("No auth token");
            return;
        }

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.GET)
            .url(API_URL + "/hive/list")
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
                        callback.onSuccess(parseHives(body));
                    } else {
                        callback.onError("HTTP " + status + ": " + body);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    /**
     * CREATE: POST /hive
     * backend: HiveController.create
     * body: { name, type, status, longitude, latitude, location }
     */
    public static void createHive(
        String name,
        String type,
        String status,
        float longitude,
        float latitude,
        String location,
        HiveSingleCallback callback
    ) {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            callback.onError("No auth token");
            return;
        }

        // Sestavi JSON body
        Json json = new Json();
        JsonValue bodyJson = new JsonValue(JsonValue.ValueType.object);
        bodyJson.addChild("name", new JsonValue(name));
        bodyJson.addChild("type", new JsonValue(type));
        bodyJson.addChild("status", new JsonValue(status));
        bodyJson.addChild("longitude", new JsonValue(longitude));
        bodyJson.addChild("latitude", new JsonValue(latitude));
        bodyJson.addChild("location", new JsonValue(location));

        String body = json.toJson(bodyJson);

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.POST)
            .url(API_URL + "/hive")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .content(body)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int statusCode = response.getStatus().getStatusCode();
                String respBody = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (statusCode >= 200 && statusCode < 300) {
                        callback.onSuccess(parseHive(respBody));
                    } else {
                        callback.onError("HTTP " + statusCode + ": " + respBody);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    /**
     * UPDATE: PUT /hive/:id
     * backend: HiveController.update (bere req.params.id ali req.body.id)
     * body lahko vsebuje: { id, name, location, type, status, id_location }
     *
     * Če želiš endpoint /hive/update, poglej zakomentirano alternativo spodaj.
     */
    public static void updateHive(Hive hive, HiveSingleCallback callback) {

        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            callback.onError("No auth token");
            return;
        }

        // ZGRADI JSON BODY KOT JsonValue
        JsonValue bodyJson = new JsonValue(JsonValue.ValueType.object);

        bodyJson.addChild("id", new JsonValue(hive.id));

        if (hive.name != null) bodyJson.addChild("name", new JsonValue(hive.name));
        if (hive.location != null) bodyJson.addChild("location", new JsonValue(hive.location));
        if (hive.type != null) bodyJson.addChild("type", new JsonValue(hive.type));
        if (hive.status != null) bodyJson.addChild("status", new JsonValue(hive.status));

        bodyJson.addChild("id_location", new JsonValue(hive.id_location));

        String body = bodyJson.toJson(JsonWriter.OutputType.json);

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.PUT)
            .url(API_URL + "/hive/" + hive.id)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .content(body)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int statusCode = response.getStatus().getStatusCode();
                String respBody = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (statusCode >= 200 && statusCode < 300) {
                        callback.onSuccess(parseHive(respBody));
                    } else {
                        callback.onError("HTTP " + statusCode + ": " + respBody);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    /**
     * REMOVE: DELETE /hive/remove?id=123
     * backend: HiveController.remove (bere params/body/query)
     */
    public static void removeHive(int hiveId, SimpleCallback callback) {

        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            callback.onError("No auth token");
            return;
        }

        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.DELETE)
            .url(API_URL + "/hive/remove?id=" + hiveId)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "text/plain")
            .build();

        // Alternativa, če imaš DELETE /hive/:id:
        // .url(API_URL + "/hive/" + hiveId)

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                int statusCode = response.getStatus().getStatusCode();
                String respBody = response.getResultAsString();

                Gdx.app.postRunnable(() -> {
                    if (statusCode >= 200 && statusCode < 300) {
                        callback.onSuccess(respBody); // "Uspesno zbrisan panj"
                    } else {
                        callback.onError("HTTP " + statusCode + ": " + respBody);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    private static Array<Hive> parseHives(String json) {
        Array<Hive> hives = new Array<>();
        JsonValue root = new JsonReader().parse(json);

        for (JsonValue h : root) {
            Hive hive = new Hive();
            hive.id = h.getInt("id");
            hive.name = h.getString("name", null);
            hive.location = h.getString("location", null);
            hive.type = h.getString("type", null);
            hive.status = h.getString("status", null);
            hive.id_location = h.getInt("id_location", 0);
            hive.id_user = h.getInt("id_user", 0);
            hives.add(hive);
        }
        return hives;
    }

    private static Hive parseHive(String json) {
        JsonValue h = new JsonReader().parse(json);
        Hive hive = new Hive();
        hive.id = h.getInt("id", 0);
        hive.name = h.getString("name", null);
        hive.location = h.getString("location", null);
        hive.type = h.getString("type", null);
        hive.status = h.getString("status", null);
        hive.id_location = h.getInt("id_location", 0);
        hive.id_user = h.getInt("id_user", 0);
        return hive;
    }
}
