package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import si.um.feri.projketRRI.api.calls.model.Location;
import si.um.feri.projketRRI.utils.Constants;

public class LocationService {

    private static final String API_URL = Constants.API_URL;
    private static final String PREFS = "auth";

    /**
     * Callback for async result
     */
    public interface LocationCallback {
        void onSuccess(Location location);
        void onError(String message);
    }

    /**
     * Load location by ID
     */
    public static void loadLocation(int locationId, LocationCallback callback) {

        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            callback.onError("No auth token");
            return;
        }

        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.GET)
                .url(API_URL + "/location/" + locationId)
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
                        callback.onSuccess(parseLocation(body));
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

    /**
     * Parse JSON → Location
     */
    private static Location parseLocation(String json) {
        JsonValue root = new JsonReader().parse(json);

        if (!root.isArray() || root.size == 0) {
            throw new IllegalArgumentException("Expected non-empty JSON array for location, got: " + json);
        }

        JsonValue obj = root.get(0);

        Location location = new Location();
        location.id = obj.getInt("id");
        location.latitude = (float) obj.getDouble("latitude");
        location.longitude = (float) obj.getDouble("longitude");

        return location;
    }
}
