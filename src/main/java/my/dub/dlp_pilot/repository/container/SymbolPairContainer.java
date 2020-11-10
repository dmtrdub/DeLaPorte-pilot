package my.dub.dlp_pilot.repository.container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.PriceData;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SymbolPairContainer {

    private final Map<ExchangeName, List<SymbolPair>> symbolPairsMap = new ConcurrentHashMap<>();

    public void addAll(@NonNull Collection<SymbolPair> symbolPairs) {
        checkNotNull(symbolPairs, "Cannot add null item to container!");
        Map<ExchangeName, List<SymbolPair>> collectedSP =
                symbolPairs.stream().collect(Collectors.groupingBy(PriceData::getExchangeName));
        symbolPairsMap.putAll(collectedSP);
    }

    public List<SymbolPair> getAll(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, "Cannot get list if ExchangeName is null!");

        return symbolPairsMap.get(exchangeName);
    }

    public List<SymbolPair> getAll() {
        return symbolPairsMap.entrySet().stream()
                .flatMap(exchangeNameListEntry -> exchangeNameListEntry.getValue().stream())
                .collect(Collectors.toList());
    }

    public SymbolPair get(@NonNull ExchangeName exchangeName, int index) {
        checkNotNull(exchangeName, "Cannot get item if ExchangeName is null!");
        checkArgument(index >= 0, "Cannot get item if index < 0!");

        List<SymbolPair> symbolPairs = symbolPairsMap.get(exchangeName);
        return symbolPairs.get(index);
    }

    public int size(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, "Cannot count size if ExchangeName is null!");

        List<SymbolPair> symbolPairs = symbolPairsMap.get(exchangeName);
        return CollectionUtils.isEmpty(symbolPairs) ? 0 : symbolPairs.size();
    }

    public void remove(@NonNull ExchangeName exchangeName, int index) {
        checkNotNull(exchangeName, "Cannot remove item if ExchangeName is null!");
        checkArgument(index >= 0, "Cannot remove item if index < 0!");

        Optional.ofNullable(symbolPairsMap.get(exchangeName)).ifPresent(symbolPairs -> symbolPairs.remove(index));
    }

    public void removeAll() {
        symbolPairsMap.clear();
    }
}
