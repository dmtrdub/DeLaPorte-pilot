package my.dub.dlp_pilot.service.impl.client;

import static my.dub.dlp_pilot.Constants.BIGONE_CLIENT_SERVICE_BEAN_NAME;
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
import java.util.Objects;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service(BIGONE_CLIENT_SERVICE_BEAN_NAME)
public class BigoneExchangeClientService extends AbstractExchangeClientService implements ExchangeClientService {

    private static final String DELIMITER = "-";

    @Autowired
    public BigoneExchangeClientService(ExchangeService exchangeService) {
        super(exchangeService, ExchangeName.BIGONE);
    }

    /**
     * @see <a href="https://open.big.one/docs/spot_asset_pair.html#all-assetpairs">BigONE REST API - Asset Pairs</a>
     */
    @Override
    public List<SymbolPair> fetchSymbolPairs() throws IOException {
        JsonNode parentNode = executeRequestParseResponse(exchange.getBaseEndpoint(), "asset_pairs", exchangeFullName);
        checkResponseStatus(parentNode);
        JsonNode dataNode = getDataNode(parentNode, NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG);
        List<SymbolPair> symbolPairsResult = new ArrayList<>();
        for (JsonNode innerNode : dataNode) {
            String name = innerNode.get("name").asText();
            if (StringUtils.isBlank(name)) {
                continue;
            }
            SymbolPair symbolPair = new SymbolPair(exchangeName, name);
            if (!setSymbols(name, DELIMITER, symbolPair)) {
                continue;
            }
            symbolPairsResult.add(symbolPair);
        }
        return symbolPairsResult;
    }

    /**
     * @see <a href="https://open.big.one/docs/spot_tickers.html#ticker">BigONE REST API - Tickers</a>
     */
    @Override
    public Set<Ticker> fetchAllTickers(@NonNull List<SymbolPair> symbolPairs) throws IOException {
        JsonNode parentNode =
                executeRequestParseResponse(exchange.getBaseEndpoint(), "asset_pairs/tickers", exchangeFullName);
        checkResponseStatus(parentNode);
        JsonNode dataNode = getDataNode(parentNode, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : dataNode) {
            Ticker ticker = new Ticker(ExchangeName.BIGONE);
            String symbolPairName = innerNode.get("asset_pair_name").asText();
            try {
                SymbolPair symbolPair =
                        symbolPairs.stream().filter(sP -> sP.getName().equals(symbolPairName)).findFirst()
                                .orElseThrow();
                ticker.setBase(symbolPair.getBase());
                ticker.setTarget(symbolPair.getTarget());
                ticker.setClosePrice(parsePrice(innerNode.get("close")).orElseThrow());
                JsonNode askNode = innerNode.get("ask");
                if (askNode == null) {
                    logInvalidPriceData(ticker.getPair(), "ASK");
                    continue;
                }
                ticker.setPriceAsk(parsePrice((askNode.get("price"))).orElseThrow());
                ticker.setAskQuantity(parseVolume(askNode.get("quantity")).orElseThrow());
                JsonNode bidNode = innerNode.get("bid");
                if (bidNode == null) {
                    logInvalidPriceData(ticker.getPair(), "BID");
                    continue;
                }
                ticker.setPriceBid(parsePrice((bidNode.get("price"))).orElseThrow());
                ticker.setBidQuantity(parseVolume(bidNode.get("quantity")).orElseThrow());
            } catch (NoSuchElementException e) {
                continue;
            }

            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @see <a href="https://open.big.one/docs/spot_asset_pair_candle.html#candles-of-a-asset-pair">BigONE REST API -
     * Candles of an asset pair</a>
     */
    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame,
            @NonNull ZonedDateTime startTime, @NonNull ZonedDateTime endTime) throws IOException {
        return fetchBars(symbolPair, timeFrame, startTime, endTime, exchange.getMaxBarsPerRequest());
    }

    @Override
    public List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, long barsLimit)
            throws IOException {
        return fetchBars(symbolPair, timeFrame, null, null, barsLimit);
    }

    private List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, ZonedDateTime startTime,
            ZonedDateTime endTime, long barsLimit) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        if (startTime != null) {
            queryParams.put("time", DateUtils.toIsoInstantString(startTime));
        }
        if (barsLimit > 0) {
            barsLimit = checkBarsLimit(barsLimit);
            queryParams.put("limit", String.valueOf(barsLimit));
        } else {
            return Collections.emptyList();
        }
        queryParams.put("period", timeFrame.getExchangeValue(exchangeName));
        JsonNode parentNode = executeRequestParseResponse(exchange.getBaseEndpoint(),
                                                          String.format("asset_pairs/%s/candles", symbolPair.getName()),
                                                          queryParams, exchangeFullName);
        checkResponseStatus(parentNode);
        JsonNode dataNode = getDataNode(parentNode, NO_BARS_FOUND_IN_RESPONSE_MSG);
        List<Bar> bars = new ArrayList<>();
        for (JsonNode innerNode : dataNode) {
            Bar bar;
            try {
                ZonedDateTime openTime = parseDateTime(innerNode.get("time")).orElseThrow();
                ZonedDateTime closeTime = openTime.plus(timeFrame.getDuration());
                if (endTime != null && closeTime.isBefore(endTime)) {
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
        bar.setClose(parsePrice(innerNode.get("close")).orElseThrow());
        bar.setHigh(parsePrice(innerNode.get("high")).orElseThrow());
        bar.setLow(parsePrice(innerNode.get("low")).orElseThrow());
        bar.setOpen(parsePrice(innerNode.get("open")).orElseThrow());
        bar.setVolume(parseVolume(innerNode.get("volume")).orElseThrow());
        return bar;
    }

    private JsonNode getDataNode(JsonNode parentNode, String errorMessage) {
        JsonNode dataNode = parentNode.get("data");
        if (dataNode == null || dataNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeFullName, errorMessage);
        }
        return dataNode;
    }

    private void checkResponseStatus(JsonNode parentNode) {
        checkResponseStatus(parentNode, "");
    }

    @Override
    protected void checkResponseStatus(JsonNode parentNode, String errorMessage) {
        JsonNode statusNode = parentNode.get("code");
        String status = statusNode.asText();
        if (!Objects.equals(status, "0")) {
            String message = statusNode.get("message").asText();
            throw new UnexpectedEndpointResponseException(exchangeFullName, status, message);
        }
    }

}
