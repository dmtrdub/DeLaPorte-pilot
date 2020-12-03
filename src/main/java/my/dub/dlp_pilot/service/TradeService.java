package my.dub.dlp_pilot.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.model.dto.Ticker;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface TradeService {

    void checkTradeOpen(Ticker tickerShort, Ticker tickerLong, BigDecimal averagePriceDifference, TestRun testRun);

    void handleTrades(ExchangeName exchangeName);

    @Retryable(value = LockAcquisitionException.class, backoff = @Backoff(0))
    void closeTrades(@NonNull ExchangeName exchangeName, @NonNull TradeResultType tradeResultType);

    Set<Trade> getCompletedTradesNotWrittenToFile(TestRun testRun);

    void saveOrUpdate(Collection<Trade> trades);

    boolean isAllTradesClosed();
}
