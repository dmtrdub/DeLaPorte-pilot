package my.dub.dlp_pilot.repository.container;

import java.util.Collections;
import java.util.Set;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Ticker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TickerContainerTest {

    private ExchangeName defaultExchangeName = ExchangeName.BIGONE;

    private TickerContainer tickerContainer;

    @BeforeEach
    void setUp() {
        tickerContainer = new TickerContainer();
        Set<Ticker> tickers = (Set<Ticker>) ReflectionTestUtils.getField(tickerContainer, "bigONETickers");
        tickers.add(new Ticker(defaultExchangeName));
    }

    @Test
    void addTickers_nullExchangeName() {
        tickerContainer.addTickers(null, Collections.emptyList());
        Assertions.assertEquals(1, tickerContainer.getTickers(defaultExchangeName).size());
    }

    @Test
    void addTickers_emptyTickersCollection() {
        tickerContainer.addTickers(defaultExchangeName, Collections.emptyList());
        Assertions.assertEquals(1, tickerContainer.getTickers(defaultExchangeName).size());
    }
}