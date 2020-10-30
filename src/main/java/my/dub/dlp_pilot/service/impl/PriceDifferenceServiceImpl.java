package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.BarAverage;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.PriceDifference;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.service.PriceDifferenceService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class PriceDifferenceServiceImpl implements PriceDifferenceService {

    private final Set<PriceDifference> priceDifferences = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final TickerService tickerService;
    private final TradeService tradeService;

    @Autowired
    public PriceDifferenceServiceImpl(TickerService tickerService, TradeService tradeService) {
        this.tickerService = tickerService;
        this.tradeService = tradeService;
    }

    @Override
    public void createPriceDifferences(@NonNull List<BarAverage> barAverages) {
        for (int i = 0; i < barAverages.size() - 1; i++) {
            for (int j = i + 1; j < barAverages.size(); j++) {
                BarAverage bA1 = barAverages.get(i);
                BarAverage bA2 = barAverages.get(j);
                if (bA1.getBase().equals(bA2.getBase()) && bA1.getTarget().equals(bA2.getTarget()) && !bA1
                        .getExchangeName().equals(bA2.getExchangeName()) && findPriceDifference(bA1.getBase(),
                                                                                                bA1.getTarget(),
                                                                                                bA1.getExchangeName(),
                                                                                                bA2.getExchangeName())
                        .isEmpty()) {
                    PriceDifference priceDifference =
                            new PriceDifference(bA1.getBase(), bA1.getTarget(), bA1.getExchangeName(),
                                                bA2.getExchangeName());
                    priceDifference.setAverage(bA1.getAveragePrice().subtract(bA2.getAveragePrice()));
                    priceDifferences.add(priceDifference);
                }
            }
        }
    }

    @Override
    public void updatePriceDifferences(@NonNull List<BarAverage> barAverages) {
        for (int i = 0; i < barAverages.size() - 1; i++) {
            for (int j = i + 1; j < barAverages.size(); j++) {
                BarAverage bA1 = barAverages.get(i);
                BarAverage bA2 = barAverages.get(j);
                if (bA1.getBase().equals(bA2.getBase()) && bA1.getTarget().equals(bA2.getTarget()) && !bA1
                        .getExchangeName().equals(bA2.getExchangeName())) {
                    String base = bA1.getBase();
                    String target = bA1.getTarget();
                    ExchangeName exchangeName = bA1.getExchangeName();
                    ExchangeName exchangeName2 = bA2.getExchangeName();
                    PriceDifference priceDifference =
                            findPriceDifference(base, target, exchangeName, exchangeName2).orElse(null);
                    if (priceDifference == null) {
                        log.error("Unable to find existing Price Difference by base: {}, target: {}, exchangeName: {}, "
                                          + "exchangeName2: {}", base, target, exchangeName, exchangeName2);
                        continue;
                    }
                    priceDifference.setAverage(bA1.getAveragePrice().subtract(bA2.getAveragePrice()));
                }
            }
        }
    }

    @Override
    public void handlePriceDifference(ExchangeName exchangeName, TestRun testRun) {
        Set<Ticker> allTickers = tickerService.getAllTickers();
        Set<Ticker> tickersToCompare = tickerService.getTickers(exchangeName);
        allTickers.removeAll(tickersToCompare);
        allTickers.forEach(equivalentTicker -> {
            Optional<Ticker> tickerOpt =
                    tickerService.findValidEquivalentTickerFromSet(equivalentTicker, tickersToCompare);
            if (tickerOpt.isEmpty()) {
                return;
            }
            Ticker ticker = tickerOpt.get();
            String base = ticker.getBase();
            String target = ticker.getTarget();
            ExchangeName exchangeNameEquivalent = equivalentTicker.getExchangeName();
            Optional<PriceDifference> priceDifferenceOptional =
                    findPriceDifference(base, target, exchangeName, exchangeNameEquivalent);
            if (priceDifferenceOptional.isEmpty()) {
                log.error("No price difference was found for: {} and {} exchanges, base {} and target {}", exchangeName,
                          exchangeNameEquivalent, base, target);
                return;
            }

            final PriceDifference priceDifference = priceDifferenceOptional.get();
            Ticker ticker1;
            Ticker ticker2;
            if (ticker.getExchangeName().equals(priceDifference.getExchangeName())) {
                ticker1 = ticker;
                ticker2 = equivalentTicker;
            } else {
                ticker1 = equivalentTicker;
                ticker2 = ticker;
            }
            BigDecimal currentTickerValue = getCurrentPriceDiffValue(ticker1, ticker2);
            if (canCheckTradeOpen(currentTickerValue, priceDifference.getAverage())) {
                tradeService.checkTradeOpen(ticker1, ticker2, priceDifference.getAverage(), testRun);
            } else {
                currentTickerValue = getCurrentPriceDiffValue(ticker2, ticker1);
                if (canCheckTradeOpen(currentTickerValue, priceDifference.getAverage().negate())) {
                    tradeService.checkTradeOpen(ticker2, ticker1, priceDifference.getAverage().negate(), testRun);
                }
            }
        });
    }

    private boolean canCheckTradeOpen(BigDecimal currentValue, BigDecimal avgValue) {
        return currentValue.compareTo(BigDecimal.ZERO) > 0 && currentValue.compareTo(avgValue) > 0;
    }

    private BigDecimal getCurrentPriceDiffValue(Ticker ticker1, Ticker ticker2) {
        return ticker1.getPriceBid().subtract(ticker2.getPriceAsk());
    }

    private Optional<PriceDifference> findPriceDifference(String base, String target, ExchangeName exchange1,
            ExchangeName exchange2) {
        checkArgument(!StringUtils.isEmpty(base), "Base cannot be empty when searching Price Difference!");
        checkArgument(!StringUtils.isEmpty(target), "Target cannot be empty when searching Price Difference!");
        checkNotNull(exchange1, "Exchange 1 cannot be null when searching Price Difference!");
        checkNotNull(exchange2, "Exchange 2 cannot be null when searching Price Difference!");

        return priceDifferences.stream()
                .filter(priceDiff -> isSimilarMatch(priceDiff, base, target, exchange1, exchange2)).findFirst();
    }

    private boolean isSimilarMatch(PriceDifference existingPD, String base, String target, ExchangeName exchange1,
            ExchangeName exchange2) {
        return existingPD.getBase().equals(base) && existingPD.getTarget().equals(target) && (
                (existingPD.getExchangeName().equals(exchange1) && existingPD.getExchangeName2().equals(exchange2)) || (
                        existingPD.getExchangeName().equals(exchange2) && existingPD.getExchangeName2()
                                .equals(exchange1)));
    }
}
