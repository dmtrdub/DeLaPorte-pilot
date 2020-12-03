package my.dub.dlp_pilot.service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface TickerService {

    void fetchAndSave(ExchangeName exchangeName);

    Set<Ticker> getTickers(ExchangeName exchangeName);

    Set<Ticker> getAllTickers();

    @Retryable(value = MissingEntityException.class, backoff = @Backoff(0))
    Ticker getTickerWithRetry(ExchangeName exchangeName, String base, String target);

    Optional<Ticker> findValidEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet);

    boolean checkStale(Ticker ticker1, Ticker ticker2, Duration staleIntervalDuration);
}
