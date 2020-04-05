package my.dub.dlp_pilot.service.impl;

import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.repository.ExchangeRepository;
import my.dub.dlp_pilot.repository.TickerRepository;
import my.dub.dlp_pilot.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
// TODO: remove when init methods will be added
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@Transactional
public class MarketDataServiceImpl implements MarketDataService {
    private final CoinGeckoApiClientImpl marketDataApiClient;
    private final TickerRepository tickRepository;
    private final ExchangeRepository exchangeRepository;

    @Autowired
    public MarketDataServiceImpl(CoinGeckoApiClientImpl marketDataApiClient, TickerRepository tickerRepository, ExchangeRepository exchangeRepository) {
        this.marketDataApiClient = marketDataApiClient;
        this.tickRepository = tickerRepository;
        this.exchangeRepository = exchangeRepository;
    }

    public void fetchMarketData() {
        List<Ticker> tickers = (List<Ticker>) tickRepository.findAll();
        log.info("Total tickers count: " + tickers.size());
        Ticker ticker = new Ticker();
        ticker.setBase("base");
        ticker.setTarget("target");
        ticker.setExchange(tickers.get(0).getExchange());
        ticker.setPrice(1.0);
        ticker.setPriceBtc(1.0);
        ticker.setPriceUsd(1.0);
        ticker.setVolume(100.0);
        ticker.setVolumeBtc(100.0);
        ticker.setVolumeUsd(100.0);
        ticker.setSpreadPercentage(0.5);
        ticker.setTime(LocalDateTime.now());
        tickRepository.save(ticker);
        log.info("Total tickers count after save: " + ((List<Ticker>) tickRepository.findAll()).size());
    }
}
