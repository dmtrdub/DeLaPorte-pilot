package my.dub.dlp_pilot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.TradeRepository;
import my.dub.dlp_pilot.repository.container.TradeContainer;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.TickerService;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class TradeServiceImplTest {

    @Mock
    private TradeRepository repository;
    @Mock
    private TradeContainer tradeContainer;
    @Mock
    private TickerService tickerService;
    @Mock
    private ExchangeService exchangeService;
    @Mock
    private ParametersHolder parameters;

    @InjectMocks
    private TradeServiceImpl service;

    private String base = "B";
    private String target = "T";
    private ExchangeName exchangeShort = ExchangeName.BINANCE;
    private ExchangeName exchangeLong = ExchangeName.BITFINEX;
    private Ticker tickerShort;
    private Ticker tickerLong;

    @BeforeEach
    void setUp() {
        tickerShort = createTicker(exchangeShort, base, target, 6.5d, 6d);
        tickerLong = createTicker(exchangeLong, base, target, 5d, 4.5d);
    }

    @Test
    void checkTradeOpen_invalidCurrentPercentageDiff() {
        when(tickerService.checkStale(eq(tickerShort), eq(tickerLong), any())).thenReturn(false);
        ExchangeName exchangeNameShort = tickerShort.getExchangeName();
        ExchangeName exchangeNameLong = tickerLong.getExchangeName();
        String base = tickerLong.getBase();
        String target = tickerLong.getTarget();
        when(tradeContainer.isSimilarPresent(eq(base), eq(target), eq(exchangeNameShort), eq(exchangeNameLong)))
                .thenReturn(false);
        when(exchangeService.isExchangeFaulty(any(ExchangeName.class))).thenReturn(false);
        when(tradeContainer.checkDetrimentalRecord(eq(exchangeNameShort), eq(exchangeNameLong), eq(base), eq(target)))
                .thenReturn(false);
        when(parameters.getParallelTradesNumber()).thenReturn(5);
        when(tradeContainer.tradesCount(eq(exchangeNameShort), eq(exchangeNameLong))).thenReturn(Pair.of(4L, 3L));
        tickerLong.setPriceAsk(BigDecimal.ONE);
        when(parameters.getEntryMinPercentageDiff()).thenReturn(BigDecimal.ZERO);
        when(parameters.getEntryMaxPercentageDiff()).thenReturn(BigDecimal.ONE);

        service.checkTradeOpen(tickerShort, tickerLong, BigDecimal.ONE, new TestRun());
        verify(tradeContainer, never()).addTrade(any(Trade.class));
    }

    @Test
    void handleTrades_openNotProfitable() {
        when(tickerService.checkStale(eq(tickerShort), eq(tickerLong), any())).thenReturn(false);
        when(tradeContainer.isSimilarPresent(eq(base), eq(target), eq(exchangeShort), eq(exchangeLong)))
                .thenReturn(false);
        when(exchangeService.isExchangeFaulty(any(ExchangeName.class))).thenReturn(false);
        when(tradeContainer.checkDetrimentalRecord(eq(exchangeShort), eq(exchangeLong), eq(base), eq(target)))
                .thenReturn(false);
        when(parameters.getParallelTradesNumber()).thenReturn(5);
        when(tradeContainer.tradesCount(eq(exchangeShort), eq(exchangeLong))).thenReturn(Pair.of(4L, 3L));
        tickerLong.setPriceAsk(BigDecimal.ONE);
        when(parameters.getEntryMinPercentageDiff()).thenReturn(BigDecimal.ZERO);
        when(parameters.getEntryMaxPercentageDiff()).thenReturn(BigDecimal.valueOf(20));
        BigDecimal entryAmount = BigDecimal.valueOf(100);
        when(parameters.getEntryAmount()).thenReturn(entryAmount);
        when(exchangeService.getTotalExpenses(any(ExchangeName.class), eq(entryAmount)))
                .thenReturn(BigDecimal.valueOf(5));
        when(parameters.getEntryProfitPercentage()).thenReturn(BigDecimal.valueOf(10));

        service.checkTradeOpen(tickerShort, tickerLong, BigDecimal.ONE, new TestRun());
        verify(tradeContainer, never()).addTrade(any(Trade.class));
    }

    @Test
    void handleTrades() {
        Trade trade1 = createTrade(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
        Trade trade2 = createTrade(BigDecimal.valueOf(8), BigDecimal.valueOf(3));
        Trade trade3 = createTrade(BigDecimal.valueOf(6), BigDecimal.valueOf(6));
        trade3.setStartTime(Instant.now().minus(12, ChronoUnit.HOURS));
        when(tradeContainer.getTrades(exchangeShort)).thenReturn(new HashSet<>(Set.of(trade1, trade2, trade3)));
        when(tickerService.getTickerWithRetry(eq(exchangeShort), eq(base), eq(target))).thenReturn(tickerShort);
        when(tickerService.getTickerWithRetry(eq(exchangeLong), eq(base), eq(target))).thenReturn(tickerLong);
        when(parameters.getTradeTimeoutDuration()).thenReturn(Duration.of(6, ChronoUnit.HOURS));
        when(parameters.getSuspenseAfterDetrimentalTradeDuration()).thenReturn(Duration.of(2, ChronoUnit.HOURS));
        BigDecimal entryAmount = BigDecimal.valueOf(100);
        when(parameters.getEntryAmount()).thenReturn(entryAmount);
        when(exchangeService.getTotalExpenses(any(ExchangeName.class), eq(entryAmount)))
                .thenReturn(BigDecimal.valueOf(5));
        when(parameters.getProfitPercentageOnExitSum(anyLong())).thenReturn(BigDecimal.valueOf(10));
        when(parameters.getDetrimentAmountPercentage()).thenReturn(BigDecimal.valueOf(10));
        when(parameters.getDetrimentalCloseOnMaxPnlDiffPercentage()).thenReturn(BigDecimal.valueOf(300));
        when(tradeContainer.isSimilarPresent(any(Trade.class))).thenReturn(true);
        when(repository.checkSimilarExists(eq(base), eq(target), eq(exchangeShort), eq(exchangeLong),
                                           any(BigDecimal.class), any(BigDecimal.class), any(TradeResultType.class)))
                .thenReturn(false);
        when(tradeContainer.remove(anyLong())).thenReturn(true);

        service.handleTrades(exchangeShort);
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(repository, times(3)).save(tradeCaptor.capture());
        verify(tradeContainer, times(3)).remove(anyLong());
        verify(tradeContainer).addDetrimentalRecord(eq(exchangeShort), eq(exchangeLong), eq(base), eq(target),
                                                    argThat((Instant argument) -> argument.isAfter(Instant.now())));
        List<Trade> closedTrades = tradeCaptor.getAllValues();
        assertThat(closedTrades).hasSize(3).matches(trades -> trades.stream().allMatch(
                trade -> trade.getEndTime() != null && trade.getPositionShort().getClosePrice()
                        .equals(tickerShort.getPriceAsk()) && trade.getPositionLong().getClosePrice()
                        .equals(tickerLong.getPriceBid())))
                .matches(trades -> trades.stream().map(Trade::getResultType).distinct().count() == 3);
    }

    private Ticker createTicker(ExchangeName exchangeName, String base, String target, double priceAsk,
            double priceBid) {
        Ticker ticker = new Ticker(exchangeName);
        ticker.setBase(base);
        ticker.setTarget(target);
        ticker.setPriceAsk(BigDecimal.valueOf(priceAsk));
        ticker.setPriceBid(BigDecimal.valueOf(priceBid));
        return ticker;
    }

    private Trade createTrade(BigDecimal openPriceShort, BigDecimal openPriceLong) {
        Trade trade = new Trade();
        trade.setStartTime(Instant.now().minusSeconds(300));
        trade.setLocalId(RandomUtils.nextLong(0, 200));
        trade.setBase(base);
        trade.setTarget(target);
        trade.setFixedExpensesUsd(BigDecimal.valueOf(0.05));

        Position posShort = new Position();
        posShort.setSide(PositionSide.SHORT);
        Exchange shortExchange = new Exchange();
        shortExchange.setName(exchangeShort);
        shortExchange.setTakerFeePercentage(BigDecimal.valueOf(0.02));
        posShort.setExchange(shortExchange);
        posShort.setOpenPrice(openPriceShort);

        Position posLong = new Position();
        posLong.setSide(PositionSide.SHORT);
        Exchange longExchange = new Exchange();
        longExchange.setName(exchangeLong);
        longExchange.setTakerFeePercentage(BigDecimal.valueOf(0.02));
        posLong.setExchange(longExchange);
        posLong.setOpenPrice(openPriceLong);

        trade.setPositions(posShort, posLong);
        return trade;
    }
}