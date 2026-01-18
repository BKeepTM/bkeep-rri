package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import si.um.feri.projketRRI.api.calls.model.Hive;

public class HiveService {

    private static final String API_URL = "http://localhost:3000";
    private static final String PREFS = "auth";

    public interface HiveCallback {
        void onSuccess(Array<Hive> hives);
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
                Gdx.app.postRunnable(() ->
                        callback.onError(t.getMessage())
                );
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() ->
                        callback.onError("Request cancelled")
                );
            }
        });
    }

    private static Array<Hive> parseHives(String json) {

        Array<Hive> hives = new Array<>();
        JsonValue root = new JsonReader().parse(json);

        for (JsonValue h : root) {
            Hive hive = new Hive();
            hive.id = h.getInt("id");
            hive.name = h.getString("name");
            hive.location = h.getString("location");
            hive.type = h.getString("type");
            hive.status = h.getString("status");
            hive.id_location = h.getInt("id_location");
            hive.id_user = h.getInt("id_user");

            hives.add(hive);
        }

        return hives;
    }
}
