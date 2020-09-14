package my.dub.dlp_pilot.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.service.client.ApiClient;
import my.dub.dlp_pilot.service.impl.FileResultServiceImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ScheduledService implements InitializingBean {

    private final ExchangeService exchangeService;
    private final TickerService tickerService;
    private final TradeService tradeService;
    private final PriceDifferenceService priceDifferenceService;
    private final ApiClient apiClient;
    private final TestRunService testRunService;
    private final FileResultServiceImpl fileResultService;

    private final ParametersHolder parameters;

    @Autowired
    public ScheduledService(ExchangeService exchangeService, TickerService tickerService, TradeService tradeService,
            PriceDifferenceService priceDifferenceService, ApiClient apiClient, TestRunService testRunService,
            FileResultServiceImpl fileResultService, ParametersHolder parameters) {
        this.exchangeService = exchangeService;
        this.tickerService = tickerService;
        this.tradeService = tradeService;
        this.priceDifferenceService = priceDifferenceService;
        this.apiClient = apiClient;
        this.testRunService = testRunService;
        this.fileResultService = fileResultService;
        this.parameters = parameters;
    }

    @Override
    public void afterPropertiesSet() {
        start();
    }

    private void start() {
        testRunService.createAndSave();
        fileResultService.init();
        Set<Exchange> exchanges = exchangeService.loadAll();
        if (CollectionUtils.isEmpty(exchanges)) {
            throw new IllegalArgumentException("There are no exchanges to work with!");
        }
        apiClient.initConnection(exchanges);
        int exchangesCount = exchanges.size();
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(exchangesCount + 2);
        taskScheduler.setThreadNamePrefix("exchange-");
        taskScheduler.setErrorHandler(t -> {
            if (t instanceof TestRunEndException) {
                if (tradeService.isAllTradesClosed()) {
                    log.info("De La Porte is exiting...");
                    taskScheduler.getScheduledExecutor().shutdownNow();
                    taskScheduler.getScheduledThreadPoolExecutor().shutdownNow();
                }
            } else {
                log.error("Unexpected error occurred in scheduled task", t);
            }
        });
        taskScheduler.initialize();
        long initDelayMillis = parameters.getInitDelayDuration().toMillis();
        exchanges.forEach(exchange -> {
            int opIntervalMillis = calculateFixedDelayInMillis(exchange);
            log.info("Fixed Operation interval set to {} ms for {} exchange", opIntervalMillis, exchange.getFullName());
            taskScheduler.scheduleWithFixedDelay(() -> run(exchange), Instant.now().plusMillis(
                    initDelayMillis), Duration.ofMillis(opIntervalMillis));
        });
        taskScheduler.scheduleWithFixedDelay(fileResultService::write,
                                             Instant.now().plusMillis(initDelayMillis* 2),
                                             Duration.ofSeconds(30));
        taskScheduler.scheduleWithFixedDelay(testRunService::checkExitFile,
                                             Instant.now().plusMillis(initDelayMillis * 4),
                                             Duration.ofSeconds(60));
    }

    private void run(Exchange exchange) {
        tickerService.fetchAndSave(exchange);
        ExchangeName exchangeName = exchange.getName();
        priceDifferenceService.handlePriceDifference(exchangeName);
        tradeService.handleTrades(exchangeName);
    }

    private int calculateFixedDelayInMillis(Exchange exchange) {
        return 60000 / exchange.getApiRequestsPerMin();
    }
}
