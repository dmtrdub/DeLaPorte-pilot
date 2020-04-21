package my.dub.dlp_pilot.service.impl;

import com.litesoftwares.coingecko.domain.Exchanges.ExchangesTickersById;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.repository.TickerRepository;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static my.dub.dlp_pilot.util.NumberUtils.*;

@Slf4j
@Service
public class TickerServiceImpl implements TickerService {

    private final CoinGeckoApiClientImpl marketDataApiClient;
    private final TickerRepository tickerRepository;
    private final TickerContainer tickerContainer;

    @Autowired
    public TickerServiceImpl(CoinGeckoApiClientImpl marketDataApiClient, TickerRepository tickerRepository,
                             TickerContainer tickerContainer) {
        this.marketDataApiClient = marketDataApiClient;
        this.tickerRepository = tickerRepository;
        this.tickerContainer = tickerContainer;
    }

    @Transactional
    public void save(Collection<Ticker> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        tickerRepository.saveAll(tickers);
    }

    @Transactional
    public void saveAndUpdateLocal(Collection<Ticker> tickers, long exchangeId) {
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        tickerRepository.saveAll(tickers);
        updateLocalContainer(exchangeId, tickers);
    }

    public void fetchMarketData(Exchange exchange) {
        Assert.notNull(exchange, "Exchange cannot be null!");
        try {
            List<ExchangesTickersById> tickersByIds = new ArrayList<>();
            for (int page = 1; page < exchange.getPagesRequestPerMin() + 1; page++) {
                //TODO: add handling of client errors
                tickersByIds.add(marketDataApiClient.getExchangesTickersById(exchange.getApiName(), null, page, null));
            }
            Long exchangeId = exchange.getId();
            List<Ticker> tickers = getUniqueTickers(exchangeId, convertToTickers(exchange, tickersByIds));
            saveAndUpdateLocal(tickers, exchangeId);
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private List<Ticker> getUniqueTickers(Long exchangeId, List<Ticker> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return Collections.emptyList();
        }
        List<Ticker> cachedTickers = tickerContainer.getTickers(exchangeId);
        if (CollectionUtils.isEmpty(cachedTickers)) {
            tickerContainer.addTickers(exchangeId, tickers);
            return tickers;
        }
        List<Ticker> result = new ArrayList<>(tickers);
        result.removeAll(cachedTickers);
        return result;
    }

    private void updateLocalContainer(Long exchangeId, Collection<Ticker> tickers) {
        tickerContainer.updateTickers(exchangeId, tickers);
    }


    private List<Ticker> convertToTickers(Exchange exchange, List<ExchangesTickersById> tickersById) {
        List<com.litesoftwares.coingecko.domain.Shared.Ticker> rawTickers =
                tickersById.stream().flatMap(t -> t.getTickers().stream()).collect(Collectors.toList());
        return rawTickers.parallelStream().map(externalTicker -> {
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
