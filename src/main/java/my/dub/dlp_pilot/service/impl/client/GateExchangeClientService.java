package my.dub.dlp_pilot.service.impl.client;

import static my.dub.dlp_pilot.Constants.GATE_CLIENT_SERVICE_BEAN_NAME;
import static my.dub.dlp_pilot.Constants.NO_BARS_FOUND_IN_RESPONSE_MSG;
import static my.dub.dlp_pilot.Constants.NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG;
import static my.dub.dlp_pilot.Constants.NO_TICKERS_FOUND_IN_RESPONSE_MSG;
import static my.dub.dlp_pilot.util.ApiClientUtils.executeRequestParseResponse;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service(GATE_CLIENT_SERVICE_BEAN_NAME)
public class GateExchangeClientService extends AbstractExchangeClientService implements ExchangeClientService {

    public static final String SYMBOL_PAIR_ACCEPTABLE_STATUS = "tradable";

    @Autowired
    public GateExchangeClientService(ExchangeService exchangeService) {
        super(exchangeService, ExchangeName.GATE);
    }

    /**
     * @see <a href="https://www.gate.io/docs/apiv4/en/index.html#list-all-currency-pairs-supported">Gate REST API -
     * List all currency pairs supported</a>
     */
    @Override
    public List<SymbolPair> fetchSymbolPairs() throws IOException {
        JsonNode parentNode =
                executeRequestParseResponse(exchange.getBaseEndpoint(), "spot/currency_pairs", exchangeFullName);
        checkResponseStatus(parentNode, NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG);
        List<SymbolPair> symbolPairsResult = new ArrayList<>();
        for (JsonNode innerNode : parentNode) {
            if (!SYMBOL_PAIR_ACCEPTABLE_STATUS.equalsIgnoreCase(innerNode.get("trade_status").asText())) {
                continue;
            }
            SymbolPair symbolPair = new SymbolPair(exchangeName, innerNode.get("id").asText());
            symbolPair.setBase(innerNode.get("base").asText());
            symbolPair.setTarget(innerNode.get("quote").asText());
            symbolPairsResult.add(symbolPair);
        }
        return symbolPairsResult;
    }

    /**
     * @see <a href="https://www.gate.io/docs/apiv4/en/index.html#retrieve-ticker-information">Gate REST API - Retrieve
     * ticker information</a>
     */
    @Override
    public Set<Ticker> fetchAllTickers(@NonNull List<SymbolPair> symbolPairs) throws IOException {
        JsonNode parentNode = executeRequestParseResponse(exchange.getBaseEndpoint(), "spot/tickers", exchangeFullName);
        checkResponseStatus(parentNode, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            Ticker ticker = new Ticker(exchangeName);
            try {
                SymbolPair symbolPair =
                        symbolPairs.stream().filter(sP -> sP.getName().equals(innerNode.get("currency_pair").asText()))
                                .findFirst().orElseThrow();
                ticker.setBase(symbolPair.getBase());
                ticker.setTarget(symbolPair.getTarget());
                ticker.setPriceAsk(parsePrice(innerNode.get("lowest_ask")).orElseThrow());
                ticker.setPriceBid(parsePrice(innerNode.get("highest_bid")).orElseThrow());
                ticker.setClosePrice(parsePrice(innerNode.get("last")).orElseThrow());
            } catch (NoSuchElementException ignored) {
                continue;
            }
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @see <a href="https://www.gate.io/docs/apiv4/en/index.html#market-candlesticks">Gate REST API - Market
     * candlesticks</a>
     */
    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame,
            @NonNull ZonedDateTime startTime, @NonNull ZonedDateTime endTime) throws IOException {
        ZonedDateTime limitedEndTime = startTime
                .plus((exchange.getMaxBarsPerRequest() - 1) * timeFrame.getDuration().toMillis(), ChronoUnit.MILLIS);
        return fetchBars(symbolPair, timeFrame, startTime, limitedEndTime, -1);
    }

    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, long barsLimit)
            throws IOException {
        return fetchBars(symbolPair, timeFrame, null, null, barsLimit);
    }

    @Override
    protected void checkResponseStatus(JsonNode parentNode, String errorMessage) {
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, errorMessage);
        }
        JsonNode messageNode = parentNode.get("message");
        if (messageNode != null) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, parentNode.get("label").asText(),
                                                          messageNode.asText());
        }
    }

    private List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, ZonedDateTime startTime,
            ZonedDateTime endTime, long barsLimit) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        if (startTime != null) {
            queryParams.put("from", String.valueOf(startTime.toEpochSecond()));
        }
        if (endTime != null) {
            queryParams.put("to", String.valueOf(endTime.toEpochSecond()));
        }
        if (barsLimit > 0) {
            barsLimit = checkBarsLimit(barsLimit);
            queryParams.put("limit", String.valueOf(barsLimit));
        }
        queryParams.put("interval", timeFrame.getExchangeValue(exchangeName));
        queryParams.put("currency_pair", symbolPair.getName());
        JsonNode parentNode = executeRequestParseResponse(exchange.getBaseEndpoint(), "spot/candlesticks", queryParams,
                                                          exchangeFullName);
        checkResponseStatus(parentNode, NO_BARS_FOUND_IN_RESPONSE_MSG);
        List<Bar> bars = new ArrayList<>();
        for (JsonNode innerNode : parentNode) {
            Bar bar;
            try {
                ZonedDateTime openTime = parseDateTime(innerNode.get(0), ChronoUnit.SECONDS).orElseThrow();
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
        bar.setVolume(parseVolume(innerNode.get(1)).orElseThrow());
        bar.setClose(parsePrice(innerNode.get(2)).orElseThrow());
        bar.setHigh(parsePrice(innerNode.get(3)).orElseThrow());
        bar.setLow(parsePrice(innerNode.get(4)).orElseThrow());
        bar.setOpen(parsePrice(innerNode.get(5)).orElseThrow());
        return bar;
    }
}
