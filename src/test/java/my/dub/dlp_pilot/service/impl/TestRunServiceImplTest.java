package my.dub.dlp_pilot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.LastBar;
import my.dub.dlp_pilot.repository.TestRunRepository;
import my.dub.dlp_pilot.service.BarService;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.PriceDifferenceService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.service.client.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
class TestRunServiceImplTest {
    private static final ExchangeName EXCHANGE_NAME = ExchangeName.BINANCE;

    @Mock
    private TestRunRepository repository;
    @Mock
    private ParametersHolder parameters;
    @Mock
    private ExchangeService exchangeService;
    @Mock
    private ClientService clientService;
    @Mock
    private TickerService tickerService;
    @Mock
    private BarService barService;
    @Mock
    private PriceDifferenceService priceDifferenceService;
    @Mock
    private TradeService tradeService;

    @InjectMocks
    private TestRunServiceImpl service;

    private Exchange exchange;
    private TimeFrame timeFrame;

    @BeforeEach
    void setUp() {
        when(parameters.getConfiguration()).thenReturn(Optional.of("CONFIG"));
        when(parameters.getDataCapturePeriodDuration()).thenReturn(Duration.of(30, ChronoUnit.MINUTES));
        timeFrame = TimeFrame.M5;
        when(parameters.getDataCaptureTimeFrame()).thenReturn(timeFrame);
        when(repository.save(any(TestRun.class))).thenAnswer(i -> i.getArguments()[0]);
        exchange = new Exchange();
        exchange.setName(EXCHANGE_NAME);
        exchange.setAscendingPreload(false);
        when(exchangeService.findAll()).thenReturn(Set.of(exchange));
        service.init();
    }

    @Test
    void runPreload_excludeSymbolPair() {
        when(clientService.fetchBarsPreload(eq(exchange.getName()), eq(timeFrame), any(Instant.class), eq(0),
                                            any(Instant.class))).thenReturn(new ArrayList<>());
        when(clientService.getSymbolPairsCount(eq(EXCHANGE_NAME))).thenReturn(5);

        assertThat(service.runPreload(exchange)).isFalse();
        verify(clientService).removeSymbolPair(eq(EXCHANGE_NAME), eq(0));
    }

    @Test
    void runPreload_proceedLoading() {
        long timeFrameDurationMinutes = timeFrame.getDuration().toMinutes();
        Bar bar1 = new Bar(EXCHANGE_NAME, "B", "T");
        bar1.setOpenTime(Instant.now().minus(timeFrameDurationMinutes, ChronoUnit.MINUTES));
        bar1.setCloseTime(Instant.now());
        Bar bar2 = new Bar(EXCHANGE_NAME, "B", "T");
        bar2.setOpenTime(Instant.now().minus(timeFrameDurationMinutes * 2, ChronoUnit.MINUTES));
        bar2.setCloseTime(Instant.now().minus(timeFrameDurationMinutes, ChronoUnit.MINUTES));
        when(clientService.fetchBarsPreload(eq(exchange.getName()), eq(timeFrame), any(Instant.class), eq(0),
                                            any(Instant.class))).thenReturn(new ArrayList<>(List.of(bar1, bar2)));
        Map<ExchangeName, Instant> preloadPairsDateTimeMap =
                (Map<ExchangeName, Instant>) ReflectionTestUtils.getField(service, "preloadPairsDateTimeMap");
        preloadPairsDateTimeMap.put(EXCHANGE_NAME, Instant.now());
        when(clientService.getSymbolPairsCount(eq(EXCHANGE_NAME))).thenReturn(0);

        assertThat(service.runPreload(exchange)).isTrue();
        assertThat(preloadPairsDateTimeMap).containsEntry(EXCHANGE_NAME, bar2.getOpenTime());
        verify(barService).save(argThat((Collection<Bar> bars) -> bars.contains(bar1)), any(TestRun.class));
    }

    @Test
    void onRefreshLoadComplete_preloadComplete() {
        BarAverage bA1 = new BarAverage(EXCHANGE_NAME, "B1", "T1", Instant.now().minusSeconds(20), 10d);
        BarAverage bA2 = new BarAverage(EXCHANGE_NAME, "B2", "T2", Instant.now().minusSeconds(15), 9d);
        BarAverage bA3 = new BarAverage(EXCHANGE_NAME, "B3", "T3", Instant.now().minusSeconds(10), 8d);
        when(barService.loadBarAverages(any(TestRun.class), eq(EXCHANGE_NAME)))
                .thenReturn(new ArrayList<>(List.of(bA1, bA2, bA3)));
        Map<ExchangeName, AtomicInteger> loadPairsIndexMap =
                (Map<ExchangeName, AtomicInteger>) ReflectionTestUtils.getField(service, "loadPairsIndexMap");
        loadPairsIndexMap.put(EXCHANGE_NAME, new AtomicInteger(1));

        service.onRefreshLoadComplete(EXCHANGE_NAME, true);
        Map<ExchangeName, List<LastBar>> refreshLoadLastBars =
                (Map<ExchangeName, List<LastBar>>) ReflectionTestUtils.getField(service, "refreshLoadLastBars");
        assertThat(refreshLoadLastBars.get(EXCHANGE_NAME)).hasSize(3).matches(lastBars -> lastBars.stream()
                .allMatch(bar -> EXCHANGE_NAME.equals(bar.getExchangeName()) && bar.getBase().startsWith("B")));
        assertThat(loadPairsIndexMap.get(EXCHANGE_NAME)).hasValue(0);
    }
}