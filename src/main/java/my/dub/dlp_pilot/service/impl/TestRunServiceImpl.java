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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.LastBar;
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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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

    private final Map<ExchangeName, AtomicInteger> loadPairsIndexMap = new ConcurrentHashMap<>();
    private final Map<ExchangeName, ZonedDateTime> preloadPairsDateTimeMap = new ConcurrentHashMap<>();
    private final Map<ExchangeName, List<LastBar>> refreshLoadLastBars = new ConcurrentHashMap<>();

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
        exchanges.forEach(exchange -> loadPairsIndexMap.put(exchange.getName(), new AtomicInteger()));
        clientService.loadAllSymbolPairs(loadPairsIndexMap.keySet());
    }

    @Override
    public boolean runPreload(Exchange exchange) {
        ExchangeName name = exchange.getName();
        boolean isDescPreload = Boolean.FALSE.equals(exchange.getAscendingPreload());
        TimeFrame dataCaptureTimeFrame = parameters.getDataCaptureTimeFrame();
        LocalDateTime fallbackStartTime = isDescPreload
                ? currentTestRun.getStartTime()
                : currentTestRun.getPreloadStartTime().minus(dataCaptureTimeFrame.getDuration());
        ZonedDateTime startTime = Optional.ofNullable(preloadPairsDateTimeMap.get(name))
                .orElse(DateUtils.toZonedDateTime(fallbackStartTime));
        ZonedDateTime endTime = DateUtils
                .toZonedDateTime(isDescPreload ? currentTestRun.getPreloadStartTime() : currentTestRun.getStartTime());
        AtomicInteger atomicSymbolPairIndex = loadPairsIndexMap.get(name);
        List<Bar> bars =
                clientService.fetchBars(name, dataCaptureTimeFrame, startTime, atomicSymbolPairIndex.get(), endTime);
        if (bars.isEmpty()) {
            clientService.removeSymbolPair(name, atomicSymbolPairIndex.get());
            preloadPairsDateTimeMap.remove(name);
            return atomicSymbolPairIndex.get() >= clientService.getSymbolPairsCount(name);
        }
        // check to proceed to next symbol pair
        if ((isDescPreload && bars.stream().anyMatch(bar -> !bar.getOpenTime().isAfter(endTime))) || (!isDescPreload
                && bars.stream().anyMatch(bar -> !bar.getCloseTime().isBefore(endTime)))) {
            atomicSymbolPairIndex.incrementAndGet();
            preloadPairsDateTimeMap.remove(name);
        } else {
            ZonedDateTime preloadIterationEndDateTime;
            if (isDescPreload) {
                preloadIterationEndDateTime =
                        bars.stream().map(Bar::getOpenTime).min(ChronoZonedDateTime::compareTo).orElse(startTime);
            } else {
                preloadIterationEndDateTime =
                        bars.stream().map(Bar::getCloseTime).max(ChronoZonedDateTime::compareTo).orElse(endTime);
            }
            if (DateUtils.isDurationLonger(isDescPreload ? endTime : preloadIterationEndDateTime,
                                           isDescPreload ? preloadIterationEndDateTime : endTime,
                                           dataCaptureTimeFrame.getDuration())) {
                preloadPairsDateTimeMap.put(name, preloadIterationEndDateTime);
            } else {
                atomicSymbolPairIndex.incrementAndGet();
                preloadPairsDateTimeMap.remove(name);
            }
        }
        barService.save(bars, currentTestRun);
        return atomicSymbolPairIndex.get() >= clientService.getSymbolPairsCount(name);
    }

    @Override
    public void onPreloadComplete(ExchangeName exchangeName) {
        loadPairsIndexMap.get(exchangeName).set(0);
        refreshLoadLastBars.putIfAbsent(exchangeName, barService.loadExchangeLastBars(currentTestRun, exchangeName));
        log.info("{} has finished its preload task", exchangeName);
    }

    @Override
    public boolean runRefreshLoad(ExchangeName exchange) {
        AtomicInteger atomicSymbolPairIndex = loadPairsIndexMap.get(exchange);

        List<Bar> bars = clientService
                .fetchBars(exchange, parameters.getDataCaptureTimeFrame(), atomicSymbolPairIndex.get(),
                           refreshLoadLastBars.get(exchange));
        atomicSymbolPairIndex.incrementAndGet();
        if (!CollectionUtils.isEmpty(bars)) {
            barService.save(bars, currentTestRun);
        }
        return atomicSymbolPairIndex.get() >= clientService.getSymbolPairsCount(exchange);
    }

    @Override
    public void onRefreshLoadComplete(ExchangeName exchangeName, boolean isPreloadComplete) {
        if (isPreloadComplete) {
            //TODO: fix incorrect time
            List<BarAverage> barAverages = barService.loadBarAverages(currentTestRun, exchangeName);
            priceDifferenceService.updatePriceDifferences(barAverages);
            List<LastBar> lastBars = barAverages.stream()
                    .map(barAverage -> new LastBar(barAverage.getExchangeName(), barAverage.getBase(),
                                                   barAverage.getTarget(), barAverage.getCloseTime()))
                    .collect(Collectors.toList());
            refreshLoadLastBars.put(exchangeName, lastBars);
        } else {
            refreshLoadLastBars.put(exchangeName, barService.loadExchangeLastBars(currentTestRun, exchangeName));
        }
        loadPairsIndexMap.get(exchangeName).set(0);
        log.info("Refresh load finished for {} exchange", exchangeName.getFullName());
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
        updateTradeStartEndTime();
        preloadPairsDateTimeMap.clear();
        clientService.updateSymbolPairs();
    }

    @Override
    public void onExit() {
        if (parameters.isDeleteBarsOnExit()) {
            barService.deleteAll(currentTestRun);
        }
    }

    @Override
    public void updateResultFile(@NonNull String filePath) {
        currentTestRun.setPathToResultFile(filePath);
        currentTestRun = repository.save(currentTestRun);
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
        log.info("Trades part starting at: {}", DateUtils.formatDateTime(tickerStaleCheckEndDateTime));
        log.info("Test Run ending at: {}", DateUtils.formatDateTime(testRunEndDateTime));
    }

    @Override
    public TestRun getCurrentTestRun() {
        return currentTestRun;
    }

    @Override
    public boolean checkTradeStopped() {
        if (!tradeStop.get()) {
            tradeStop.set(!LocalDateTime.now().isBefore(tradeStopDateTime));
        }
        return tradeStop.get();
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
            log.error("Error when working with exit file {}! Details: {}", exitFile, e.getMessage());
        }
    }

    private boolean checkTickerStaleCheckEnd() {
        if (!tickerStaleCheckEnd.get()) {
            tickerStaleCheckEnd.set(!LocalDateTime.now().isBefore(tickerStaleCheckEndDateTime));
        }
        return tickerStaleCheckEnd.get();
    }
}
