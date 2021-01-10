package my.dub.dlp_pilot.repository.container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.PriceData;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * An in-memory container for {@link SymbolPair} objects. Uses a concurrent map implementation, where the key represents
 * the {@link ExchangeName}. NOTE: this container <b>allows duplicates</b>!
 */
@Component
public class SymbolPairContainer {

    private final Map<ExchangeName, List<SymbolPair>> symbolPairsMap = new ConcurrentHashMap<>();

    /**
     * Add multiple {@link SymbolPair} objects to container.
     *
     * @param symbolPairs
     *         a non-null collection of symbol pairs, possibly having different {@link SymbolPair#getExchangeName()}
     */
    public void addAll(@NonNull Collection<SymbolPair> symbolPairs) {
        checkNotNull(symbolPairs, Constants.NULL_ARGUMENT_MESSAGE, "symbolPairs");
        Map<ExchangeName, List<SymbolPair>> collectedSP =
                symbolPairs.stream().collect(Collectors.groupingBy(PriceData::getExchangeName));
        symbolPairsMap.putAll(collectedSP);
    }

    /**
     * Get all records for a specific {@link ExchangeName}.
     *
     * @param exchangeName
     *         a non-null exchange name
     *
     * @return a non-null list of {@link SymbolPair} objects with the specified exchange name
     */
    public List<SymbolPair> getAll(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");

        return Optional.ofNullable(symbolPairsMap.get(exchangeName)).orElse(new ArrayList<>());
    }

    /**
     * Get all records in container.
     *
     * @return a non-null list of all {@link SymbolPair} objects stored in this container
     */
    public List<SymbolPair> getAll() {
        return symbolPairsMap.entrySet().stream()
                .flatMap(exchangeNameListEntry -> exchangeNameListEntry.getValue().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get a record with a specific {@link ExchangeName} and stored under a specific index.
     *
     * @param exchangeName
     *         a non-null exchange name
     * @param index
     *         a non-negative index of the container's underlying list
     *
     * @return a {@link SymbolPair} object
     *
     * @throws IndexOutOfBoundsException
     *         if the index value exceeds the number of elements in the container's underlying list
     */
    public SymbolPair get(@NonNull ExchangeName exchangeName, int index) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");
        checkArgument(index >= 0, "Cannot get item if index < 0!");

        List<SymbolPair> symbolPairs = symbolPairsMap.get(exchangeName);
        return symbolPairs.get(index);
    }

    /**
     * Get the number of records with a specific {@link ExchangeName}.
     *
     * @param exchangeName
     *         a non-null exchange name
     *
     * @return the number of records
     */
    public int size(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");

        List<SymbolPair> symbolPairs = symbolPairsMap.get(exchangeName);
        return CollectionUtils.isEmpty(symbolPairs) ? 0 : symbolPairs.size();
    }

    /**
     * Remove a record with a specific {@link ExchangeName} stored under a specific index.
     *
     * @param exchangeName
     *         a non-null exchange name
     * @param index
     *         a non-negative index of the container's underlying list
     *
     * @throws IndexOutOfBoundsException
     *         if the index value exceeds the number of elements in the container's underlying list
     */
    public void remove(@NonNull ExchangeName exchangeName, int index) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");
        checkArgument(index >= 0, "Cannot remove item if index < 0!");

        Optional.ofNullable(symbolPairsMap.get(exchangeName)).ifPresent(symbolPairs -> symbolPairs.remove(index));
    }

    /**
     * Remove all container records.
     */
    public void removeAll() {
        symbolPairsMap.clear();
    }
}
