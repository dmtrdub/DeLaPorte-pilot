package my.dub.dlp_pilot.service.impl.client;

import static my.dub.dlp_pilot.util.ApiClientUtils.executeRequestParseResponse;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.client.UnexpectedEndpointResponseException;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.service.ExchangeClientService;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.client.AbstractExchangeClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service(Constants.BITFINEX_CLIENT_SERVICE_BEAN_NAME)
public class BitfinexExchangeClientService extends AbstractExchangeClientService implements ExchangeClientService {

    private static final List<String> RAW_TARGET_SYMBOLS =
            List.of("BTC", "ETH", "USD", "EUR", "JPY", "GBP", "EOS", "UST", "XCH", "CNHT");
    private static final String SYMBOL_PAIR_PREFIX = "t";

    @Autowired
    public BitfinexExchangeClientService(ExchangeService exchangeService) {
        super(exchangeService, ExchangeName.BITFINEX);
    }

    /**
     * @see <a href="https://docs.bitfinex.com/reference#rest-public-conf">Bitfinex REST API - Configs</a>
     */
    @Override
    public List<SymbolPair> fetchSymbolPairs() throws IOException {
        JsonNode parentNode = executeRequestParseResponse(exchange.getBaseEndpoint(),
                                                          "conf/pub:map:currency:sym,pub:list:pair:exchange",
                                                          exchangeFullName);
        checkResponseStatus(parentNode, Constants.NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG);
        JsonNode firstNode = parentNode.get(0);
        Map<String, String> rawCurrenciesMap = StreamSupport.stream(firstNode.spliterator(), false)
                .filter(jsonNode -> !jsonNode.get(1).asText().contains("-")).collect(Collectors
                .toMap(jsonNode -> jsonNode.get(0).asText().toUpperCase(), jsonNode -> jsonNode.get(1).asText()
                .toUpperCase()));
        JsonNode secondNode = parentNode.get(1);
        if (secondNode == null || secondNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName,
                                                          Constants.NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG);
        }
        List<SymbolPair> symbolPairsResult = new ArrayList<>();
        for (JsonNode innerNode : secondNode) {
            String symbolPairName = innerNode.asText();
            String rawTarget = RAW_TARGET_SYMBOLS.stream().filter(symbolPairName::endsWith).findFirst().orElse(null);
            if (rawTarget == null) {
                continue;
            }
            String rawBase = symbolPairName.replace(rawTarget, "").replace(":", "");
            String parsedBase = rawCurrenciesMap.getOrDefault(rawBase, rawBase);
            String parsedTarget = rawCurrenciesMap.getOrDefault(rawTarget, rawTarget);
            SymbolPair symbolPair = new SymbolPair(exchangeName, SYMBOL_PAIR_PREFIX + symbolPairName);
            symbolPair.setBase(parsedBase);
            symbolPair.setTarget(parsedTarget);
            symbolPairsResult.add(symbolPair);
        }
        return symbolPairsResult;
    }

    /**
     * @see <a href="https://docs.bitfinex.com/reference#rest-public-tickers">Bitfinex REST API - Tickers</a>
     */
    @Override
    public Set<Ticker> fetchAllTickers(@NonNull List<SymbolPair> symbolPairs) throws IOException {
        JsonNode parentNode =
                executeRequestParseResponse(exchange.getBaseEndpoint(), "tickers", "symbols", "ALL", exchangeFullName);
        checkResponseStatus(parentNode, Constants.NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            if (innerNode == null || innerNode.size() < 4) {
                log.trace("Inner node does not contain full ticker data in response for exchange {}!", exchangeName);
                continue;
            }
            String pair = innerNode.get(0).asText();
            if (!pair.startsWith(SYMBOL_PAIR_PREFIX)) {
                continue;
            }
            Ticker ticker = new Ticker(ExchangeName.BITFINEX);
            try {
                SymbolPair symbolPair =
                        symbolPairs.stream().filter(sP -> sP.getName().equals(pair)).findFirst().orElseThrow();
                ticker.setBase(symbolPair.getBase());
                ticker.setTarget(symbolPair.getTarget());
                ticker.setPriceBid(parsePrice(innerNode.get(1)).orElseThrow());
                ticker.setPriceAsk(parsePrice(innerNode.get(3)).orElseThrow());
                ticker.setClosePrice(parsePrice(innerNode.get(7)).orElseThrow());
            } catch (NoSuchElementException ignored) {
                continue;
            }
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @see <a href="https://docs.bitfinex.com/reference#rest-public-candles">Bitfinex REST API - Candles</a>
     */
    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, @NonNull Instant startTime,
            @NonNull Instant endTime) throws IOException {
        return fetchBars(symbolPair, timeFrame, startTime, endTime, exchange.getMaxBarsPerRequest());
    }

    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, long barsLimit)
            throws IOException {
        return fetchBars(symbolPair, timeFrame, null, null, barsLimit);
    }

    private List<Bar> fetchBars(SymbolPair symbolPair, TimeFrame timeFrame, Instant startTime, Instant endTime,
            long barsLimit) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        if (startTime != null) {
            queryParams.put("start", String.valueOf(startTime.toEpochMilli()));
        }
        if (barsLimit > 0) {
            barsLimit = checkBarsLimit(barsLimit);
            queryParams.put("limit", String.valueOf(barsLimit));
        } else {
            return Collections.emptyList();
        }
        // asc sort
        queryParams.put("sort", "1");
        String endpointUrl = String.format("candles/trade:%s:%s/hist", timeFrame.getExchangeValue(exchangeName),
                                           symbolPair.getName());
        JsonNode parentNode =
                executeRequestParseResponse(exchange.getBaseEndpoint(), endpointUrl, queryParams, exchangeFullName);
        checkResponseStatus(parentNode, Constants.NO_BARS_FOUND_IN_RESPONSE_MSG);
        List<Bar> bars = new ArrayList<>();
        for (JsonNode innerNode : parentNode) {
            Bar bar;
            try {
                Instant openTime = parseDateTimeFromMillis(innerNode.get(0)).orElseThrow();
                Instant closeTime = openTime.plus(timeFrame.getDuration());
                if (endTime != null && openTime.isAfter(endTime)) {
                    break;
                }
                if (closeTime.isAfter(Instant.now())) {
                    continue;
                }
                bar = new Bar(exchangeName, symbolPair.getBase(), symbolPair.getTarget());
                bar.setOpenTime(openTime);
                bar.setCloseTime(closeTime);
                bar.setOpen(parsePrice(innerNode.get(1)).orElseThrow());
                bar.setClose(parsePrice(innerNode.get(2)).orElseThrow());
                bar.setHigh(parsePrice(innerNode.get(3)).orElseThrow());
                bar.setLow(parsePrice(innerNode.get(4)).orElseThrow());
                bar.setVolume(parseVolume(innerNode.get(5)).orElseThrow());
            } catch (NoSuchElementException ignored) {
                continue;
            }
            bars.add(bar);
        }
        return bars;
    }

    @Override
    protected void checkResponseStatus(JsonNode parentNode, String errorMessage) {
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, errorMessage);
        }
        JsonNode firstNode = parentNode.get(0);
        if (firstNode == null || firstNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, errorMessage);
        }
        if ("error".equalsIgnoreCase(firstNode.asText())) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, parentNode.get(2).asText(),
                                                          parentNode.get(3).asText());
        }
    }

}
