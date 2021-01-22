package my.dub.dlp_pilot.service.impl.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static my.dub.dlp_pilot.Constants.BITMAX_CLIENT_SERVICE_BEAN_NAME;
import static my.dub.dlp_pilot.Constants.NO_BARS_FOUND_IN_RESPONSE_MSG;

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
import my.dub.dlp_pilot.service.client.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service(BITMAX_CLIENT_SERVICE_BEAN_NAME)
public class BitmaxExchangeClientService extends AbstractExchangeClientService implements ExchangeClientService {

    public static final String SYMBOL_PAIR_ACCEPTABLE_STATUS = "Normal";
    public static final String SYMBOL = "symbol";

    @Autowired
    public BitmaxExchangeClientService(ExchangeService exchangeService, ApiClient apiClient) {
        super(exchangeService, apiClient, ExchangeName.BITMAX);
    }

    /**
     * @see <a href="https://bitmax-exchange.github.io/bitmax-pro-api/#list-all-products">Bitfinex REST API - List all
     * Products</a>
     */
    @Override
    public List<SymbolPair> fetchSymbolPairs() throws IOException {
        JsonNode parentNode =
                apiClient.executeRequestParseResponse(exchange.getBaseEndpoint(), "products", exchangeFullName);
        checkResponseStatus(parentNode, "");
        JsonNode dataNode = getDataNode(parentNode, Constants.NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG);
        List<SymbolPair> symbolPairsResult = new ArrayList<>();
        for (JsonNode innerNode : dataNode) {
            if (!SYMBOL_PAIR_ACCEPTABLE_STATUS.equalsIgnoreCase(innerNode.get("status").asText())) {
                continue;
            }
            SymbolPair symbolPair = new SymbolPair(exchangeName, innerNode.get(SYMBOL).asText());
            symbolPair.setBase(innerNode.get("baseAsset").asText());
            symbolPair.setTarget(innerNode.get("quoteAsset").asText());
            symbolPairsResult.add(symbolPair);
        }
        return symbolPairsResult;
    }

    /**
     * @see <a href="https://bitmax-exchange.github.io/bitmax-pro-api/#ticker">Bitfinex REST API - Ticker</a>
     */
    @Override
    public Set<Ticker> fetchAllTickers(@NonNull List<SymbolPair> symbolPairs) throws IOException {
        checkNotNull(symbolPairs, Constants.NULL_ARGUMENT_MESSAGE, "symbolPairs");

        JsonNode parentNode =
                apiClient.executeRequestParseResponse(exchange.getBaseEndpoint(), "ticker", exchangeFullName);
        checkResponseStatus(parentNode, "");
        JsonNode dataNode = getDataNode(parentNode, Constants.NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : dataNode) {
            Ticker ticker = new Ticker(exchangeName);
            try {
                SymbolPair symbolPair =
                        symbolPairs.stream().filter(sP -> sP.getName().equals(innerNode.get(SYMBOL).asText()))
                                .findFirst().orElseThrow();
                ticker.setBase(symbolPair.getBase());
                ticker.setTarget(symbolPair.getTarget());
                ticker.setClosePrice(parsePrice(innerNode.get("close")).orElseThrow());
                JsonNode askNode = innerNode.get("ask");
                if (askNode == null || askNode.isEmpty()) {
                    logInvalidPriceData(symbolPair.getPair(), "ASK");
                    continue;
                }
                ticker.setPriceAsk(parsePrice(askNode.get(0)).orElseThrow());
                ticker.setAskQuantity(parseVolume(askNode.get(1)).orElseThrow());
                JsonNode bidNode = innerNode.get("bid");
                if (bidNode == null || bidNode.isEmpty()) {
                    logInvalidPriceData(symbolPair.getPair(), "BID");
                    continue;
                }
                ticker.setPriceBid(parsePrice(bidNode.get(0)).orElseThrow());
                ticker.setBidQuantity(parseVolume(bidNode.get(1)).orElseThrow());
            } catch (NoSuchElementException ignored) {
                continue;
            }
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @see <a href="https://bitmax-exchange.github.io/bitmax-pro-api/#historical-bar-data">Bitfinex REST API -
     * Historical Bar Data</a>
     */
    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, @NonNull Instant startTime,
            @NonNull Instant endTime) throws IOException {
        checkNotNull(symbolPair, Constants.NULL_ARGUMENT_MESSAGE, "symbolPair");
        checkNotNull(timeFrame, Constants.NULL_ARGUMENT_MESSAGE, "timeFrame");
        checkNotNull(startTime, Constants.NULL_ARGUMENT_MESSAGE, "startTime");
        checkNotNull(endTime, Constants.NULL_ARGUMENT_MESSAGE, "endTime");

        return fetchBars(symbolPair, timeFrame, startTime, endTime, exchange.getMaxBarsPerRequest());
    }

    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, long barsLimit)
            throws IOException {
        checkNotNull(symbolPair, Constants.NULL_ARGUMENT_MESSAGE, "symbolPair");
        checkNotNull(timeFrame, Constants.NULL_ARGUMENT_MESSAGE, "timeFrame");

        return fetchBars(symbolPair, timeFrame, null, null, barsLimit);
    }

    @Override
    protected void checkResponseStatus(JsonNode parentNode, String errorMessage) {
        int code = parentNode.get("code").asInt();
        if (code != 0) {
            String message = parentNode.get("message").asText();
            throw new UnexpectedEndpointResponseException(exchangeFullName, String.valueOf(code), message);
        }
    }

    private JsonNode getDataNode(JsonNode parentNode, String errorMessage) {
        JsonNode dataNode = parentNode.get("data");
        if (dataNode == null || dataNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, errorMessage);
        }
        return dataNode;
    }

    private List<Bar> fetchBars(SymbolPair symbolPair, TimeFrame timeFrame, Instant startTime, Instant endTime,
            long barsLimit) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(SYMBOL, symbolPair.getName());
        queryParams.put("interval", timeFrame.getExchangeValue(exchangeName));
        if (startTime != null) {
            queryParams.put("from", String.valueOf(startTime.toEpochMilli()));
        }
        if (barsLimit > 0) {
            barsLimit = checkBarsLimit(barsLimit);
            queryParams.put("n", String.valueOf(barsLimit));
        } else {
            return Collections.emptyList();
        }
        JsonNode parentNode = apiClient
                .executeRequestParseResponse(exchange.getBaseEndpoint(), "barhist", queryParams, exchangeFullName);
        checkResponseStatus(parentNode, "");
        JsonNode dataNode = getDataNode(parentNode, NO_BARS_FOUND_IN_RESPONSE_MSG);
        List<Bar> bars = new ArrayList<>();
        for (JsonNode innerNode : dataNode) {
            Bar bar;
            try {
                JsonNode innerDataNode = innerNode.get("data");
                if (innerDataNode == null || innerDataNode.isEmpty()) {
                    log.trace("Inner node does not contain bar data in response for exchange {}!", exchangeName);
                    continue;
                }
                Instant openTime = parseDateTimeFromMillis(innerDataNode.get("ts")).orElseThrow();
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
                bar.setOpen(parsePrice(innerDataNode.get("o")).orElseThrow());
                bar.setClose(parsePrice(innerDataNode.get("c")).orElseThrow());
                bar.setHigh(parsePrice(innerDataNode.get("h")).orElseThrow());
                bar.setLow(parsePrice(innerDataNode.get("l")).orElseThrow());
                bar.setVolume(parseVolume(innerDataNode.get("v")).orElseThrow());
            } catch (NoSuchElementException ignored) {
                continue;
            }
            bars.add(bar);
        }
        return bars;
    }

}
