package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.PriceDifference;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.repository.container.PriceDifferenceContainer;
import my.dub.dlp_pilot.service.PriceDifferenceService;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PriceDifferenceServiceImpl implements PriceDifferenceService {

    private final PriceDifferenceContainer container;

    private final TickerService tickerService;
    private final TestRunService testRunService;
    private final TradeService tradeService;
    private final ParametersComponent parameters;

    @Autowired
    public PriceDifferenceServiceImpl(TickerService tickerService, PriceDifferenceContainer container,
            TestRunService testRunService, TradeService tradeService, ParametersComponent parameters) {
        this.tickerService = tickerService;
        this.container = container;
        this.testRunService = testRunService;
        this.tradeService = tradeService;
        this.parameters = parameters;
    }

    @Override
    public void handlePriceDifference(ExchangeName exchangeName) {
        Set<Ticker> allTickers = tickerService.checkAndGetTickers();
        Set<Ticker> tickersToCompare = tickerService.getTickers(exchangeName);
        allTickers.removeAll(tickersToCompare);
        allTickers.forEach(ticker -> {
            Ticker equivalentTicker = tickerService.findValidEquivalentTickerFromSet(ticker, tickersToCompare);
            if (equivalentTicker == null) {
                return;
            }
            String base = ticker.getBase();
            String target = ticker.getTarget();
            ExchangeName exchangeName1 = ticker.getExchangeName();
            ExchangeName exchangeName2 = equivalentTicker.getExchangeName();
            final Optional<PriceDifference> priceDifferenceOptional =
                    container.findPriceDifference(base, target, exchangeName1, exchangeName2);
            if (priceDifferenceOptional.isEmpty()) {
                PriceDifference priceDifference =
                        createPriceDifference(ticker, equivalentTicker, base, target, exchangeName1, exchangeName2);
                boolean added = container.add(priceDifference);
                checkState(added,
                           "Price Difference should have been added to container! " + priceDifference.toShortString());
                log.trace("New {} added to container", priceDifference.toShortString());
                return;
            }
            final PriceDifference priceDifference = priceDifferenceOptional.get();
            if (!DateUtils.isDurationLonger(priceDifference.getLastRecordDateTime(), DateUtils.currentDateTime(),
                                            parameters.getDataCaptureIntervalDuration())) {
                return;
            }
            List<BigDecimal> values = priceDifference.getValues();
            if (!testRunService.isInitialDataCapture()) {
                values.remove(0);
            }
            BigDecimal currentPriceDifference = getCurrentPriceDiffValue(ticker, equivalentTicker);
            values.add(currentPriceDifference);
            BigDecimal currentAverage = Calculations.average(priceDifference.getValues());
            priceDifference.setAvgValue(currentAverage);

            handleValidPriceDifference(priceDifference, ticker, equivalentTicker, currentPriceDifference,
                                       currentAverage);
        });
    }

    private void handleValidPriceDifference(PriceDifference priceDifference, Ticker ticker1, Ticker ticker2,
            BigDecimal currentPriceDifference, BigDecimal currentAverage) {
        if (!testRunService.isInitialDataCapture() && !testRunService.isTradeStopped() && !testRunService.isTestRunEnd()
                && currentPriceDifference.compareTo(BigDecimal.ZERO) > 0
                && currentPriceDifference.compareTo(currentAverage) > 0) {
            // set breakthrough price + time if first breakthrough occurs, or when data capture period has passed
            if (priceDifference.getBreakThroughAvgPriceDiff() == null || (
                    parameters.getPriceDataInvalidateAfterSeconds() != 0
                            && DateUtils.durationSeconds(priceDifference.getBreakThroughDateTime()) > parameters
                            .getPriceDataInvalidateAfterSeconds())) {
                log.trace("Updated breakthrough price difference ({}) for {}", currentAverage,
                          priceDifference.toShortString());
                priceDifference.setCurrentBreakThroughPrice(currentAverage);
            }
            tradeService.checkTradeOpen(priceDifference, ticker1, ticker2);
        }
    }

    private PriceDifference createPriceDifference(Ticker ticker, Ticker equivalentTicker, String base, String target,
            ExchangeName exchangeName1, ExchangeName exchangeName2) {
        PriceDifference priceDifference = new PriceDifference();
        priceDifference.setBase(base);
        priceDifference.setTarget(target);
        priceDifference.setExchangeName(exchangeName1);
        priceDifference.setExchangeName2(exchangeName2);
        priceDifference.getValues().add(getCurrentPriceDiffValue(ticker, equivalentTicker));
        priceDifference.setLastRecordDateTime(DateUtils.currentDateTime());
        return priceDifference;
    }

    private BigDecimal getCurrentPriceDiffValue(Ticker ticker1, Ticker ticker2) {
        return ticker1.getPriceBid().subtract(ticker2.getPriceAsk());
    }
}
