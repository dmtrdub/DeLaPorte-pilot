package my.dub.dlp_pilot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.client.ClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class TickerServiceImplTest {
    private static final ExchangeName EXCHANGE_NAME = ExchangeName.BINANCE;

    @Mock
    private TickerContainer tickerContainer;
    @Mock
    private ClientService clientService;

    @InjectMocks
    private TickerServiceImpl service;

    @Test
    void findValidEquivalentTickerFromSet() {
        Ticker originalTicker = createTicker(EXCHANGE_NAME, "B1", "T1");
        Ticker ticker1 = createTicker(ExchangeName.BITMAX, "B1", "T2");
        Ticker ticker2 = createTicker(ExchangeName.BITMAX, "B2", "T1");
        Ticker ticker3 = createTicker(ExchangeName.BITMAX, "B1", "T1");

        assertThat(service.findValidEquivalentTickerFromSet(originalTicker, Set.of(ticker1, ticker2, ticker3)))
                .isEqualTo(Optional.of(ticker3));
    }

    @Test
    void checkStale() {
        Ticker ticker1 = createTicker(EXCHANGE_NAME, "B1", "T1");
        Ticker ticker2 = createTicker(ExchangeName.BITMAX, "B1", "T1");
        ticker2.setDateTime(Instant.now().minusSeconds(300));

        assertThat(service.checkStale(ticker1, ticker2, Duration.ofSeconds(60))).isFalse();
        assertThat(ticker2.isStale()).isTrue();
    }

    private Ticker createTicker(ExchangeName exchangeName, String base, String target) {
        Ticker ticker = new Ticker(exchangeName);
        ticker.setBase(base);
        ticker.setTarget(target);
        ticker.setPriceAsk(BigDecimal.ONE);
        ticker.setPriceBid(BigDecimal.ONE);
        return ticker;
    }
}