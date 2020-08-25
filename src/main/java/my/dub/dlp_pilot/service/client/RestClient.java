package my.dub.dlp_pilot.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.rest.UnexpectedEndpointResponseException;
import my.dub.dlp_pilot.exception.rest.UnexpectedResponseStatusCodeException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class RestClient implements InitializingBean {

    private static final String NO_TICKERS_FOUND_IN_RESPONSE_MSG = "No tickers found in response!";
    private static final String PRICE_TYPE_ASK = "ASK";
    private static final String PRICE_TYPE_BID = "BID";

    private static HttpTransport transport;
    private static HttpRequestFactory requestFactory;

    private final Map<ExchangeName, Map<String, String>> symbolAdditionalData = new ConcurrentHashMap<>();

    private final ExchangeService exchangeService;

    public RestClient(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    private static HttpTransport transport() {
        if (null == transport) {
            transport = new NetHttpTransport();
        }
        return transport;
    }

    private static HttpRequestFactory reqFactory() {
        if (null == requestFactory) {
            requestFactory = transport().createRequestFactory();
        }
        return requestFactory;
    }

    @Override
    public void afterPropertiesSet() {
        Set<Exchange> exchanges = exchangeService.loadAll();
        pingAll(exchanges);
        fetchAllAdditionalSymbolData(exchanges);
    }

    public void pingAll(Set<Exchange> exchanges) {
        exchanges.forEach(exchange -> {
            try {
                ping(exchange);
            } catch (IOException e) {
                log.error("Unable test connection with Exchange: {}! Caused by: {}", exchange.getFullName(),
                          e.getMessage());
            }
        });
    }

    public void ping(Exchange exchange) throws IOException {
        String baseEndpoint = exchange.getBaseEndpoint();
        String pingUrl;
        ExchangeName exchangeName = exchange.getName();
        String fullName = exchangeName.getFullName();
        switch (exchangeName) {
            case BITFINEX:
                pingUrl = baseEndpoint + "platform/status";
                break;
            case BITHUMB:
                pingUrl = baseEndpoint + "serverTime";
                break;
            case BITBAY:
            case BITMAX:
            case COINONE:
            case EXMO:
            case GATE:
                log.warn("Exchange: {} does not have a dedicated endpoint to check connection!", fullName);
                return;
            default:
                pingUrl = baseEndpoint + "ping";
        }

        HttpRequest req = reqFactory().buildGetRequest(new GenericUrl(pingUrl));
        int responseCode = req.execute().getStatusCode();
        log.info("Checked connection with Exchange: {}. Status code: {}", fullName, responseCode);
    }

    public void fetchAllAdditionalSymbolData(Set<Exchange> exchanges) {
        exchanges.forEach(exchange -> {
            try {
                fetchAdditionalSymbolData(exchange);
            } catch (IOException e) {
                log.error("Unable to fetch additional symbol data for Exchange: {}! Caused by: {}",
                          exchange.getFullName(), e.getMessage());
            }
        });
    }

    public void fetchAdditionalSymbolData(Exchange exchange) throws IOException {
        switch (exchange.getName()) {
            case BW: {
                fetchBWAdditionalSymbolData(exchange);
                break;
            }

            case BINANCE: {
                fetchBinanceAdditionalSymbolData(exchange);
                break;
            }
            default:
                break;
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
                case BIGONE:
                    result = fetchBigONETickers(exchange);
                    break;
                case BINANCE:
                    result = fetchBinanceTickers(exchange);
                    break;
                case BITBAY:
                    result = fetchBitBayTickers(exchange);
                    break;
                case BITFINEX:
                    result = fetchBitfinexTickers(exchange);
                    break;
                case BITHUMB:
                    result = fetchBithumbTickers(exchange);
                    break;
                case BITMART:
                    result = fetchBitmartTickers(exchange);
                    break;
                case BITMAX:
                    result = fetchBitmaxTickers(exchange);
                    break;
                case BITTREX:
                    result = fetchBittrexTickers(exchange);
                    break;
                case BW:
                    result = fetchBWTickers(exchange);
                    break;
                case COINONE:
                    result = fetchCoinoneTickers(exchange);
                    break;
                case EXMO:
                    result = fetchExmoTickers(exchange);
                    break;
                case GATE:
                    result = fetchGateTickers(exchange);
            }
            log.trace("Successfully fetched {} tickers from {} exchange", result.size(), exchange.getFullName());
            if (exchange.isFaulty()) {
                exchangeService.updateCachedExchangeFault(exchange, false);
            }
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.error(e.getMessage());
            exchangeService.updateCachedExchangeFault(exchange, true);
        } catch (IOException e) {
            log.error("Unable to fetch tickers on {} exchange! Details: {}", exchangeName, e.toString());
            exchangeService.updateCachedExchangeFault(exchange, true);
        }

        return result;
    }

    private void fetchBinanceAdditionalSymbolData(Exchange exchange) throws IOException {
        Map<String, String> data = fetchBinanceSymbols(exchange);
        symbolAdditionalData.put(ExchangeName.BINANCE, data);
    }

    private void fetchBWAdditionalSymbolData(Exchange exchange) throws IOException {
        Map<String, String> data = fetchBWMarketIdPairData(exchange);
        symbolAdditionalData.put(ExchangeName.BW, data);
    }

    private Map<String, String> fetchBWMarketIdPairData(Exchange bwExchange) throws IOException {
        String exchangeName = bwExchange.getFullName();
        String resp = executeRequest(bwExchange.getBaseEndpoint(),
                                     "exchange/config/controller/website/marketcontroller/getByWebId", exchangeName);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        JsonNode statusNode = parentNode.get("resMsg");
        int code = statusNode.get("code").asInt();
        if (code != 1) {
            String message = statusNode.get("message").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, String.valueOf(code), message);
        }
        JsonNode dataNode = parentNode.get("datas");
        if (dataNode.isEmpty() || "null".equalsIgnoreCase(dataNode.asText())) {
            throw new UnexpectedEndpointResponseException(exchangeName, "No symbol data found in response!");
        }
        Map<String, String> result = new HashMap<>();
        for (JsonNode innerNode : dataNode) {
            String pair = innerNode.get("name").asText();
            int state = innerNode.get("state").asInt();
            if (state != 1) {
                log.trace("Symbol {} on {} exchange has unacceptable state {}. Skipping symbol info...", pair,
                          exchangeName, state);
                continue;
            }
            String marketId = innerNode.get("marketId").asText();
            result.put(marketId, pair);
        }
        return result;
    }

    private Map<String, String> fetchBinanceSymbols(Exchange binanceExchange) throws IOException {
        String exchangeName = binanceExchange.getFullName();
        String resp = executeRequest(binanceExchange.getBaseEndpoint(), "exchangeInfo", exchangeName);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        JsonNode symbolsNode = parentNode.get("symbols");
        if (symbolsNode == null || symbolsNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, "No symbol data found in response!");
        }
        Map<String, String> result = new HashMap<>();
        for (JsonNode innerNode : symbolsNode) {
            String pair = innerNode.get("symbol").asText();
            String status = innerNode.get("status").asText();
            if (List.of("HALT", "BREAK").contains(status)) {
                log.trace("Symbol {} on {} exchange has unacceptable status {}. Skipping symbol info...", pair,
                          exchangeName, status);
                continue;
            }
            String base = innerNode.get("baseAsset").asText();
            String target = innerNode.get("quoteAsset").asText();
            result.put(pair, base + Constants.DEFAULT_PAIR_DELIMITER + target);
        }
        return result;
    }

    /**
     * @param exchange
     *
     * @return
     *
     * @see <a href="https://open.big.one/docs/spot_tickers.html#ticker">BigONE REST API - Ticker</a>
     */
    private Set<Ticker> fetchBigONETickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "asset_pairs/tickers", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        JsonNode statusNode = parentNode.get("code");
        String status = statusNode.asText();
        if (!Objects.equals(status, "0")) {
            String message = statusNode.get("message").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, status, message);
        }
        JsonNode dataNode = parentNode.get("data");
        if (dataNode == null || dataNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : dataNode) {
            Ticker ticker = new Ticker(ExchangeName.BIGONE);
            boolean parsePairResult = setSymbols(innerNode.get("asset_pair_name").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            JsonNode askNode = innerNode.get("ask");
            if (askNode == null || askNode.get("price") == null) {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_ASK);
                continue;
            }
            ticker.setPriceAsk(new BigDecimal(askNode.get("price").asText()));
            JsonNode bidNode = innerNode.get("bid");
            if (bidNode == null || bidNode.get("price") == null) {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_BID);
                continue;
            }
            ticker.setPriceBid(new BigDecimal(bidNode.get("price").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see
     * <a href="https://github.com/binance-exchange/binance-official-api-docs/blob/master/rest-api.md#symbol-order-book-ticker">Binance
     * REST API - Order Book</a>
     */
    private Set<Ticker> fetchBinanceTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "ticker/bookTicker", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        JsonNode statusNode = parentNode.get("code");

        if (statusNode != null) {
            String message = statusNode.get("msg").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, statusNode.asText(), message);
        }
        if (parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            Ticker ticker = new Ticker(ExchangeName.BINANCE);
            String pair = innerNode.get("symbol").asText();
            String dividedPair = symbolAdditionalData.get(exchange.getName()).get(pair);
            if (dividedPair == null) {
                continue;
            }
            boolean parsePairResult = setSymbols(dividedPair, ticker);
            if (!parsePairResult) {
                continue;
            }
            if (innerNode.get("askPrice") != null) {
                ticker.setPriceAsk(new BigDecimal(innerNode.get("askPrice").asText()));
            } else {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_ASK);
                continue;
            }
            if (innerNode.get("bidPrice") != null) {
                ticker.setPriceBid(new BigDecimal(innerNode.get("bidPrice").asText()));
            } else {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_BID);
                continue;
            }
            tickers.add(ticker);
        }
        return tickers;
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
        JsonNode statusNode = parentNode.get("status");

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
            boolean parsePairResult = setSymbols(innerNode.get("market").get("code").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            String priceBid = innerNode.get("highestBid").asText();
            if (StringUtils.isEmpty(priceBid) || "null".equalsIgnoreCase(priceBid)) {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_BID);
                continue;
            }
            String priceAsk = innerNode.get("lowestAsk").asText();
            if (StringUtils.isEmpty(priceAsk) || "null".equalsIgnoreCase(priceAsk)) {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_ASK);
                continue;
            }
            ticker.setPriceBid(new BigDecimal(priceBid));
            ticker.setPriceAsk(new BigDecimal(priceAsk));
            setTickerDateTime(innerNode, ticker, "time", ChronoUnit.MILLIS);
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see <a href="https://docs.bitfinex.com/reference#rest-public-tickers">Bitfinex REST API - Tickers</a>
     */
    private Set<Ticker> fetchBitfinexTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "tickers?symbols=ALL", exchangeName);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        if (parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode first = parentNode.get(0);
        if (first != null && "error".equalsIgnoreCase(first.asText())) {
            throw new UnexpectedEndpointResponseException(exchangeName, parentNode.get(2).asText(),
                                                          parentNode.get(3).asText());
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            if (innerNode == null || innerNode.isEmpty() || innerNode.size() < 4) {
                log.trace("Inner node does not contain full ticker data in response for exchange {}!", exchangeName);
                continue;
            }
            String pair = innerNode.get(0).asText();
            String prefix = "t";
            if (!pair.startsWith(prefix)) {
                continue;
            }
            Ticker ticker = new Ticker(ExchangeName.BITFINEX);
            String target = Constants.BITFINEX_TARGET_SYMBOLS.stream().filter(pair::endsWith).findFirst().orElse(null);
            if (target == null) {
                continue;
            }
            String base = pair.replace(prefix, "").replace(":", "").replace(target, "");
            ticker.setBase(base);
            ticker.setTarget(target);
            ticker.setPriceBid(new BigDecimal(innerNode.get(1).asText()));
            ticker.setPriceAsk(new BigDecimal(innerNode.get(3).asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see
     * <a href="https://github.com/bithumb-pro/bithumb.pro-official-api-docs/blob/master/rest-api.md#1-ticker">Bithumb
     * REST API - Ticker</a>
     */
    private Set<Ticker> fetchBithumbTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "spot/ticker?symbol=ALL", exchangeName);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        int code = parentNode.get("code").asInt();
        if (code != 0) {
            String message = parentNode.get("msg").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, String.valueOf(code), message);
        }
        JsonNode data = parentNode.get("data");
        if (data == null || data.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : data) {
            Ticker ticker = new Ticker(ExchangeName.BITHUMB);
            boolean parsePairResult = setSymbols(innerNode.get("s").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            //TODO: use orderbook
            ticker.setPriceAsk(new BigDecimal(innerNode.get("c").asText()));
            ticker.setPriceBid(new BigDecimal(innerNode.get("c").asText()));
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
        String resp = executeRequest(exchange.getBaseEndpoint(), "ticker", exchangeName);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode message = parentNode.get("message");
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
            ticker.setPriceAsk(new BigDecimal(innerNode.get("ask_1").asText()));
            ticker.setPriceBid(new BigDecimal(innerNode.get("bid_1").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     *
     * @see <a href="https://bitmax-exchange.github.io/bitmax-pro-api/#ticker">BitMax REST API - Ticker</a>
     */
    private Set<Ticker> fetchBitmaxTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "ticker", exchangeName);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);

        int code = parentNode.get("code").asInt();
        if (code != 0) {
            String message = parentNode.get("message").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, String.valueOf(code), message);
        }
        JsonNode data = parentNode.get("data");
        if (data == null || data.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : data) {
            Ticker ticker = new Ticker(ExchangeName.BITMAX);
            boolean parsePairResult = setSymbols(innerNode.get("symbol").asText(), "/", ticker);
            if (!parsePairResult) {
                continue;
            }
            JsonNode askNode = innerNode.get("ask");
            if (askNode == null || askNode.isEmpty()) {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_ASK);
                continue;
            }
            ticker.setPriceAsk(new BigDecimal(askNode.get(0).asText()));
            JsonNode bidNode = innerNode.get("bid");
            if (bidNode == null || bidNode.isEmpty()) {
                logInvalidPriceData(exchangeName, ticker.getPair(), PRICE_TYPE_BID);
                continue;
            }
            ticker.setPriceBid(new BigDecimal(bidNode.get(0).asText()));
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
        JsonNode codeResp = parentNode.get("code");
        if (codeResp != null) {
            String code = codeResp.asText();
            String message = parentNode.get("detail") != null ? parentNode.get("detail").asText() : "";
            throw new UnexpectedEndpointResponseException(exchangeName, code, message);
        }
        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            Ticker ticker = new Ticker(ExchangeName.BITTREX);
            boolean parsePairResult = setSymbols(innerNode.get("symbol").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
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
     * <a href="https://github.com/bw-exchange/api_docs_en/wiki/REST_api_reference#interface-description-get--tickers-single-symbol">BW
     * REST API - Ticker</a>
     */
    private Set<Ticker> fetchBWTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "api/data/v1/tickers", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        JsonNode statusNode = parentNode.get("resMsg");
        int code = statusNode.get("code").asInt();
        if (code != 1) {
            String message = statusNode.get("message").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, String.valueOf(code), message);
        }
        JsonNode dataNode = parentNode.get("datas");
        if (dataNode.isEmpty() || "null".equalsIgnoreCase(dataNode.asText())) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }

        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : dataNode) {
            String marketId = innerNode.get(0).asText();
            String pair = symbolAdditionalData.get(exchange.getName()).get(marketId);
            if (StringUtils.isEmpty(pair)) {
                log.warn("{} exchange: Cannot find pair by marketId ({}). Skipping...", exchangeName, marketId);
                continue;
            }
            Ticker ticker = new Ticker(ExchangeName.BW);
            boolean parsePairResult = setSymbols(pair, "_", ticker);
            if (!parsePairResult) {
                continue;
            }
            //TODO: use orderbook
            ticker.setPriceBid(new BigDecimal(innerNode.get(1).asText()));
            ticker.setPriceAsk(new BigDecimal(innerNode.get(1).asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * NOTE: only KRW target symbol is supported!
     *
     * @param exchange
     *
     * @see <a href="https://doc.coinone.co.kr/#operation/public_api_ticker_utc">Coinone REST API - Ticker</a>
     */
    private Set<Ticker> fetchCoinoneTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "ticker_utc?currency=all", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        int code = parentNode.get("errorCode").asInt();
        if (code != 0) {
            String errorMsg = parentNode.get("errorMsg").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, String.valueOf(code), errorMsg);
        }

        Set<Ticker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            if (innerNode.isTextual()) {
                continue;
            }
            Ticker ticker = new Ticker(ExchangeName.COINONE);
            ticker.setBase(innerNode.get("currency").asText().toUpperCase());
            ticker.setTarget("KRW");
            //TODO: use orderbook
            ticker.setPriceBid(new BigDecimal(innerNode.get("last").asText()));
            ticker.setPriceAsk(new BigDecimal(innerNode.get("last").asText()));
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
        String resp = executeRequest(exchange.getBaseEndpoint(), "ticker", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode errorNode = parentNode.get("error");
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
     * @see
     * <a href="https://www.gate.io/en/api2#tickers">GATE.IO REST API - Tickers</a>
     */
    private Set<Ticker> fetchGateTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "tickers", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        if (parentNode == null || parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        JsonNode errorCodeNode = parentNode.get("code");
        if (errorCodeNode != null) {
            throw new UnexpectedEndpointResponseException(exchangeName, errorCodeNode.asText(), parentNode.get("message").asText());
        }
        Set<Ticker> tickers = new HashSet<>();
        parentNode.fields().forEachRemaining(entry -> {
            Ticker ticker = new Ticker(ExchangeName.GATE);
            boolean parsePairResult = setSymbols(entry.getKey(), "_", ticker);
            if (!parsePairResult) {
                return;
            }
            JsonNode innerNode = entry.getValue();
            ticker.setPriceAsk(new BigDecimal(innerNode.get("lowestAsk").asText()));
            ticker.setPriceBid(new BigDecimal(innerNode.get("highestBid").asText()));
            tickers.add(ticker);
        });
        return tickers;
    }

    private String executeRequest(String baseUrl, String endpointUrl, String exchangeName) throws IOException {
        String fullUrl = baseUrl + endpointUrl;
        HttpRequest req = reqFactory().buildGetRequest(new GenericUrl(fullUrl));
        HttpResponse response = req.execute();
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            throw new UnexpectedResponseStatusCodeException(exchangeName, statusCode, fullUrl);
        }
        return response.parseAsString();
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
            dateTime = DateUtils.getDateTimeFromEpochSecond(epoch);
        } else {
            dateTime = DateUtils.getDateTimeFromEpochMilli(epoch);
        }
        if (dateTime.isBefore(DateUtils.currentDateTime())) {
            ticker.setDateTime(dateTime);
        }
    }
}
