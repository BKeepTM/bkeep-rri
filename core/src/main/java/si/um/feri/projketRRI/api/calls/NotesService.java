package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import si.um.feri.projketRRI.api.calls.model.Notes;
import si.um.feri.projketRRI.utils.Constants;

import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotesService {

    private static final String API_URL = Constants.API_URL;
    // private static final String API_URL = "http://localhost:3000";

    private static final String PREFS = "auth";

    private static final OkHttpClient client = new OkHttpClient();

    private static final Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");


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
                throw new IOException("HTTP " + response.code() + ": " + safeBody(response));
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

    public static class CreateNoteResponse {
        public int id;
    }

    public interface CreateNoteCallback {
        void onSuccess(int insertedId);
        void onError(String message);
    }

    public static int createNote(String content, String time, int hiveId) throws IOException {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("time", time);
        payload.put("hiveId", hiveId);

        String jsonBody = gson.toJson(payload);

        Request request = new Request.Builder()
            .url(API_URL + "/notes")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .post(RequestBody.create(jsonBody, JSON_MEDIA))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = safeBody(response);

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + respBody);
            }

            CreateNoteResponse created = gson.fromJson(respBody, CreateNoteResponse.class);
            return created != null ? created.id : 0;
        }
    }

    public static void createNoteAsync(String content, String time, int hiveId, CreateNoteCallback callback) {
        new Thread(() -> {
            try {
                int id = createNote(content, time, hiveId);
                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onSuccess(id);
                });
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    public static class UpdateNoteResponse {
        public String message;
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public static String updateNote(int noteId, String content, String time, Integer idHive) throws IOException {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", noteId);

        if (content != null) payload.put("content", content);
        if (time != null) payload.put("time", time);

        if (idHive != null) {
            payload.put("id_hive", idHive);
        }

        String jsonBody = gson.toJson(payload);

        Request request = new Request.Builder()
            .url(API_URL + "/notes/update")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .post(RequestBody.create(jsonBody, JSON_MEDIA))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = safeBody(response);
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + respBody);
            }
            UpdateNoteResponse updated = gson.fromJson(respBody, UpdateNoteResponse.class);
            return (updated != null && updated.message != null) ? updated.message : "OK";
        }
    }


    public static void updateNoteAsync(int noteId, String content, String time, Integer idHive, SimpleCallback callback) {
        new Thread(() -> {
            try {
                String msg = updateNote(noteId, content, time, idHive);
                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onSuccess(msg);
                });
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    public static String removeNote(int noteId) throws IOException {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", noteId);

        String jsonBody = gson.toJson(payload);

        Request request = new Request.Builder()
            .url(API_URL + "/notes/remove")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "text/plain")
            .post(RequestBody.create(jsonBody, JSON_MEDIA))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = safeBody(response);

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + respBody);
            }
            return respBody != null ? respBody : "OK";
        }
    }

    public static void removeNoteAsync(int noteId, SimpleCallback callback) {
        new Thread(() -> {
            try {
                String msg = removeNote(noteId);
                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onSuccess(msg);
                });
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    private static String safeBody(Response response) throws IOException {
        ResponseBody rb = response.body();
        return rb != null ? rb.string() : "";
    }
}
