package my.dub.dlp_pilot.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Utility class for date-related operations.
 */
public final class DateUtils {
    private DateUtils() {
    }

    private static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");
    private static final String FORMAT_PATTERN = "dd.MM.yyyy HH:mm:ss";
    private static final String FORMAT_PATTERN_SHORT = "ddMMyy-HHmm";

    public static String formatDateTime(@NonNull LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN);
        return dateTime.format(formatter);
    }

    public static String formatDateTime(@NonNull Instant dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN).withZone(ZONE_ID_UTC);
        return formatter.format(dateTime);
    }

    public static String formatDateTimeShort(@NonNull LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN_SHORT);
        return dateTime.format(formatter);
    }

    public static boolean isDurationLonger(@NonNull Temporal start, @NonNull Temporal end,
            @NonNull Duration toCompare) {
        Duration duration = Duration.between(start, end);
        return duration.compareTo(toCompare) > 0;
    }

    public static boolean isCurrentDurationLonger(@NonNull Temporal start, @NonNull Duration toCompare) {
        Duration duration = currentDuration(start);
        return duration.compareTo(toCompare) > 0;
    }

    public static long durationSeconds(@NonNull Temporal start) {
        return currentDuration(start).toSeconds();
    }

    public static Duration currentDuration(@NonNull Temporal start) {
        return Duration.between(start, Instant.now());
    }

    public static long durationMillis(@NonNull Temporal start) {
        return currentDuration(start).toMillis();
    }

    public static String durationSecondsDetailed(@Nullable Temporal start, @Nullable Temporal end) {
        if (start == null || end == null) {
            return "0";
        }
        Duration duration = Duration.between(start, end);
        return String.format("%d.%3d", duration.toSeconds(), duration.toMillisPart()).replaceAll("\\s+", "");
    }

    // ISO-8601 standard
    public static Duration parseDuration(@Nullable String input) {
        if (StringUtils.isEmpty(input) || "0".equals(input)) {
            return Duration.ZERO;
        }
        input = input.replaceAll("\\s", "").toUpperCase();
        StringBuilder inputBuilder = new StringBuilder(input);
        if (input.contains("D")) {
            if (!input.endsWith("D")) {
                inputBuilder.insert(input.indexOf("D") + 1, 'T');
            }
        } else {
            inputBuilder.insert(0, 'T');
        }
        if (!input.startsWith("P")) {
            inputBuilder.insert(0, 'P');
        }
        return Duration.parse(inputBuilder);
    }

    public static String formatDuration(@NonNull Duration duration) {
        return duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
    }

    public static String toIsoInstantString(@NonNull Instant dateTime) {
        return DateTimeFormatter.ISO_INSTANT.withZone(ZONE_ID_UTC).format(dateTime);
    }

    public static Instant toInstant(@NonNull LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static LocalDateTime toLocalDateTime(@NonNull Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static String formatLocalDateTime(@NonNull Instant instant) {
        return formatDateTime(toLocalDateTime(instant));
    }
}
