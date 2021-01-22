package my.dub.dlp_pilot.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.lang.NonNull;

/**
 * Utility class for operations with Collections.
 */
public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> List<T> toList(@NonNull Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

    public static <T> Set<T> toSet(@NonNull Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toSet());
    }
}
