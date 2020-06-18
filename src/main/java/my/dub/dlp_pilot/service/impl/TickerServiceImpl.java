package my.dub.dlp_pilot.service.impl;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.client.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.client.RestClient;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Service
public class TickerServiceImpl implements TickerService {

    private final TickerContainer tickerContainer;
    private final RestClient restClient;
    private final TestRunService testRunService;
    private final ParametersComponent parameters;

    @Autowired
    public TickerServiceImpl(TickerContainer tickerContainer, RestClient restClient,
                             TestRunService testRunService, ParametersComponent parameters) {
        this.tickerContainer = tickerContainer;
        this.restClient = restClient;
        this.testRunService = testRunService;
        this.parameters = parameters;
    }

    @Override
    public void fetchAndSave(Exchange exchange) {
        if (testRunService.isTradeStopped()) {
            return;
        }
        Set<Ticker> tickers = new HashSet<>(restClient.fetchTickers(exchange));
        save(exchange.getName(), tickers);
    }

    @Override
    public void save(ExchangeName exchangeName, Collection<Ticker> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        tickerContainer.addTickers(exchangeName, tickers);
    }

    public boolean checkStale(Ticker ticker) {
        if (!ticker.isStale() &&
                DateUtils.durationSeconds(ticker.getDateTime()) > parameters.getStaleDifferenceSeconds()) {
            ticker.setStale(true);
            return true;
        }
        return false;
    }

    @Override
    public Set<Ticker> getTickers(ExchangeName exchangeName) {
        return tickerContainer.getTickers(exchangeName, false);
    }

    @Override
    public Set<Ticker> getAllTickers() {
        return tickerContainer.getAll(false);
    }

    @Override
    public Optional<Ticker> getTicker(ExchangeName exchangeName, String base, String target) {
        return tickerContainer.getTicker(exchangeName, base, target);
    }

    @Override
    public Ticker findEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet) {
        Objects.requireNonNull(originalTicker, "Ticker that has to be compared is null!");
        return tickerSet.stream().filter(ticker -> ticker.getBase().equals(originalTicker.getBase()) &&
                ticker.getTarget().equals(originalTicker.getTarget())).findAny().orElse(null);
    }
}
