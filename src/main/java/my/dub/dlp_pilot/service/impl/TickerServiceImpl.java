package my.dub.dlp_pilot.service.impl;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.rest.NonexistentUSDPriceException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.client.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.client.RestClient;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class TickerServiceImpl implements TickerService, InitializingBean {

    private final TickerContainer tickerContainer;
    private final RestClient restClient;

    @Value("${trade_ticker_stale_difference_seconds}")
    private int staleDifferenceSeconds;

    @Value("${usd_price_fallback_exchange_name}")
    private String fallbackExchangeParam;

    private ExchangeName fallbackExchangeName;

    @Autowired
    public TickerServiceImpl(TickerContainer tickerContainer, RestClient restClient) {
        this.tickerContainer = tickerContainer;
        this.restClient = restClient;
    }

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
    }

    private void validateInputParams() {
        if (staleDifferenceSeconds <= 0) {
            throw new IllegalArgumentException("Stale difference for ticker cannot be <= 0 seconds!");
        }
        fallbackExchangeName = ExchangeName.valueOf(fallbackExchangeParam);
    }

    @Override
    public void fetchAndSave(Exchange exchange) {
        Set<Ticker> tickers = new HashSet<>(restClient.fetchTickers(exchange));
        save(exchange.getName(), tickers);
    }

    @Override
    public void save(ExchangeName exchangeName, Collection<Ticker> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        tickerContainer.addTickers(exchangeName, tickers);
    }

    public boolean checkStale(Ticker ticker) {
        if (!ticker.isStale() && DateUtils.durationSeconds(ticker.getDateTime()) > staleDifferenceSeconds) {
            ticker.setStale(true);
            return true;
        }
        return false;
    }

    @Override
    public Set<Ticker> getTickers(ExchangeName exchangeName) {
        return tickerContainer.getTickers(exchangeName, false);
    }

    @Override
    public Set<Ticker> getAllTickers() {
        return tickerContainer.getAll(false);
    }

    @Override
    public Optional<Ticker> getTicker(Position position) {
        return tickerContainer.getTicker(position);
    }

    @Override
    public Ticker findEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet) {
        List<String> baseSynonyms = getSymbolSynonyms(originalTicker.getBase());
        List<String> targetSynonyms = getSymbolSynonyms(originalTicker.getTarget());
        return tickerSet.stream().filter(ticker -> baseSynonyms.contains(ticker.getBase()) &&
                targetSynonyms.contains(ticker.getTarget())).findAny().orElse(null);
    }

    private List<String> getSymbolSynonyms(String symbol) {
        if (Constants.BITCOIN_SYMBOLS.contains(symbol)) {
            return Constants.BITCOIN_SYMBOLS;
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

    @Override
    public BigDecimal getUsdPrice(String base, ExchangeName exchangeName) {
        BigDecimal price = findUsdPrice(base, exchangeName);
        if (price != null) {
            return price;
        }
        return getSymbolSynonyms(base).stream().map(baseSynonym -> findUsdPrice(baseSynonym, fallbackExchangeName))
                .filter(
                        Objects::nonNull).findFirst().orElseThrow(
                        () -> new NonexistentUSDPriceException(exchangeName.getFullName(),
                                                               fallbackExchangeName.getFullName(), base));
    }

    private BigDecimal findUsdPrice(String base, ExchangeName exchangeName) {
        return Constants.USD_SYMBOLS.stream().map(usdSymbol -> tickerContainer.getTicker(exchangeName, base, usdSymbol))
                .filter(
                        Optional::isPresent).map(ticker -> ticker.get().getPrice()).findFirst().orElse(null);
    }
}
