package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import si.um.feri.projketRRI.api.calls.model.Hive;
import si.um.feri.projketRRI.api.calls.model.Notification;

public class ApiClient {

    private static final String API_URL = "https://pi.darkosever.si";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final String PREFS = "auth";

    static Preferences prefs = Gdx.app.getPreferences(PREFS);
    static String token = prefs.getString("token", "");
    private static final Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    public static List<Hive> getHivesByUser() throws IOException {
        Request request = new Request.Builder()
            .url(API_URL + "/hive/list")
            .header("Authorization", "Bearer " + token)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            Type listType = new TypeToken<List<Hive>>(){}.getType();
            return gson.fromJson(responseBody, listType);
        }
    }

    public static List<Hive> getAllHives() throws IOException {
        Request request = new Request.Builder()
            .url(API_URL + "/hive/getAll")
            .header("Authorization", "Bearer " + token)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            Type listType = new TypeToken<List<Hive>>(){}.getType();
            return gson.fromJson(responseBody, listType);
        }
    }
    public static Hive getHiveById(int id) throws IOException {
        Request request = new Request.Builder()
            .url(API_URL + "/hive/" + id)
            .header("Authorization", "Bearer " + token)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            return gson.fromJson(responseBody, Hive.class);
        }
    }
    public static boolean removeHive(int id) throws IOException {
        RemoveRequest payload = new RemoveRequest(id);
        String jsonBody = gson.toJson(payload);

        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL + "/hive/remove")
            .header("Authorization", "Bearer " + token)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return true;
        }
    }

    public static List<Hive> searchHives(String nameQuery) throws IOException {
        SearchRequest searchPayload = new SearchRequest(nameQuery);
        String jsonBody = gson.toJson(searchPayload);

        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL + "/hive/search")
            .header("Authorization", "Bearer " + token)
            .method("GET", body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            Type listType = new TypeToken<List<Hive>>(){}.getType();
            return gson.fromJson(responseBody, listType);
        }
    }
    public static List<Notification> getNotifications() throws IOException {
        Request request = new Request.Builder()
            .url(API_URL + "/notification/list")
            .header("Authorization", "Bearer " + token)
            .get()
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            Type listType = new TypeToken<List<Notification>>(){}.getType();
            return gson.fromJson(responseBody, listType);
        }
    }
    private static class SearchRequest {
        String name;
        SearchRequest(String name) { this.name = name; }
    }

    private static class RemoveRequest {
        int id;
        RemoveRequest(int id) { this.id = id; }
    }
}
