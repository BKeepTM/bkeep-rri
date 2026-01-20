package si.um.feri.projketRRI.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class WeatherFormat {

    private static final DateTimeFormatter OUT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter SQL =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Accepts:
     *  - 2026-01-20T00:00:00.000Z (ISO Instant)
     *  - 2026-01-20 09:17:13      (SQL datetime)
     *  - 2026-01-20T00:00:00     (ISO LocalDateTime without Z)
     */
    public static String formatReportDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "-";
        String s = raw.trim();

        // 1) ISO with Z (Instant)
        try {
            Instant ins = Instant.parse(s);
            return OUT.withZone(ZoneId.systemDefault()).format(ins);
        } catch (Exception ignored) {}

        // 2) SQL datetime
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, SQL);
            return ldt.format(OUT);
        } catch (Exception ignored) {}

        // 3) ISO LocalDateTime without Z
        try {
            LocalDateTime ldt = LocalDateTime.parse(s);
            return ldt.format(OUT);
        } catch (Exception ignored) {}

        // fallback
        return s;
    }
}
