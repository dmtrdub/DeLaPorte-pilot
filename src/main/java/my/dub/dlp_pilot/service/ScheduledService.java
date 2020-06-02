package my.dub.dlp_pilot.service;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.Exchange;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${init_trade_delay_ms}")
    private long initDelayMs;

    @Autowired
    public ScheduledService(ExchangeService exchangeService, TickerService tickerService,
                            TradeService tradeService) {
        this.exchangeService = exchangeService;
        this.tickerService = tickerService;
        this.tradeService = tradeService;
    }

    @Override
    public void afterPropertiesSet() {
        start();
    }

    private void start() {
        Set<Exchange> exchanges = exchangeService.findAll();
        if (CollectionUtils.isEmpty(exchanges)) {
            throw new IllegalArgumentException("There are no exchanges to work with!");
        }
        int exchangesCount = exchanges.size();
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(exchangesCount);
        taskScheduler.setThreadNamePrefix("scheduled-");
        //FIXME: disabled for debugging purposes
        //taskScheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
        taskScheduler.initialize();
        exchanges.forEach(exchange -> {
            int delayMillis = calculateFixedDelayInMillis(exchange);
            log.info("Fixed Operation delay set to {} ms for {} exchange", delayMillis, exchange.getFullName());
            taskScheduler.scheduleWithFixedDelay(() -> run(exchange), Instant.now().plusMillis(initDelayMs),
                                                 Duration.ofMillis(delayMillis));
        });
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
