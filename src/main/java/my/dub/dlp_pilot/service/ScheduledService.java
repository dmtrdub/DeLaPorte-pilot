package my.dub.dlp_pilot.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.service.impl.FileResultServiceImpl;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ScheduledService implements InitializingBean {

    private final ExchangeService exchangeService;
    private final TradeService tradeService;
    private final TestRunService testRunService;
    private final FileResultServiceImpl fileResultService;
    private final ParametersHolder parameters;

    private final Map<ExchangeName, ScheduledFuture<?>> taskSchedulerLoadFutures = new ConcurrentHashMap<>();
    private final Map<ExchangeName, LocalDateTime> loadStartDateTimes = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler loadTaskScheduler = new ThreadPoolTaskScheduler();

    private CountDownLatch preloadCountDownLatch;

    @Autowired
    public ScheduledService(ExchangeService exchangeService, TradeService tradeService, TestRunService testRunService,
            FileResultServiceImpl fileResultService, ParametersHolder parameters) {
        this.exchangeService = exchangeService;
        this.tradeService = tradeService;
        this.testRunService = testRunService;
        this.fileResultService = fileResultService;
        this.parameters = parameters;
    }

    @Override
    public void afterPropertiesSet() {
        init();
        startPreload();
        startTest();
    }

    private void init() {
        Set<Exchange> exchanges = exchangeService.findAll();
        if (exchanges == null || exchanges.size() < 2) {
            throw new IllegalArgumentException("There are no exchanges to work with!");
        }
        testRunService.init();
    }

    private void startPreload() {
        log.info("#### STARTING PRELOAD! ####");
        Set<Exchange> exchanges = exchangeService.findAll();
        int exchangesCount = exchanges.size();
        loadTaskScheduler.setPoolSize(exchangesCount);
        loadTaskScheduler.setThreadNamePrefix("load-");
        loadTaskScheduler.setErrorHandler(t -> {
            log.error("Unexpected error occurred in scheduled task.", t);
            log.warn("De La Porte is exiting prematurely!");
            shutdownNow(loadTaskScheduler);
            System.exit(-1);
        });
        loadTaskScheduler.initialize();
        preloadCountDownLatch = new CountDownLatch(exchangesCount);
        exchanges.forEach(exchange -> {
            int opIntervalMillis = calculatePreloadFixedDelayInMillis(exchange);
            log.info("Preload Operation interval set to {} ms for {} exchange", opIntervalMillis,
                     exchange.getFullName());
            taskSchedulerLoadFutures.put(exchange.getName(), loadTaskScheduler
                    .scheduleWithFixedDelay(runPreloadTask(exchange), Duration.ofMillis(opIntervalMillis)));
            loadStartDateTimes.put(exchange.getName(), LocalDateTime.now());
        });
        try {
            preloadCountDownLatch.await();
            log.info("#### PRELOAD COMPLETE! ####");
        } catch (InterruptedException e) {
            log.error("Main thread has been interrupted before completing the preload!");
            log.warn("De La Porte is exiting prematurely!");
            Thread.currentThread().interrupt();
            shutdownNow(loadTaskScheduler);
            System.exit(-1);
        }
        loadTaskScheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
    }

    private Runnable runPreloadTask(Exchange exchange) {
        return () -> {
            boolean finished = testRunService.runPreload(exchange);
            if (finished) {
                ExchangeName name = exchange.getName();
                taskSchedulerLoadFutures.remove(name).cancel(true);
                testRunService.onPreloadComplete(name);
                setNextLoadTask(exchange);
                preloadCountDownLatch.countDown();
            }
        };
    }

    private Runnable runRefreshLoadTask(Exchange exchange) {
        return () -> {
            ExchangeName exchangeName = exchange.getName();
            loadStartDateTimes.putIfAbsent(exchangeName, LocalDateTime.now());
            boolean finished = testRunService.runRefreshLoad(exchangeName);
            if (finished) {
                taskSchedulerLoadFutures.remove(exchangeName).cancel(true);
                testRunService.onRefreshLoadComplete(exchangeName, preloadCountDownLatch.getCount() == 0);
                setNextLoadTask(exchange);
            }
        };
    }

    private void setNextLoadTask(Exchange exchange) {
        int opIntervalMillis = calculateRefreshLoadFixedDelayInMillis(exchange);
        ExchangeName exchangeName = exchange.getName();
        Instant taskStartTime = DateUtils.toInstant(
                loadStartDateTimes.get(exchangeName).plus(parameters.getDataCaptureTimeFrame().getDuration()));
        taskSchedulerLoadFutures.put(exchangeName, loadTaskScheduler
                .scheduleWithFixedDelay(runRefreshLoadTask(exchange), taskStartTime,
                                        Duration.ofMillis(opIntervalMillis)));
        loadStartDateTimes.remove(exchangeName);
    }

    private void startTest() {
        testRunService.prepareRunTest();
        fileResultService.init();
        log.info("#### STARTING TRADES TEST! ####");
        Set<Exchange> exchanges = exchangeService.findAll();
        int exchangesCount = exchanges.size();
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(exchangesCount + 2);
        taskScheduler.setThreadNamePrefix("test_run-");
        taskScheduler.setErrorHandler(t -> {
            if (t instanceof TestRunEndException) {
                if (tradeService.isAllTradesClosed()) {
                    testRunService.onExit();
                    log.info("#### TEST RUN FINISHED! ####");
                    shutdownNow(taskScheduler);
                    shutdownNow(loadTaskScheduler);
                }
            } else {
                log.error("Unexpected error occurred in scheduled task", t);
            }
        });
        taskScheduler.initialize();
        exchanges.forEach(exchange -> {
            int opIntervalMillis = calculateTestRunFixedDelayInMillis(exchange);
            log.info("Fixed Operation interval set to {} ms for {} exchange", opIntervalMillis, exchange.getFullName());
            taskScheduler.scheduleWithFixedDelay(() -> testRunService.runTest(exchange.getName()),
                                                 Duration.ofMillis(opIntervalMillis));
        });
        taskScheduler.scheduleWithFixedDelay(fileResultService::write, Duration.ofSeconds(30));
        taskScheduler.scheduleWithFixedDelay(testRunService::checkExitFile, Duration.ofSeconds(60));
    }

    private void shutdownNow(ThreadPoolTaskScheduler taskScheduler) {
        taskScheduler.getScheduledExecutor().shutdownNow();
        taskScheduler.getScheduledThreadPoolExecutor().shutdownNow();
    }

    private int calculateTestRunFixedDelayInMillis(Exchange exchange) {
        return 60000 / exchange.getApiRequestsPerMin();
    }

    private int calculatePreloadFixedDelayInMillis(Exchange exchange) {
        return 60000 / exchange.getApiRequestsPerMinPreload();
    }

    private int calculateRefreshLoadFixedDelayInMillis(Exchange exchange) {
        return 60000 / (exchange.getApiRequestsPerMinPreload() - exchange.getApiRequestsPerMin());
    }
}
