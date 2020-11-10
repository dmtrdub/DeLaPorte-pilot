package my.dub.dlp_pilot.service.client;

import static my.dub.dlp_pilot.util.ApiClientUtils.executeRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.client.UnexpectedEndpointResponseException;
import my.dub.dlp_pilot.exception.client.UnexpectedResponseStatusCodeException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @deprecated use implementations of {@link my.dub.dlp_pilot.service.ExchangeClientService}
 */
@Slf4j
@Component
@Deprecated(forRemoval = true)
public class ApiClient {

    private static final String NO_TICKERS_FOUND_IN_RESPONSE_MSG = "No tickers found in response!";
    private static final String NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG = "No symbol data found in response!";

    private static final String PRICE_TYPE_ASK = "ASK";
    private static final String PRICE_TYPE_BID = "BID";

    private static final String MESSAGE = "message";
    private static final String SYMBOL = "symbol";
    private static final String STATUS = "status";
    private static final String CODE = "code";
    private static final String DATA = "data";
    private static final String CLOSE = "close";
    private static final String ERROR = "error";
    private static final String TICKER = "ticker";

    private final Map<ExchangeName, Map<String, String>> symbolAdditionalData = new ConcurrentHashMap<>();

    private final ExchangeService exchangeService;

    public ApiClient(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    public void initConnection(Set<Exchange> exchanges) {
        fetchAllAdditionalSymbolData(exchanges);
    }

    private void fetchAllAdditionalSymbolData(Set<Exchange> exchanges) {
        exchanges.forEach(exchange -> {
            try {
                fetchAdditionalSymbolData(exchange);
            } catch (IOException e) {
                log.error("Unable to fetch additional symbol data for Exchange: {}! Caused by: {}",
                          exchange.getFullName(), e.getMessage());
            }
        });
    }

    private void fetchAdditionalSymbolData(Exchange exchange) throws IOException {
        if (exchange.getName() == ExchangeName.HUOBI) {
            fetchHuobiAdditionalSymbolData(exchange);
        }
        if (!CollectionUtils.isEmpty(symbolAdditionalData.get(exchange.getName()))) {
            log.debug("Successfully fetched additional symbol data for {} exchange", exchange.getFullName());
        }
    }

    public Set<Ticker> fetchTickers(Exchange exchange) {
        Set<Ticker> result = new HashSet<>();
        ExchangeName exchangeName = exchange.getName();
        try {
            switch (exchangeName) {
                case BITBAY:
                    result = fetchBitBayTickers(exchange);
                    break;
                case BITMART:
                    result = fetchBitmartTickers(exchange);
                    break;
                case BITTREX:
                    result = fetchBittrexTickers(exchange);
                    break;
                case EXMO:
                    result = fetchExmoTickers(exchange);
                    break;
                case GATE:
                    result = fetchGateTickers(exchange);
                    break;
                case HUOBI:
                    result = fetchHuobiTickers(exchange);
                    break;
            }

        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.error(e.getMessage());
            exchangeService.updateExchangeFault(exchangeName, true);
        } catch (IOException e) {
            log.error("Unable to fetch tickers on {} exchange! Details: {}", exchangeName, e.toString());
            exchangeService.updateExchangeFault(exchangeName, true);
        }

        return result;
    }

    private void fetchHuobiAdditionalSymbolData(Exchange exchange) throws IOException {
        Map<String, String> data = fetchHuobiSymbols(exchange);
        symbolAdditionalData.put(ExchangeName.HUOBI, data);
    }

    private Map<String, String> fetchHuobiSymbols(Exchange huobiExchange) throws IOException {
        String exchangeName = huobiExchange.getFullName();
        String resp = executeRequest(huobiExchange.getBaseEndpoint(), "v1/common/symbols", exchangeName);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        checkHuobiStatus(exchangeName, parentNode);
        JsonNode symbolsNode = parentNode.get(DATA);
        if (symbolsNode == null || symbolsNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG);
        }
        Map<String, String> result = new HashMap<>();
        for (JsonNode innerNode : symbolsNode) {
            String pair = innerNode.get(SYMBOL).asText();
            String status = innerNode.get("state").asText();
            if (!"online".equalsIgnoreCase(status)) {
                log.trace("Symbol {} on {} exchange has unacceptable status {}. Skipping symbol info...", pair,
                          exchangeName, status);
                continue;
            }
            String base = innerNode.get("base-currency").asText();
            String target = innerNode.get("quote-currency").asText();
            result.put(pair, base + Constants.DEFAULT_PAIR_DELIMITER + target);
        }
        return result;
    }

    /**
     * @param exchange
     *
     * @see <a href="https://docs.bitbay.net/v1.0.1-en/reference#ticker-1">BitBay REST API - Ticker</a>
     */
    private Set<Ticker> fetchBitBayTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "trading/ticker", exchangeName);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        JsonNode statusNode = parentNode.get(STATUS);

        String status = statusNode.asText();
        if (!"Ok".equalsIgnoreCase(status)) {
            ArrayList<String> errors = objectMapper.convertValue(parentNode.get("errors"), ArrayList.class);
            String message = String.join("; ", errors);
            throw new UnexpectedEndpointResponseException(exchangeName, status, message);
        }

        JsonNode itemsNode = parentNode.get("items");
        if (itemsNode == null || itemsNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : itemsNode) {
            Ticker ticker = new Ticker(ExchangeName.BITBAY);
            boolean parsePairResult = setSymbols(innerNode.get("market").get(CODE).asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            BigDecimal closePrice = parsePrice(innerNode.get("rate"), exchangeName);
            if (closePrice == null) {
                continue;
            }
            ticker.setClosePrice(closePrice);
            BigDecimal bidPrice = parsePrice(innerNode.get("highestBid"), exchangeName);
            if (bidPrice == null) {
                continue;
            }
            ticker.setPriceBid(bidPrice);
            BigDecimal askPrice = parsePrice(innerNode.get("lowestAsk"), exchangeName);
            if (askPrice == null) {
                continue;
            }
            ticker.setPriceAsk(askPrice);
            setTickerDateTime(innerNode, ticker, "time", ChronoUnit.MILLIS);
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see <a href="https://developer.bitmart.com/v2/en/?http#spot-api-public-endpoints-ticker">Bitmart REST API -
     * Ticker</a>
     */
    private Set<Ticker> fetchBitmartTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), TICKER, exchangeName);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode message = parentNode.get(MESSAGE);
        if (message != null) {
            JsonNode codeResp = parentNode.get("errno");
            String code = codeResp == null ? "" : codeResp.asText();
            throw new UnexpectedEndpointResponseException(exchangeName, code, message.asText());
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            Ticker ticker = new Ticker(ExchangeName.BITMART);
            boolean parsePairResult = setSymbols(innerNode.get("symbol_id").asText(), "_", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setClosePrice(new BigDecimal(innerNode.get("current_price").asText()));
            ticker.setPriceAsk(new BigDecimal(innerNode.get("ask_1").asText()));
            ticker.setPriceBid(new BigDecimal(innerNode.get("bid_1").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see <a href="https://bittrex.github.io/api/v3#operation--markets-tickers-get">Bittrex REST API - Ticker</a>
     */
    private Set<Ticker> fetchBittrexTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "markets/tickers", exchangeName);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);

        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode codeResp = parentNode.get(CODE);
        if (codeResp != null) {
            String code = codeResp.asText();
            String message = parentNode.get("detail") != null ? parentNode.get("detail").asText() : "";
            throw new UnexpectedEndpointResponseException(exchangeName, code, message);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            Ticker ticker = new Ticker(ExchangeName.BITTREX);
            boolean parsePairResult = setSymbols(innerNode.get(SYMBOL).asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setClosePrice(new BigDecimal(innerNode.get("lastTradeRate").asText()));
            ticker.setPriceAsk(new BigDecimal(innerNode.get("askRate").asText()));
            ticker.setPriceBid(new BigDecimal(innerNode.get("bidRate").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see
     * <a href="https://documenter.getpostman.com/view/10287440/SzYXWKPi?version=d0437340-da8e-4fe8-8a3b-e81b8e972e22#4c8e6459-3503-4361-b012-c34bb9f7e385">EXMO
     * REST API - Ticker</a>
     */
    private Set<Ticker> fetchExmoTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), TICKER, exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode errorNode = parentNode.get(ERROR);
        if (errorNode != null) {
            throw new UnexpectedEndpointResponseException(exchangeName, errorNode.asText());
        }
        Set<Ticker> tickers = new HashSet<>();
        parentNode.fields().forEachRemaining(entry -> {
            Ticker ticker = new Ticker(ExchangeName.EXMO);
            boolean parsePairResult = setSymbols(entry.getKey(), "_", ticker);
            if (!parsePairResult) {
                return;
            }
            JsonNode innerNode = entry.getValue();
            ticker.setClosePrice(new BigDecimal(innerNode.get("last_trade").asText()));
            ticker.setPriceAsk(new BigDecimal(innerNode.get("sell_price").asText()));
            ticker.setPriceBid(new BigDecimal(innerNode.get("buy_price").asText()));
            setTickerDateTime(innerNode, ticker, "updated", ChronoUnit.SECONDS);
            tickers.add(ticker);
        });
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see <a href="https://www.gate.io/en/api2#tickers">GATE.IO REST API - Tickers</a>
     */
    private Set<Ticker> fetchGateTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "tickers", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode errorCodeNode = parentNode.get(CODE);
        if (errorCodeNode != null) {
            throw new UnexpectedEndpointResponseException(exchangeName, errorCodeNode.asText(),
                                                          parentNode.get(MESSAGE).asText());
        }
        Set<Ticker> tickers = new HashSet<>();
        parentNode.fields().forEachRemaining(entry -> {
            Ticker ticker = new Ticker(ExchangeName.GATE);
            boolean parsePairResult = setSymbols(entry.getKey(), "_", ticker);
            if (!parsePairResult) {
                return;
            }
            JsonNode innerNode = entry.getValue();
            BigDecimal close = parsePrice(innerNode.get("last"), exchangeName);
            if (close == null) {
                return;
            }
            ticker.setClosePrice(close);
            BigDecimal ask = parsePrice(innerNode.get("lowestAsk"), exchangeName);
            if (ask == null) {
                return;
            }
            ticker.setPriceAsk(ask);
            BigDecimal bid = parsePrice(innerNode.get("highestBid"), exchangeName);
            if (bid == null) {
                return;
            }
            ticker.setPriceBid(bid);
            tickers.add(ticker);
        });
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see <a href="https://huobiapi.github.io/docs/spot/v1/en/#get-latest-tickers-for-all-pairs">HUOBI REST API -
     * Tickers</a>
     */
    private Set<Ticker> fetchHuobiTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "market/tickers", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        checkHuobiStatus(exchangeName, parentNode);
        JsonNode dataNode = parentNode.get(DATA);
        if (dataNode == null || dataNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : dataNode) {
            String symbol = innerNode.get(SYMBOL).asText();
            String pair = symbolAdditionalData.get(exchange.getName()).get(symbol);
            if (StringUtils.isEmpty(pair)) {
                continue;
            }
            Ticker ticker = new Ticker(ExchangeName.HUOBI);
            setSymbols(pair, ticker);
            ticker.setClosePrice(new BigDecimal(innerNode.get(CLOSE).asText()));
            ticker.setPriceAsk(new BigDecimal(innerNode.get("ask").asText()));
            ticker.setPriceBid(new BigDecimal(innerNode.get("bid").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    private void checkHuobiStatus(String exchangeName, JsonNode parentNode) {
        JsonNode statusNode = parentNode.get(STATUS);
        String status = statusNode.asText();
        if (ERROR.equalsIgnoreCase(status)) {
            throw new UnexpectedEndpointResponseException(exchangeName, parentNode.get("err-code").asText(),
                                                          parentNode.get("err-msg").asText());
        }
    }

    private boolean setSymbols(String input, Ticker ticker) {
        return setSymbols(input, Constants.DEFAULT_PAIR_DELIMITER, ticker);
    }

    private boolean setSymbols(String input, String splitRegex, Ticker ticker) {
        String[] symbols;
        try {
            symbols = input.split(splitRegex, 2);
            ticker.setBase(findOriginalSymbol(symbols[0].toUpperCase()));
            ticker.setTarget(findOriginalSymbol(symbols[1].toUpperCase()));
        } catch (ArrayIndexOutOfBoundsException e) {
            log.trace("Incorrect pair input ({}) for {} exchange! Expecting to parse with split regex: {}", input,
                      ticker.getExchangeName().getFullName(), splitRegex);
            return false;
        }
        return true;
    }

    private String findOriginalSymbol(String symbol) {
        if (Constants.BITCOIN_SYMBOLS.contains(symbol)) {
            return Constants.BITCOIN_SYMBOLS.get(0);
        }
        if (Constants.BITCOIN_CASH_SYMBOLS.contains(symbol)) {
            return Constants.BITCOIN_CASH_SYMBOLS.get(0);
        }
        if (Constants.BITCOIN_SV_SYMBOLS.contains(symbol)) {
            return Constants.BITCOIN_SV_SYMBOLS.get(0);
        }
        if (Constants.STELLAR_SYMBOLS.contains(symbol)) {
            return Constants.STELLAR_SYMBOLS.get(0);
        }
        return symbol;
    }

    private void logInvalidPriceData(String exchangeName, String pair, String priceType) {
        log.trace("Unable to fetch {} price data for pair: {} on exchange: {}. Skipping...", priceType, pair,
                  exchangeName);
    }

    private void setTickerDateTime(JsonNode innerNode, Ticker ticker, String fieldName, ChronoUnit epochChronoUnit) {
        ZonedDateTime dateTime;
        long epoch = innerNode.get(fieldName).asLong();
        if (ChronoUnit.SECONDS.equals(epochChronoUnit)) {
            dateTime = DateUtils.dateTimeFromEpochSecond(epoch);
        } else {
            dateTime = DateUtils.dateTimeFromEpochMilli(epoch);
        }
        if (dateTime.isBefore(DateUtils.currentDateTimeUTC())) {
            ticker.setDateTime(dateTime);
        }
    }

    private BigDecimal parsePrice(JsonNode priceNode, String exchangeName) {
        if (priceNode == null) {
            log.trace("Null or empty price node found in response from {} exchange. Skipping...", exchangeName);
            return null;
        }
        String price = priceNode.asText();
        try {
            return new BigDecimal(price);
        } catch (NumberFormatException e) {
            log.trace("Wrong price value found in response ({}) from {} exchange. Skipping...", price, exchangeName);
            return null;
        }
    }
}
