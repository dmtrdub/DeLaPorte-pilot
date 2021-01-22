package my.dub.dlp_pilot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void formatDuration() {
        String duration = DateUtils.formatDuration(Duration.ofDays(15));
        assertTrue(duration.contains("h"));
    }

    @Test
    void parseDuration_short() {
        Duration duration = DateUtils.parseDuration("5d");
        assertEquals(5, duration.toDays());
    }

    @Test
    void parseDuration() {
        Duration duration = DateUtils.parseDuration("1D 5h 20M 30s");
        assertEquals(5, duration.toHoursPart());
    }
}