package my.dub.dlp_pilot;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.AppConfig;
import my.dub.dlp_pilot.service.MarketDataService;
import my.dub.dlp_pilot.service.impl.MarketDataServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("Init");
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        MarketDataServiceImpl marketDataServiceImpl = (MarketDataServiceImpl) context.getBean(MarketDataService.class);
        marketDataServiceImpl.fetchMarketData();
        context.registerShutdownHook();
    }
}
