package my.dub.dlp_pilot.repository.container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.PriceData;
import my.dub.dlp_pilot.model.SymbolPair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SymbolPairContainer {

    private final Map<ExchangeName, List<SymbolPair>> symbolPairsMap = new ConcurrentHashMap<>();

    public void add(@NonNull List<SymbolPair> symbolPairs) {
        checkNotNull(symbolPairs, "Cannot add null item list to container!");
        SymbolPair symbolPairFirst = symbolPairs.get(0);
        checkArgument(symbolPairs.stream().allMatch(
                symbolPair -> symbolPair.getExchangeName().equals(symbolPairFirst.getExchangeName())),
                      "SymbolPairs cannot have different ExchangeNames when adding to container!");

        symbolPairsMap.put(symbolPairFirst.getExchangeName(), symbolPairs);
    }

    public List<SymbolPair> getAll(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, "Cannot get list if ExchangeName is null!");

        return symbolPairsMap.get(exchangeName);
    }

    public SymbolPair get(@NonNull ExchangeName exchangeName, int index) {
        checkNotNull(exchangeName, "Cannot get item if ExchangeName is null!");
        checkArgument(index >= 0, "Cannot get item if index < 0!");

        List<SymbolPair> symbolPairs = symbolPairsMap.get(exchangeName);
        if (index >= symbolPairs.size()) {
            return null;
        }
        return symbolPairs.get(index);
    }

    public int size(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, "Cannot count size if ExchangeName is null!");

        List<SymbolPair> symbolPairs = symbolPairsMap.get(exchangeName);
        return CollectionUtils.isEmpty(symbolPairs) ? 0 : symbolPairs.size();
    }

    public void updateAll(@NonNull Collection<? extends PriceData> priceDataToRetain) {
        checkNotNull(priceDataToRetain, "Cannot retain symbolPairs if price data is null!");

        Map<ExchangeName, List<SymbolPair>> newSymbolPairsMap =
                symbolPairsMap.values().stream().flatMap(Collection::stream)
                        .filter(symbolPair -> priceDataToRetain.stream()
                                .anyMatch(priceData -> priceData.isSimilar(symbolPair)))
                        .collect(Collectors.groupingBy(PriceData::getExchangeName));
        symbolPairsMap.clear();
        symbolPairsMap.putAll(newSymbolPairsMap);
    }
}
