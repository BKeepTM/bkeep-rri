package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import si.um.feri.projketRRI.api.calls.model.Notes;

public class NotesService {

    //private static final String API_URL = "https://pi.darkosever.si";

    private static final String API_URL = "http://localhost:3000";

    private static final String PREFS = "auth";



    private static final OkHttpClient client = new OkHttpClient();

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public static List<Notes> getNotesForHive(int hiveId) throws IOException {

        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(API_URL + "/notes/" + hiveId)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String body = response.body().string();
            Type listType = new TypeToken<List<Notes>>() {}.getType();
            return gson.fromJson(body, listType);
        }
    }

    public interface NotesCallback {
        void onSuccess(List<Notes> notes);
        void onError(String message);
    }

    public static void loadNotesForHive(int hiveId, NotesCallback callback) {
        new Thread(() -> {
            try {
                List<Notes> notes = getNotesForHive(hiveId);

                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onSuccess(notes);
                });

            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }
}
