package my.dub.dlp_pilot.service;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.Exchange;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

@Slf4j
@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ScheduledMarketDataRetriever implements InitializingBean {

    @Value("${market_data_api_limit_per_min}")
    private int apiLimitPerMin;

    @Value("${market_data_api_request_page_size}")
    private int apiRequestPageSize;

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
        if (CollectionUtils.isEmpty(exchanges)) {
            throw new IllegalArgumentException("There are no exchanges to work with!");
        }
        int totalRequestsPerMin = setPagesRequestPerMin(exchanges);
        log.info("Established retriever rate: {} calls/min. API limit: {} calls/min", totalRequestsPerMin,
                 apiLimitPerMin);
        int exchangesCount = exchanges.size();
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(exchangesCount);
        taskScheduler.setThreadNamePrefix("exchangeFetcher-");
        taskScheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
        taskScheduler.initialize();
        for (Exchange exchange : exchanges) {
            taskScheduler
                    .scheduleWithFixedDelay(() -> marketDataService.fetchMarketData(exchange), Duration.ofSeconds(60));
        }
    }

    private int setPagesRequestPerMin(Collection<Exchange> exchanges) {
        int totalRequests = 0;
        for (Exchange exchange : exchanges) {
            double pagesRequestCount = Math.ceil(exchange.getPairsCount() * 1.0d / apiRequestPageSize);
            totalRequests += pagesRequestCount;
            exchange.setPagesRequestPerMin((int) pagesRequestCount);
        }
        if (totalRequests > apiLimitPerMin) {
            int countSinglePageRequest = Math.toIntExact(
                    exchanges.stream().filter(exchange -> exchange.getPagesRequestPerMin() == 1).count());
            int pageRequests = (apiLimitPerMin - countSinglePageRequest) / (exchanges.size() - countSinglePageRequest);
            if (pageRequests < 1) {
                throw new IllegalStateException("Exclude some exchanges to comply with API limit!");
            }
            totalRequests = ((exchanges.size() - countSinglePageRequest) * pageRequests) + countSinglePageRequest;
            exchanges.forEach(exchange -> {
                if (exchange.getPagesRequestPerMin() != 1) {
                    exchange.setPagesRequestPerMin(pageRequests);
                }
            });
        }
        return totalRequests;
    }
}
