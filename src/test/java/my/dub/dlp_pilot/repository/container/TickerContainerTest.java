package my.dub.dlp_pilot.repository.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
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
        Ticker ticker = new Ticker(defaultExchangeName);
        ticker.setBase("B");
        ticker.setTarget("T");
        ticker.setClosePrice(BigDecimal.ONE);
        ticker.setPriceBid(BigDecimal.ONE);
        ticker.setPriceAsk(BigDecimal.ONE);
        tickers.add(ticker);
    }

    @Test
    void getTicker() {
        Ticker t1 = new Ticker(defaultExchangeName);
        t1.setBase("C");
        t1.setTarget("T");
        Ticker t2 = new Ticker(defaultExchangeName);
        t2.setBase("B");
        t2.setTarget("Q");
        Ticker t3 = new Ticker(ExchangeName.BINANCE);
        t3.setBase("B");
        t3.setTarget("T");
        Set<Ticker> tickers = (Set<Ticker>) ReflectionTestUtils.getField(tickerContainer, "bigONETickers");
        tickers.addAll(List.of(t1, t2, t3));

        then(tickerContainer.getTicker(defaultExchangeName, "C", "T")).isEqualTo(Optional.of(t1));
    }

    @Test
    void addTickers_emptyTickersCollection() {
        tickerContainer.addTickers(defaultExchangeName, Collections.emptyList());
        assertThat(tickerContainer.getAll()).hasSize(1);
    }

    @Test
    void addTickers_exchangeNameMismatch() {
        Ticker newTicker = new Ticker(ExchangeName.BINANCE);
        newTicker.setBase("B");
        newTicker.setTarget("T");

        thenThrownBy(() -> tickerContainer.addTickers(defaultExchangeName, List.of(newTicker)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addTickers() {
        Ticker newTicker = new Ticker(defaultExchangeName);
        newTicker.setBase("B");
        newTicker.setTarget("T");
        newTicker.setPriceAsk(BigDecimal.valueOf(0.95d));
        newTicker.setPriceBid(BigDecimal.valueOf(0.9d));
        BigDecimal closePrice = BigDecimal.valueOf(0.91d);
        newTicker.setClosePrice(closePrice);
        Ticker newTicker2 = new Ticker(defaultExchangeName);
        newTicker2.setBase("C");
        newTicker2.setTarget("T");

        tickerContainer.addTickers(defaultExchangeName, List.of(newTicker, newTicker2));

        Set<Ticker> tickers = tickerContainer.getTickers(defaultExchangeName);
        assertThat(tickers).hasSize(2);
        Ticker addedTicker = tickers.stream().filter(ticker -> ticker.getBase().equals("B")).findFirst().orElse(null);
        assertNotNull(addedTicker);
        assertNotNull(addedTicker.getPreviousPriceBid());
        assertEquals(closePrice, addedTicker.getClosePrice());
    }
}