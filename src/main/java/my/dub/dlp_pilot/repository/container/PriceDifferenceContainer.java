package my.dub.dlp_pilot.repository.container;

import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.PriceDifference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// Allow price differences with inverted exchanges - to ensure precise ask/bid difference
@Component
public class PriceDifferenceContainer {
    private final Set<PriceDifference> priceDifferences = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public Optional<PriceDifference> findPriceDifference(String base, String target, ExchangeName exchange1,
                                                         ExchangeName exchange2) {
        checkArgument(!StringUtils.isEmpty(base), "Base cannot be empty when searching Price Difference!");
        checkArgument(!StringUtils.isEmpty(target), "Target cannot be empty when searching Price Difference!");
        checkNotNull(exchange1, "Exchange 1 cannot be null when searching Price Difference!");
        checkNotNull(exchange2, "Exchange 2 cannot be null when searching Price Difference!");

        return priceDifferences.stream()
                               .filter(priceDiff -> isSimilarMatch(priceDiff, base, target, exchange1, exchange2))
                               .findFirst();
    }

    public boolean add(PriceDifference priceDifference) {
        if (priceDifference == null || isSimilarPresent(priceDifference)) {
            return false;
        }
        return priceDifferences.add(priceDifference);
    }

    public boolean isSimilarPresent(PriceDifference priceDifference) {
        return priceDifferences.stream().anyMatch(
                pD -> isSimilarMatch(pD, priceDifference.getBase(), priceDifference.getTarget(),
                                     priceDifference.getExchangeName(), priceDifference.getExchangeName2()));
    }

    private boolean isSimilarMatch(PriceDifference existingPD, String base, String target,
                                   ExchangeName exchange1, ExchangeName exchange2) {
        return existingPD.getBase().equals(base) && existingPD.getTarget().equals(target) &&
                existingPD.getExchangeName().equals(exchange1) && existingPD.getExchangeName2().equals(exchange2);
    }
}
