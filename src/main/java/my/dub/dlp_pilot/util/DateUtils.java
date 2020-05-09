package my.dub.dlp_pilot.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public final class DateUtils {
    private DateUtils() {
    }

    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.UTC;

    public static ZonedDateTime getDateTime(String dateTime) {
        return ZonedDateTime.ofInstant(Instant.parse(dateTime), DEFAULT_ZONE_OFFSET);
    }

    public static ZonedDateTime currentDateTime() {
        return ZonedDateTime.now(DEFAULT_ZONE_OFFSET);
    }

    public static long durationMinutes(Temporal start, Temporal end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toMinutes();
    }

    public static long durationSeconds(Temporal start, Temporal end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toSeconds();
    }
}
