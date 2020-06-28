package my.dub.dlp_pilot.service;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.model.Exchange;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ScheduledService implements InitializingBean {

    private final ExchangeService exchangeService;
    private final TickerService tickerService;
    private final TradeService tradeService;
    private final TestRunService testRunService;
    private final FileResultService fileResultService;

    private final ParametersComponent parameters;

    @Autowired
    public ScheduledService(ExchangeService exchangeService, TickerService tickerService,
                            TradeService tradeService, TestRunService testRunService,
                            FileResultService fileResultService,
                            ParametersComponent parameters) {
        this.exchangeService = exchangeService;
        this.tickerService = tickerService;
        this.tradeService = tradeService;
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
        Set<Exchange> exchanges = exchangeService.findAll();
        if (CollectionUtils.isEmpty(exchanges)) {
            throw new IllegalArgumentException("There are no exchanges to work with!");
        }
        int exchangesCount = exchanges.size();
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(exchangesCount + 2);
        taskScheduler.setThreadNamePrefix("scheduled-");
        taskScheduler.setErrorHandler(t -> {
            if (t instanceof TestRunEndException) {
                if (tradeService.allTradesClosed()) {
                    log.info("De La Porte is exiting...");
                    taskScheduler.getScheduledExecutor().shutdownNow();
                    taskScheduler.getScheduledThreadPoolExecutor().shutdownNow();
                }
            } else {
                log.error("Unexpected error occurred in scheduled task", t);
            }
        });
        taskScheduler.initialize();
        exchanges.forEach(exchange -> {
            int delayMillis = calculateFixedDelayInMillis(exchange);
            log.info("Fixed Operation delay set to {} ms for {} exchange", delayMillis, exchange.getFullName());
            taskScheduler
                    .scheduleWithFixedDelay(() -> run(exchange), Instant.now().plusMillis(parameters.getInitDelayMs()),
                                            Duration.ofMillis(delayMillis));
        });
        taskScheduler.scheduleWithFixedDelay(fileResultService::write,
                                             Instant.now().plusMillis(parameters.getInitDelayMs() * 2),
                                             Duration.ofSeconds(30));
        taskScheduler.scheduleWithFixedDelay(testRunService::checkExitFile,
                                             Instant.now().plusMillis(parameters.getInitDelayMs() * 4),
                                             Duration.ofSeconds(60));
    }

    private void run(Exchange exchange) {
        tickerService.fetchAndSave(exchange);
        tradeService.searchForTrades(exchange);
        tradeService.handleTrades(exchange.getName());
    }

    private int calculateFixedDelayInMillis(Exchange exchange) {
        return 60000 / exchange.getApiRequestsPerMin();
    }
}
