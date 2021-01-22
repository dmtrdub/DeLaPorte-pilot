package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.client.ClientService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link TickerService} service.
 */
@Slf4j
@Service
public class TickerServiceImpl implements TickerService {

    private static final String EXCHANGE_NAME_PARAMETER = "exchangeName";

    private final TickerContainer tickerContainer;
    private final ClientService clientService;

    @Autowired
    public TickerServiceImpl(TickerContainer tickerContainer, ClientService clientService) {
        this.tickerContainer = tickerContainer;
        this.clientService = clientService;
    }

    @Override
    public void fetchAndSave(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);

        Set<Ticker> tickers = clientService.fetchTickers(exchangeName);
        tickerContainer.addTickers(exchangeName, tickers);
    }

    @Override
    public Set<Ticker> getTickers(@NonNull ExchangeName exchangeName) {
        return tickerContainer
                .getTickers(checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER));
    }

    @Override
    public Set<Ticker> getAllTickers() {
        return tickerContainer.getAll();
    }

    @Override
    public Ticker getTickerWithRetry(@NonNull ExchangeName exchangeName, @NonNull String base, @NonNull String target) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);
        checkNotNull(base, Constants.NULL_ARGUMENT_MESSAGE, "base");
        checkNotNull(target, Constants.NULL_ARGUMENT_MESSAGE, "target");

        return tickerContainer.getTicker(exchangeName, base, target)
                .orElseThrow(() -> new MissingEntityException(Ticker.class, exchangeName.getFullName(), base, target));
    }

    @Override
    public Optional<Ticker> findValidEquivalentTickerFromSet(@NonNull Ticker originalTicker,
            @NonNull Set<Ticker> tickerSet) {
        checkNotNull(originalTicker, Constants.NULL_ARGUMENT_MESSAGE, "originalTicker");
        checkNotNull(tickerSet, Constants.NULL_ARGUMENT_MESSAGE, "tickerSet");

        if (originalTicker.isPriceInvalid()) {
            log.trace("Invalid price found in {}!", originalTicker.toShortString());
            return Optional.empty();
        }
        return tickerSet.stream()
                .filter(ticker -> !ticker.isPriceInvalid() && ticker.getBase().equals(originalTicker.getBase())
                        && ticker.getTarget().equals(originalTicker.getTarget())).findAny();
    }

    @Override
    public boolean checkStale(@NonNull Ticker ticker1, @NonNull Ticker ticker2,
            @NonNull Duration staleIntervalDuration) {
        checkNotNull(ticker1, Constants.NULL_ARGUMENT_MESSAGE, "ticker1");
        checkNotNull(ticker2, Constants.NULL_ARGUMENT_MESSAGE, "ticker2");
        checkNotNull(staleIntervalDuration, Constants.NULL_ARGUMENT_MESSAGE, "staleIntervalDuration");

        boolean staleTicker1 = checkTickerStale(ticker1, staleIntervalDuration);
        boolean staleTicker2 = checkTickerStale(ticker2, staleIntervalDuration);
        return staleTicker1 && staleTicker2;
    }

    private boolean checkTickerStale(Ticker ticker, Duration staleIntervalDuration) {
        if (!ticker.isStale() && DateUtils.durationMillis(ticker.getDateTime()) >= staleIntervalDuration.toMillis()) {
            ticker.setStale(true);
        }
        return ticker.isStale();
    }
}
