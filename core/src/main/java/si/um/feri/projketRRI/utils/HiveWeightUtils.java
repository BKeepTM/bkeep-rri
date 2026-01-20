package si.um.feri.projketRRI.utils;

import com.badlogic.gdx.utils.Array;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import si.um.feri.projketRRI.api.calls.model.HiveWeight;

public final class HiveWeightUtils {

    private HiveWeightUtils() {}

    public static HiveWeight latest(Array<HiveWeight> hiveWeights) {
        if (hiveWeights == null || hiveWeights.size == 0) return null;

        HiveWeight best = null;
        long bestTs = Long.MIN_VALUE;
        int bestId = Integer.MIN_VALUE;

        for (int i = 0; i < hiveWeights.size; i++) {
            HiveWeight hw = hiveWeights.get(i);
            if (hw == null) continue;

            long ts = parseToEpochMillis(hw.timeWeight);
            if (ts != Long.MIN_VALUE) {
                if (best == null || ts > bestTs) {
                    best = hw;
                    bestTs = ts;
                    bestId = hw.id;
                }
            } else {
                if (best == null && hw.id > bestId) {
                    best = hw;
                    bestId = hw.id;
                } else if (best != null && bestTs == Long.MIN_VALUE && hw.id > bestId) {
                    best = hw;
                    bestId = hw.id;
                }
            }
        }

        return best;
    }

    private static long parseToEpochMillis(String s) {
        if (s == null) return Long.MIN_VALUE;
        String x = s.trim();
        if (x.isEmpty()) return Long.MIN_VALUE;

        try {
            return Instant.parse(x).toEpochMilli();
        } catch (Exception ignored) {}

        try {
            return OffsetDateTime.parse(x).toInstant().toEpochMilli();
        } catch (Exception ignored) {}

        try {
            LocalDateTime ldt = LocalDateTime.parse(x, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignored) {}

        return Long.MIN_VALUE;
    }
}
