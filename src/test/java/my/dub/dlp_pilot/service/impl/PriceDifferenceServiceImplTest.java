package my.dub.dlp_pilot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.PriceDifference;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
class PriceDifferenceServiceImplTest {

    @Mock
    private TickerService tickerService;
    @Mock
    private TradeService tradeService;

    @InjectMocks
    private PriceDifferenceServiceImpl service;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createPriceDifferences() {
        BarAverage bA1 = new BarAverage(ExchangeName.BINANCE, "B1", "T1", Instant.now().minusSeconds(20), 10d);
        BarAverage bA2 = new BarAverage(ExchangeName.EXMO, "B1", "T1", Instant.now().minusSeconds(15), 9d);
        BarAverage bA3 = new BarAverage(ExchangeName.BITFINEX, "B1", "T1", Instant.now().minusSeconds(10), 8d);
        BarAverage bA4 = new BarAverage(ExchangeName.BINANCE, "B2", "T1", Instant.now().minusSeconds(19), 7d);
        BarAverage bA5 = new BarAverage(ExchangeName.BIGONE, "B2", "T2", Instant.now().minusSeconds(5), 6d);
        BarAverage bA6 = new BarAverage(ExchangeName.EXMO, "B2", "T2", Instant.now().minusSeconds(30), 5d);
        BarAverage bA7 = new BarAverage(ExchangeName.BIGONE, "B1", "T2", Instant.now().minusSeconds(30), 4d);
        BarAverage bA8 = new BarAverage(ExchangeName.BINANCE, "B1", "T1", Instant.now().minusSeconds(1), 3d);
        BarAverage bA9 = new BarAverage(ExchangeName.BITFINEX, "B3", "T3", Instant.now().minusSeconds(12), 11d);
        BarAverage bA10 = new BarAverage(ExchangeName.HUOBI, "B3", "T2", Instant.now(), 15d);

        service.createPriceDifferences(List.of(bA1, bA2, bA3, bA4, bA5, bA6, bA7, bA8, bA9, bA10));
        Set<PriceDifference> resultSet =
                (Set<PriceDifference>) ReflectionTestUtils.getField(service, "priceDifferences");
        assertThat(resultSet).hasSize(4);
        assertThat(resultSet.stream().filter(priceDifference -> priceDifference.isSimilar(bA5)).count()).isEqualTo(1);
    }

    @Test
    void updatePriceDifferences() {
        PriceDifference priceDifference =
                new PriceDifference("B1", "T1", ExchangeName.BINANCE, BigDecimal.ONE, ExchangeName.EXMO,
                                    BigDecimal.ZERO);
        PriceDifference priceDifference2 =
                new PriceDifference("B2", "T2", ExchangeName.BINANCE, BigDecimal.ONE, ExchangeName.EXMO,
                                    BigDecimal.ZERO);
        Set<PriceDifference> resultSet =
                (Set<PriceDifference>) ReflectionTestUtils.getField(service, "priceDifferences");
        resultSet.addAll(List.of(priceDifference, priceDifference2));
        BarAverage bA1 = new BarAverage(ExchangeName.BINANCE, "B1", "T1", Instant.now().minusSeconds(20), 100d);
        BarAverage bA2 = new BarAverage(ExchangeName.EXMO, "B1", "T1", Instant.now().minusSeconds(15), 150d);

        service.updatePriceDifferences(List.of(bA1, bA2));
        PriceDifference updatedPD = resultSet.stream().filter(priceDifference::equals).findFirst().orElseThrow();
        assertThat(updatedPD).matches(pD -> pD.getExchange1Average().equals(bA1.getAveragePrice()))
                .matches(pD -> pD.getExchange2Average().equals(bA2.getAveragePrice()));
    }

    @Test
    void handlePriceDifference() {
        Ticker ticker1 = createTicker("B", "T", ExchangeName.BINANCE);
        Ticker ticker2 = createTicker("B", "T", ExchangeName.BITMAX);
        ticker2.setClosePrice(BigDecimal.valueOf(3d));
        ticker2.setPriceAsk(BigDecimal.valueOf(3.5d));
        ticker2.setPriceBid(BigDecimal.valueOf(2.5d));
        Ticker ticker3 = createTicker("B2", "T", ExchangeName.GATE);
        when(tickerService.getAllTickers()).thenReturn(new HashSet<>(Set.of(ticker1, ticker2, ticker3)));
        when(tickerService.getTickers(ExchangeName.BINANCE)).thenReturn(new HashSet<>(Set.of(ticker1)));
        when(tickerService.findValidEquivalentTickerFromSet(eq(ticker2),
                                                            argThat((Set<Ticker> tSet) -> tSet.contains(ticker1))))
                .thenReturn(Optional.of(ticker1));
        Set<PriceDifference> resultSet =
                (Set<PriceDifference>) ReflectionTestUtils.getField(service, "priceDifferences");
        BigDecimal binanceAvg = BigDecimal.valueOf(0.8);
        BigDecimal bitmaxAvg = BigDecimal.valueOf(0.1);
        resultSet.add(new PriceDifference("B", "T", ExchangeName.BINANCE, binanceAvg, ExchangeName.BITMAX, bitmaxAvg));
        resultSet.add(new PriceDifference("B2", "T2", ExchangeName.BINANCE, BigDecimal.ONE, ExchangeName.EXMO,
                                          BigDecimal.ZERO));

        service.handlePriceDifference(ExchangeName.BINANCE, new TestRun());
        verify(tradeService)
                .checkTradeOpen(eq(ticker1), eq(ticker2), eq(binanceAvg.subtract(bitmaxAvg)), any(TestRun.class));
    }

    private Ticker createTicker(String base, String target, ExchangeName exchangeName) {
        Ticker ticker = new Ticker(exchangeName);
        ticker.setBase(base);
        ticker.setTarget(target);
        ticker.setClosePrice(BigDecimal.valueOf(5d));
        ticker.setPriceAsk(BigDecimal.valueOf(5.5d));
        ticker.setPriceBid(BigDecimal.valueOf(4.5d));
        return ticker;
    }
}