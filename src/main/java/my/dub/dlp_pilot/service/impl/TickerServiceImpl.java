package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.client.ClientService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TickerServiceImpl implements TickerService {

    private final TickerContainer tickerContainer;
    private final ClientService clientService;

    @Autowired
    public TickerServiceImpl(TickerContainer tickerContainer, ClientService clientService) {
        this.tickerContainer = tickerContainer;
        this.clientService = clientService;
    }

    @Override
    public void fetchAndSave(ExchangeName exchangeName) {
        Set<Ticker> tickers = clientService.fetchTickers(exchangeName);
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
    public Ticker getTickerWithRetry(ExchangeName exchangeName, String base, String target) {
        return tickerContainer.getTicker(exchangeName, base, target)
                .orElseThrow(() -> new MissingEntityException(Ticker.class, exchangeName.getFullName(), base, target));
    }

    @Override
    public Optional<Ticker> findValidEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet) {
        checkNotNull(originalTicker, "Ticker that has to be compared cannot be null!");
        if (originalTicker.isPriceInvalid()) {
            log.trace("Invalid price found in {}!", originalTicker.toShortString());
            return Optional.empty();
        }
        return tickerSet.stream()
                .filter(ticker -> !ticker.isPriceInvalid() && ticker.getBase().equals(originalTicker.getBase())
                        && ticker.getTarget().equals(originalTicker.getTarget())).findAny();
    }

    @Override
    public boolean checkStale(Ticker ticker1, Ticker ticker2, Duration staleIntervalDuration) {
        checkNotNull(ticker1, "Ticker1 that has to be checked as stale cannot be null!");
        checkNotNull(ticker2, "Ticker2 that has to be checked as stale cannot be null!");

        return checkTickerStale(ticker1, staleIntervalDuration) && checkTickerStale(ticker2, staleIntervalDuration);
    }

    private boolean checkTickerStale(Ticker ticker, Duration staleIntervalDuration) {
        if (!ticker.isStale() && DateUtils.durationMillis(ticker.getDateTime()) >= staleIntervalDuration.toMillis()) {
            ticker.setStale(true);
        }
        return ticker.isStale();
    }
}
