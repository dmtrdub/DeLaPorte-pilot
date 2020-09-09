package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.client.ApiClient;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class TickerServiceImpl implements TickerService {

    private final TickerContainer tickerContainer;
    private final ApiClient apiClient;
    private final TestRunService testRunService;
    private final ParametersComponent parameters;

    @Autowired
    public TickerServiceImpl(TickerContainer tickerContainer, ApiClient apiClient, TestRunService testRunService,
            ParametersComponent parameters) {
        this.tickerContainer = tickerContainer;
        this.apiClient = apiClient;
        this.testRunService = testRunService;
        this.parameters = parameters;
    }

    @Override
    public void fetchAndSave(Exchange exchange) {
        if (testRunService.checkTestRunEnd()) {
            return;
        }
        save(exchange.getName(), apiClient.fetchTickers(exchange));
    }

    @Override
    public void save(ExchangeName exchangeName, Collection<Ticker> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        tickerContainer.addTickers(exchangeName, tickers);
    }

    @Override
    public Set<Ticker> getTickers(ExchangeName exchangeName) {
        return tickerContainer.getTickers(exchangeName);
    }

    @Override
    public Set<Ticker> getAllTickers() {
        return tickerContainer.getAll();
    }

    @Override
    public Optional<Ticker> getTicker(ExchangeName exchangeName, String base, String target) {
        return tickerContainer.getTicker(exchangeName, base, target);
    }

    @Override
    public Optional<Ticker> findValidEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet) {
        checkNotNull(originalTicker, "Ticker that has to be compared cannot be null!");
        if (originalTicker.isPriceInvalid()) {
            return Optional.empty();
        }
        return tickerSet.stream()
                .filter(ticker -> !ticker.isPriceInvalid() && ticker.getBase().equals(originalTicker.getBase())
                        && ticker.getTarget().equals(originalTicker.getTarget())).findAny();
    }

    @Override
    public boolean checkStale(Ticker ticker1, Ticker ticker2) {
        checkNotNull(ticker1, "Ticker1 that has to be checked as stale cannot be null!");
        checkNotNull(ticker2, "Ticker2 that has to be checked as stale cannot be null!");

        return checkTickerStale(ticker1) && checkTickerStale(ticker2);
    }

    private boolean checkTickerStale(Ticker ticker) {
        if (!ticker.isStale() && DateUtils.durationSeconds(ticker.getDateTime()) >= parameters
                .getStaleDifferenceSeconds()) {
            ticker.setStale(true);
        }
        return ticker.isStale();
    }
}
