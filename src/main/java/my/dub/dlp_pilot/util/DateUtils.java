package my.dub.dlp_pilot.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

public final class DateUtils {
    private DateUtils() {
    }

    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.UTC;
    private static final String FORMAT_PATTERN = "dd.MM.yyyy-HH:mm:ss";
    private static final String FORMAT_PATTERN_SHORT = "ddMMyy-HHmm";

    public static ZonedDateTime getDateTimeFromEpochMilli(long epoch) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), DEFAULT_ZONE_OFFSET);
    }

    public static ZonedDateTime getDateTimeFromEpochSecond(long epoch) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), DEFAULT_ZONE_OFFSET);
    }

    public static ZonedDateTime currentDateTimeUTC() {
        return ZonedDateTime.now(DEFAULT_ZONE_OFFSET);
    }

    public static String formatDateTime(ZonedDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN);
        return dateTime.format(formatter);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN);
        return dateTime.format(formatter);
    }

    public static String formatDateTimeShort(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN_SHORT);
        return dateTime.format(formatter);
    }

    public static boolean isDurationLonger(Temporal start, Temporal end, Duration toCompare) {
        Duration duration = Duration.between(start, end);
        return duration.compareTo(toCompare) > 0;
    }

    public static long durationMinutes(Temporal start) {
        if (start == null) {
            return 0;
        }
        return Duration.between(start, currentDateTimeUTC()).toMinutes();
    }

    public static long durationSeconds(Temporal start) {
        if (start == null) {
            return 0;
        }
        return Duration.between(start, currentDateTimeUTC()).toSeconds();
    }

    public static long durationMillis(Temporal start) {
        if (start == null) {
            return 0;
        }
        return Duration.between(start, currentDateTimeUTC()).toMillis();
    }

    public static String durationSecondsDetailed(Temporal start, Temporal end) {
        if (start == null || end == null) {
            return "0";
        }
        Duration duration = Duration.between(start, end);
        return String.format("%d.%3d", duration.toSeconds(), duration.toMillisPart()).replaceAll("\\s+", "");
    }

    public static Duration parseDuration(@NonNull String input) {
        if (StringUtils.isEmpty(input) || "0".equals(input)) {
            return Duration.ZERO;
        }
        if (!input.startsWith("PT")) {
            input = "PT" + input;
        }
        return Duration.parse(input);
    }

    public static String formatDuration(@NonNull Duration duration) {
        return duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
    }
}
