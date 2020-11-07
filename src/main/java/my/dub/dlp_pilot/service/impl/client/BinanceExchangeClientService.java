package my.dub.dlp_pilot.service.impl.client;

import static my.dub.dlp_pilot.Constants.BINANCE_CLIENT_SERVICE_BEAN_NAME;
import static my.dub.dlp_pilot.Constants.NO_BARS_FOUND_IN_RESPONSE_MSG;
import static my.dub.dlp_pilot.Constants.NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG;
import static my.dub.dlp_pilot.Constants.NO_TICKERS_FOUND_IN_RESPONSE_MSG;
import static my.dub.dlp_pilot.util.ApiClientUtils.executeRequestParseResponse;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.exception.client.UnexpectedEndpointResponseException;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.service.ExchangeClientService;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.client.AbstractExchangeClientService;
import my.dub.dlp_pilot.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service(BINANCE_CLIENT_SERVICE_BEAN_NAME)
public class BinanceExchangeClientService extends AbstractExchangeClientService implements ExchangeClientService {

    private static final List<String> SYMBOL_STATUS_DISABLED = List.of("HALT", "BREAK");
    private static final String SYMBOL = "symbol";

    @Autowired
    public BinanceExchangeClientService(ExchangeService exchangeService) {
        super(exchangeService, ExchangeName.BINANCE);
    }

    /**
     * @see
     * <a href="https://github.com/binance-exchange/binance-official-api-docs/blob/master/rest-api.md#exchange-information">
     * Binance REST API - Exchange information</a>
     */
    @Override
    public List<SymbolPair> fetchSymbolPairs() throws IOException {
        JsonNode parentNode = executeRequestParseResponse(exchange.getBaseEndpoint(), "exchangeInfo", exchangeFullName);
        JsonNode symbolsNode = parentNode.get("symbols");
        if (symbolsNode == null || symbolsNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG);
        }
        List<SymbolPair> result = new ArrayList<>();
        for (JsonNode innerNode : symbolsNode) {
            String name = innerNode.get(SYMBOL).asText();
            if (StringUtils.isBlank(name)) {
                continue;
            }
            String status = innerNode.get("status").asText();
            if (SYMBOL_STATUS_DISABLED.contains(status)) {
                log.trace("Symbol {} on {} exchange has unacceptable status {}. Skipping symbol info...", name,
                          exchangeName, status);
                continue;
            }
            SymbolPair symbolPair = new SymbolPair(exchangeName, name);
            String base = innerNode.get("baseAsset").asText();
            if (StringUtils.isBlank(base)) {
                continue;
            }
            symbolPair.setBase(parseSymbol(base));
            String target = innerNode.get("quoteAsset").asText();
            if (StringUtils.isBlank(target)) {
                continue;
            }
            symbolPair.setTarget(parseSymbol(target));
            result.add(symbolPair);
        }
        return result;
    }

    /**
     * @see
     * <a href="https://github.com/binance-exchange/binance-official-api-docs/blob/master/rest-api.md#symbol-order-book-ticker">
     * Binance REST API - Order Book</a>
     */
    @Override
    public Set<Ticker> fetchAllTickers(List<SymbolPair> symbolPairs) throws IOException {
        JsonNode parentNode =
                executeRequestParseResponse(exchange.getBaseEndpoint(), "ticker/bookTicker", exchangeFullName);
        checkResponseStatus(parentNode, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            Ticker ticker = new Ticker(ExchangeName.BINANCE);
            String pair = innerNode.get(SYMBOL).asText();
            try {
                SymbolPair symbolPair =
                        symbolPairs.stream().filter(sp -> pair.equals(sp.getName())).findFirst().orElseThrow();
                ticker.setBase(symbolPair.getBase());
                ticker.setTarget(symbolPair.getTarget());
                ticker.setPriceAsk(parsePrice(innerNode.get("askPrice")).orElseThrow());
                ticker.setPriceBid(parsePrice(innerNode.get("bidPrice")).orElseThrow());
                ticker.setBidQuantity(parseVolume(innerNode.get("bidQty")).orElseThrow());
                ticker.setAskQuantity(parseVolume(innerNode.get("askQty")).orElseThrow());
            } catch (NoSuchElementException e) {
                continue;
            }
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @see
     * <a href="https://github.com/binance-exchange/binance-official-api-docs/blob/master/rest-api.md#klinecandlestick-data">
     * Binance REST API - Kline/Candlestick data</a>
     */
    @Override
    public List<Bar> fetchBars(SymbolPair symbolPair, TimeFrame timeFrame, ZonedDateTime startTime,
            ZonedDateTime endTime) throws IOException {
        return fetchBars(symbolPair, timeFrame, startTime, endTime, exchange.getMaxBarsPerRequest());
    }

    @Override
    public List<Bar> fetchBars(SymbolPair symbolPair, TimeFrame timeFrame, long barsLimit) throws IOException {
        return fetchBars(symbolPair, timeFrame, null, null, barsLimit);
    }

    private List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, ZonedDateTime startTime,
            ZonedDateTime endTime, long barsLimit) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        if (startTime != null) {
            queryParams.put("startTime", String.valueOf(startTime.toInstant().toEpochMilli()));
        }
        if (barsLimit > 0) {
            checkBarsLimit(barsLimit);
            queryParams.put("limit", String.valueOf(barsLimit));
        } else {
            return Collections.emptyList();
        }
        queryParams.put("interval", timeFrame.getExchangeValue(exchangeName));
        queryParams.put(SYMBOL, symbolPair.getName());
        JsonNode parentNode =
                executeRequestParseResponse(exchange.getBaseEndpoint(), "klines", queryParams, exchangeFullName);
        checkResponseStatus(parentNode, NO_BARS_FOUND_IN_RESPONSE_MSG);
        List<Bar> bars = new ArrayList<>();
        for (JsonNode innerNode : parentNode) {
            Bar bar;
            try {
                ZonedDateTime openTime = parseDateTime(innerNode.get(0)).orElseThrow();
                ZonedDateTime closeTime = openTime.plus(timeFrame.getDuration());
                if (endTime != null && openTime.isAfter(endTime)) {
                    break;
                }
                if (closeTime.isAfter(DateUtils.currentDateTimeUTC())) {
                    continue;
                }
                bar = createBar(innerNode, symbolPair.getBase(), symbolPair.getTarget());
                bar.setOpenTime(openTime);
                bar.setCloseTime(closeTime);
            } catch (NoSuchElementException ignored) {
                continue;
            }
            bars.add(bar);
        }
        return bars;
    }

    private Bar createBar(JsonNode innerNode, String base, String target) {
        Bar bar = new Bar(exchangeName, base, target);
        bar.setOpen(parsePrice(innerNode.get(1)).orElseThrow());
        bar.setHigh(parsePrice(innerNode.get(2)).orElseThrow());
        bar.setLow(parsePrice(innerNode.get(3)).orElseThrow());
        bar.setClose(parsePrice(innerNode.get(4)).orElseThrow());
        bar.setVolume(parseVolume(innerNode.get(5)).orElseThrow());
        return bar;
    }

    private void checkResponseStatus(JsonNode parentNode, String errorMessage) {
        JsonNode statusNode = parentNode.get("code");
        if (statusNode != null) {
            String message = statusNode.get("msg").asText();
            throw new UnexpectedEndpointResponseException(exchangeFullName, statusNode.asText(), message);
        }
        if (parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, errorMessage);
        }
    }
}
