package my.dub.dlp_pilot.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class DateUtils {
    private DateUtils() {
    }

    public static LocalDateTime getUTCLocalDateTimeFromString(String ldt) {
        return LocalDateTime.ofInstant(Instant.parse(ldt), ZoneOffset.UTC);
    }
}
