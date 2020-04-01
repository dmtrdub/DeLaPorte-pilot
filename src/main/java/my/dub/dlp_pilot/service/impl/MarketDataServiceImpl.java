package my.dub.dlp_pilot.service.impl;

import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MarketDataServiceImpl implements MarketDataService {
    private final CoinGeckoApiClientImpl coinGeckoApiClient;

    @Autowired
    public MarketDataServiceImpl(CoinGeckoApiClientImpl coinGeckoApiClient) {
        this.coinGeckoApiClient = coinGeckoApiClient;
    }

    public void fetchMarketData() {
        //ExchangesTickersById hitbtcExchangeTicker = coinGeckoApiClient.getExchangesTickersById("hitbtc");
        log.info("success!");
    }
}
