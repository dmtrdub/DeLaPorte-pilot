package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ScheduledMarketDataRetriever implements InitializingBean {

    @Value("${market_data_api_limit_per_min}")
    private int apiLimitPerMin;
    @Value("${market_data_retrieve_interval_sec}")
    private String optionalRetrieveIntervalSec;

    private int intervalSec;

    private final ExchangeService exchangeService;
    private final MarketDataService marketDataService;

    @Autowired
    public ScheduledMarketDataRetriever(ExchangeService exchangeService, MarketDataService marketDataService) {
        this.exchangeService = exchangeService;
        this.marketDataService = marketDataService;
    }

    @Override
    public void afterPropertiesSet() {
        start();
    }

    private void start() {
        Set<Exchange> exchanges = exchangeService.findAll();
        int exchangesCount = exchanges.size();
        setIntervalSec(exchangesCount);
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(exchangesCount);
        taskScheduler.setThreadNamePrefix("exch_fetcher-");
        taskScheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
        taskScheduler.initialize();
        for (Exchange exchange : exchanges) {
            taskScheduler.scheduleWithFixedDelay(() -> marketDataService.fetchMarketData(exchange), Duration.ofSeconds(intervalSec));
        }
    }

    private int calculateTimeIntervalSec(int exchangesCount) {
        return (60 * exchangesCount) / apiLimitPerMin;
    }

    private void setIntervalSec(int exchangesCount) {
        try {
            intervalSec = Integer.parseInt(optionalRetrieveIntervalSec);
            if (intervalSec <= 0) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            intervalSec = calculateTimeIntervalSec(exchangesCount);
        }
    }
}
