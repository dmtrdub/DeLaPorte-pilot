package my.dub.dlp_pilot.service.impl;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.*;
import my.dub.dlp_pilot.repository.TickerRepository;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TickerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
public class TickerServiceImpl implements TickerService {

    private final TickerRepository tickerRepository;
    private final TickerContainer tickerContainer;

    @Autowired
    public TickerServiceImpl(TickerRepository tickerRepository,
                             TickerContainer tickerContainer) {
        this.tickerRepository = tickerRepository;
        this.tickerContainer = tickerContainer;
    }

    @Transactional
    @Override
    public Iterable<Ticker> save(Collection<Ticker> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return Collections.emptyList();
        }
        return tickerRepository.saveAll(tickers);
    }

    @Transactional
    @Override
    public void saveAndUpdateLocal(Collection<Ticker> tickers, long exchangeId) {
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        tickerRepository.saveAll(tickers);
        tickerContainer.updateTickers(exchangeId, tickers);
    }

    @Override
    public void fetchMarketData(Exchange exchange) {
        Assert.notNull(exchange, "Exchange cannot be null!");
    }

    @Override
    public boolean isPairEquivalent(String baseOriginal, String targetOriginal, String baseCompared,
                                    String targetCompared) {
        if (StringUtils.isEmpty(baseOriginal) || StringUtils.isEmpty(targetOriginal) ||
                StringUtils.isEmpty(baseCompared) || StringUtils.isEmpty(targetCompared)) {
            return false;
        }
        List<String> baseSynonyms = getSymbolSynonyms(baseOriginal);
        List<String> targetSynonyms = getSymbolSynonyms(targetOriginal);
        return baseSynonyms.contains(baseCompared) && targetSynonyms.contains(targetCompared);
    }

    @Override
    public Set<Ticker> getTickers(Long exchangeId) {
        return tickerContainer.getTickers(exchangeId);
    }

    @Override
    public Optional<Ticker> getTicker(Long exchangeId, String base, String target) {
        return tickerContainer.getTicker(exchangeId, base, target);
    }

    @Override
    public Optional<Ticker> getTicker(Trade trade, PositionSide side) {
        return tickerContainer.getTicker(trade, side);
    }

    @Override
    public Optional<Ticker> getTicker(Position position) {
        return tickerContainer.getTicker(position);
    }

    @Override
    public Map<Long, Set<Ticker>> getExchangeIDTickersMap() {
        return tickerContainer.getExchangeIDTickersMap();
    }

    private List<String> getSymbolSynonyms(String symbol) {
        if (Constants.BITCOIN_SYMBOLS.contains(symbol)) {
            return Constants.BITCOIN_SYMBOLS;
        }
        if (Constants.USD_SYMBOLS.contains(symbol)) {
            return Constants.USD_SYMBOLS;
        }
        if (Constants.BITCOIN_CASH_SYMBOLS.contains(symbol)) {
            return Constants.BITCOIN_CASH_SYMBOLS;
        }
        if (Constants.BITCOIN_SV_SYMBOLS.contains(symbol)) {
            return Constants.BITCOIN_SV_SYMBOLS;
        }
        if (Constants.STELLAR_SYMBOLS.contains(symbol)) {
            return Constants.STELLAR_SYMBOLS;
        }
        return List.of(symbol);
    }

    private List<Ticker> getUniqueTickers(Long exchangeId, List<Ticker> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return Collections.emptyList();
        }
        Set<Ticker> cachedTickers = tickerContainer.getTickers(exchangeId);
        if (CollectionUtils.isEmpty(cachedTickers)) {
            tickerContainer.addTickers(exchangeId, tickers);
            return tickers;
        }
        List<Ticker> result = new ArrayList<>(tickers);
        result.removeAll(cachedTickers);
        return result;
    }
}
