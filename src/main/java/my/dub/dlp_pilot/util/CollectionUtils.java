package my.dub.dlp_pilot.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> List<T> toList(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .collect(Collectors.toList());
    }

    public static <T> Set<T> toSet(final Iterable<T> iterable) {
        Set<T> collect = new HashSet<>();
        try {
            collect = StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toSet());
        } catch (NullPointerException npe) {
            log.error(npe.getMessage());
        }
        return collect;
    }
}
