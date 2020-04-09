package my.dub.dlp_pilot.service.impl;

import com.litesoftwares.coingecko.domain.Exchanges.ExchangesTickersById;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.repository.TickerRepository;
import my.dub.dlp_pilot.service.MarketDataService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static my.dub.dlp_pilot.util.NumberUtils.*;

@Slf4j
@Service
@Transactional
public class MarketDataServiceImpl implements MarketDataService {

    private final CoinGeckoApiClientImpl marketDataApiClient;
    private final TickerRepository tickRepository;

    @Autowired
    public MarketDataServiceImpl(CoinGeckoApiClientImpl marketDataApiClient, TickerRepository tickerRepository) {
        this.marketDataApiClient = marketDataApiClient;
        this.tickRepository = tickerRepository;
    }

    public void fetchMarketData(Exchange exchange) {
        Assert.notNull(exchange, "Exchange cannot be null!");
        try {
            ExchangesTickersById tickersById = marketDataApiClient.getExchangesTickersById(exchange.getApiName());
            List<Ticker> tickers = convertToTicker(exchange, tickersById);
            tickRepository.saveAll(tickers);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private List<Ticker> convertToTicker(Exchange exchange, ExchangesTickersById tickersById) {
        return tickersById.getTickers().parallelStream().map(externalTicker -> {
            Ticker ticker = new Ticker();
            ticker.setExchange(exchange);
            ticker.setBase(externalTicker.getBase());
            ticker.setTarget(externalTicker.getTarget());
            ticker.setVolume(getVolumeDecimal(externalTicker.getVolume()));
            Map<String, String> convertedVolume = externalTicker.getConvertedVolume();
            ticker.setVolumeBtc(getVolumeDecimal(convertedVolume.get("btc")));
            ticker.setVolumeUsd(getVolumeDecimal(convertedVolume.get("usd")));
            ticker.setPrice(getPriceDecimal(externalTicker.getLast()));
            Map<String, String> convertedPrice = externalTicker.getConvertedLast();
            ticker.setPriceBtc(getPriceDecimal(convertedPrice.get("btc")));
            ticker.setPriceUsd(getPriceDecimal(convertedPrice.get("usd")));
            ticker.setSpreadPercentage(getPercentageDecimal(externalTicker.getBidAskSpreadPercentage()));
            ticker.setTime(DateUtils.getUTCLocalDateTimeFromString(externalTicker.getTimestamp()));
            ticker.setAnomaly(externalTicker.isAnomaly());
            ticker.setStale(externalTicker.isStale());
            return ticker;
        }).collect(Collectors.toList());
    }
}
