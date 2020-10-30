package my.dub.dlp_pilot.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.BarAverage;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.repository.TestRunRepository;
import my.dub.dlp_pilot.service.BarService;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.PriceDifferenceService;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.service.client.ClientService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TestRunServiceImpl implements TestRunService {

    private final TestRunRepository repository;
    private final ParametersHolder parameters;
    private final ExchangeService exchangeService;
    private final ClientService clientService;
    private final TickerService tickerService;
    private final BarService barService;
    private final PriceDifferenceService priceDifferenceService;
    private final TradeService tradeService;

    private TestRun currentTestRun;

    private LocalDateTime tickerStaleCheckEndDateTime;
    private LocalDateTime tradeStopDateTime;
    private LocalDateTime testRunEndDateTime;

    private final AtomicBoolean tickerStaleCheckEnd = new AtomicBoolean();
    private final AtomicBoolean tradeStop = new AtomicBoolean();
    private final AtomicBoolean testRunEnd = new AtomicBoolean();

    private final Map<ExchangeName, AtomicInteger> loadedPairsMap = new ConcurrentHashMap<>();
    private final Map<ExchangeName, ZonedDateTime> loadedPairsEndDateTimeMap = new ConcurrentHashMap<>();

    @Autowired
    public TestRunServiceImpl(TestRunRepository repository, ParametersHolder parameters,
            ExchangeService exchangeService, ClientService clientService, TickerService tickerService,
            BarService barService, PriceDifferenceService priceDifferenceService, TradeService tradeService) {
        this.repository = repository;
        this.parameters = parameters;
        this.exchangeService = exchangeService;
        this.clientService = clientService;
        this.tickerService = tickerService;
        this.barService = barService;
        this.priceDifferenceService = priceDifferenceService;
        this.tradeService = tradeService;
    }

    @Override
    public void init() {
        createAndSave();
        Set<Exchange> exchanges = exchangeService.findAll();
        exchanges.forEach(exchange -> loadedPairsMap.put(exchange.getName(), new AtomicInteger()));
        clientService.loadAllSymbolPairs(loadedPairsMap.keySet());
    }

    @Override
    public boolean runPreload(Exchange exchange) {
        ExchangeName name = exchange.getName();
        boolean isDescPreload = Boolean.FALSE.equals(exchange.getAscendingPreload());
        ZonedDateTime startTime = Optional.ofNullable(loadedPairsEndDateTimeMap.get(name)).orElse(DateUtils
                                                                                                          .toZonedDateTime(
                                                                                                                  isDescPreload
                                                                                                                          ? currentTestRun
                                                                                                                          .getStartTime()
                                                                                                                          : currentTestRun
                                                                                                                                  .getPreloadStartTime()));
        ZonedDateTime endTime = DateUtils
                .toZonedDateTime(isDescPreload ? currentTestRun.getPreloadStartTime() : currentTestRun.getStartTime());
        AtomicInteger atomicSymbolPairIndex = loadedPairsMap.get(name);
        List<Bar> bars = clientService
                .fetchBars(name, parameters.getDataCaptureTimeFrame(), startTime, atomicSymbolPairIndex.get(), endTime);
        // check to proceed to next symbol pair
        if ((isDescPreload && bars.stream().anyMatch(bar -> !bar.getOpenTime().isAfter(endTime))) || (!isDescPreload
                && bars.stream().anyMatch(bar -> !bar.getCloseTime().isBefore(endTime))) || (bars.isEmpty()
                && atomicSymbolPairIndex.get() < clientService.getSymbolPairsCount(name))) {
            atomicSymbolPairIndex.incrementAndGet();
            loadedPairsEndDateTimeMap.remove(name);
        } else {
            ZonedDateTime preloadIterationEndDateTime;
            if (isDescPreload) {
                preloadIterationEndDateTime =
                        bars.stream().map(Bar::getOpenTime).min(ChronoZonedDateTime::compareTo).orElse(startTime);
            } else {
                preloadIterationEndDateTime =
                        bars.stream().map(Bar::getCloseTime).max(ChronoZonedDateTime::compareTo).orElse(endTime);
            }
            loadedPairsEndDateTimeMap.put(name, preloadIterationEndDateTime);
        }
        barService.save(bars, currentTestRun);
        return atomicSymbolPairIndex.get() >= clientService.getSymbolPairsCount(name);
    }

    @Override
    public void onPreloadComplete(ExchangeName exchangeName) {
        loadedPairsMap.get(exchangeName).set(0);
    }

    @Override
    public boolean runRefreshLoad(ExchangeName exchange) {
        AtomicInteger atomicSymbolPairIndex = loadedPairsMap.get(exchange);
        Bar bar = clientService
                .fetchSingleBar(exchange, parameters.getDataCaptureTimeFrame(), atomicSymbolPairIndex.get());
        atomicSymbolPairIndex.incrementAndGet();
        if (bar != null) {
            barService.save(List.of(bar), currentTestRun);
        }
        return atomicSymbolPairIndex.get() >= clientService.getSymbolPairsCount(exchange);
    }

    @Override
    public void onRefreshLoadComplete(ExchangeName exchangeName) {
        List<BarAverage> barAverages = barService.loadBarAverages(currentTestRun, exchangeName);
        priceDifferenceService.updatePriceDifferences(barAverages);
        loadedPairsMap.get(exchangeName).set(0);
    }

    @Override
    public void runTest(ExchangeName exchangeName) {
        if (!checkTestRunEnd()) {
            tickerService.fetchAndSave(exchangeName);
            if (checkTickerStaleCheckEnd() && !checkTradeStopped()) {
                priceDifferenceService.handlePriceDifference(exchangeName, currentTestRun);
                tradeService.handleTrades(exchangeName);
            }
        } else {
            tradeService.closeTrades(exchangeName, TradeResultType.TEST_RUN_END);
        }
    }

    @Override
    @Transactional
    public void prepareRunTest() {
        List<BarAverage> barAverages = barService.loadAllBarAverages(currentTestRun);
        priceDifferenceService.createPriceDifferences(barAverages);
        clientService.correctSymbolPairsAfterPreload(barAverages);
        updateTradeStartEndTime();
        loadedPairsEndDateTimeMap.clear();
    }

    @Override
    public void onExit() {
        if (parameters.isDeleteBarsOnExit()) {
            barService.deleteAll(currentTestRun);
        }
    }

    private void createAndSave() {
        TestRun testRun = new TestRun();
        String configuration = parameters.getConfiguration()
                .orElseThrow(() -> new IllegalArgumentException("Empty configuration passed to Test Run!"));
        testRun.setConfigParams(configuration);
        LocalDateTime startTime = LocalDateTime.now();
        testRun.setStartTime(startTime);
        testRun.setPreloadStartTime(startTime.minus(parameters.getDataCapturePeriodDuration()));
        currentTestRun = repository.save(testRun);
    }

    private void updateTradeStartEndTime() {
        LocalDateTime tickerStaleCheckStartDateTime = LocalDateTime.now();
        tickerStaleCheckEndDateTime =
                tickerStaleCheckStartDateTime.plus(parameters.getStaleIntervalDuration()).plusSeconds(1);
        currentTestRun.setTradesStartTime(tickerStaleCheckEndDateTime);
        tradeStopDateTime = tickerStaleCheckEndDateTime.plus(parameters.getTestRunDuration());
        testRunEndDateTime = tradeStopDateTime.plus(parameters.getExitDelayDuration());
        currentTestRun.setEndTime(testRunEndDateTime);
        currentTestRun = repository.save(currentTestRun);
        log.info("Trade starting at: {}", DateUtils.formatDateTime(tickerStaleCheckEndDateTime));
        log.info("Test Run ending at: {}", DateUtils.formatDateTime(testRunEndDateTime));
    }

    @Override
    public TestRun getCurrentTestRun() {
        return currentTestRun;
    }

    @Override
    public boolean checkTestRunEnd() {
        if (!testRunEnd.get()) {
            testRunEnd.set(!LocalDateTime.now().isBefore(testRunEndDateTime));
        }
        return testRunEnd.get();
    }

    @Override
    @Transactional
    public void checkExitFile() {
        if (checkTradeStopped()) {
            return;
        }
        String forcedExitFilePath = parameters.getForcedExitFilePath();
        if (StringUtils.isEmpty(forcedExitFilePath)) {
            return;
        }
        File exitFile = new File(forcedExitFilePath);
        if (!exitFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(exitFile.toPath());
            if (lines.stream().anyMatch(line -> line.contains(parameters.getExitCode()))) {
                tradeStopDateTime = LocalDateTime.now();
                testRunEndDateTime = tradeStopDateTime.plus(parameters.getExitDelayDuration());
                currentTestRun.setEndTime(testRunEndDateTime);
                repository.save(currentTestRun);
                log.info("Found force exit file {} containing valid exit code! Stopping trades now. "
                                 + "Test Run will end at {}", exitFile.getName(),
                         DateUtils.formatDateTime(testRunEndDateTime));
                Files.delete(exitFile.toPath());
            }
        } catch (IOException e) {
            log.error("Error when reading exit file {}! Details: {}", exitFile, e.getMessage());
        }
    }

    private boolean checkTickerStaleCheckEnd() {
        if (!tickerStaleCheckEnd.get()) {
            tickerStaleCheckEnd.set(!LocalDateTime.now().isBefore(tickerStaleCheckEndDateTime));
        }
        return tickerStaleCheckEnd.get();
    }

    private boolean checkTradeStopped() {
        if (!tradeStop.get()) {
            tradeStop.set(!LocalDateTime.now().isBefore(tradeStopDateTime));
        }
        return tradeStop.get();
    }
}
