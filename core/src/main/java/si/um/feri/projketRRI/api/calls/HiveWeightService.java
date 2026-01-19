package si.um.feri.projketRRI.api.calls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import si.um.feri.projketRRI.api.calls.model.HiveWeight;
import si.um.feri.projketRRI.utils.Constants;

public class HiveWeightService {

    private static final String API_URL = Constants.API_URL;

    //private static final String API_URL = "http://localhost:3000";

    private static final String PREFS = "auth";


    private static final OkHttpClient client = new OkHttpClient();

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    /** Fetch all hive weights from /hiveWeight/list */
    public static List<HiveWeight> loadAllHiveWeights() throws IOException {

        Preferences prefs = Gdx.app.getPreferences(PREFS);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(API_URL + "/hiveWeight/list")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String body = response.body().string();
            Type listType = new TypeToken<List<HiveWeight>>() {}.getType();
            return gson.fromJson(body, listType);
        }
    }

    /**
     * Groups weights by hive id (id_hive) into LibGDX structures.
     * Also sorts each hive's Array by timeWeight ascending (oldest -> newest).
     */
    public static IntMap<Array<HiveWeight>> groupByHiveId(List<HiveWeight> weights) {
        IntMap<Array<HiveWeight>> byHive = new IntMap<>();

        for (HiveWeight w : weights) {
            Array<HiveWeight> arr = byHive.get(w.idHive);
            if (arr == null) {
                arr = new Array<>();
                byHive.put(w.idHive, arr);
            }
            arr.add(w);
        }

        // sort each hive array by ISO time string
        for (IntMap.Entry<Array<HiveWeight>> e : byHive) {
            e.value.sort(new Comparator<HiveWeight>() {
                @Override
                public int compare(HiveWeight a, HiveWeight b) {
                    // Your strings are like "2025-10-05T10:00:00.000Z" -> ISO-8601
                    long ta = parseIsoToMillis(a.timeWeight);
                    long tb = parseIsoToMillis(b.timeWeight);
                    return Long.compare(ta, tb);
                }
            });
        }

        return byHive;
    }

    private static long parseIsoToMillis(String iso) {
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (Exception ex) {
            // If parsing fails, keep stable order but don’t crash
            return 0L;
        }
    }

    /**
     * Convenience: fetch + group in one call.
     */
    public static IntMap<Array<HiveWeight>> loadGroupedByHive() throws IOException {
        return groupByHiveId(loadAllHiveWeights());
    }


}
