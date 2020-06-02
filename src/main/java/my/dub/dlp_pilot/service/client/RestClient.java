package my.dub.dlp_pilot.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.rest.UnexpectedEndpointResponseException;
import my.dub.dlp_pilot.exception.rest.UnexpectedResponseStatusCodeException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.client.*;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RestClient implements InitializingBean {

    private static final String NO_TICKERS_FOUND_IN_RESPONSE_MSG = "No tickers found in response!";
    private static HttpTransport transport;
    private static HttpRequestFactory requestFactory;

    private final Map<ExchangeName, Map<String, String>> symbolAdditionalData = new ConcurrentHashMap<>();

    private final ExchangeService exchangeService;

    public RestClient(ExchangeService exchangeService) {this.exchangeService = exchangeService;}

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
        Set<Exchange> exchanges = exchangeService.findAll();
        pingAll(exchanges);
        fetchAllAdditionalSymbolData(exchanges);

        //TODO: remove
        //exchanges.forEach(this::fetchTickers);
    }

    public void pingAll(Set<Exchange> exchanges) {
        exchanges.forEach(exchange -> {
            try {
                ping(exchange);
            }
            catch (IOException e) {
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
                log.warn("Exchange: {} does not have a dedicated endpoint to check connection!",
                         fullName);
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
            }
            catch (IOException e) {
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
        String resp = executeRequest(
                bwExchange.getBaseEndpoint(), "exchange/config/controller/website/marketcontroller/getByWebId",
                exchangeName);

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
                log.debug("Symbol {} on {} exchange has unacceptable state {}. Skipping symbol info...", pair,
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
                log.debug("Symbol {} on {} exchange has unacceptable status {}. Skipping symbol info...", pair,
                          exchangeName, status);
                continue;
            }
            String base = innerNode.get("baseAsset").asText();
            String target = innerNode.get("quoteAsset").asText();
            result.put(pair, base + Constants.DEFAULT_PAIR_DELIMITER + target);
        }
        return result;
    }

    public Set<? extends Ticker> fetchTickers(Exchange exchange) {
        Set<? extends Ticker> result = new HashSet<>();
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
            }
            log.debug("Successfully fetched {} tickers from {} exchange", result.size(), exchange.getFullName());
        }
        catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.error(e.getMessage());
        }
        catch (IOException e) {
            log.error("Unable to fetch tickers on {} exchange! Details: {}", exchangeName, e.getMessage());
        }

        return result;
    }

    /**
     * @param exchange
     * @return
     * @see <a href="https://open.big.one/docs/spot_tickers.html#ticker">BigONE REST API - Ticker</a>
     */
    private Set<BigONETicker> fetchBigONETickers(Exchange exchange) throws IOException {
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
        Set<BigONETicker> tickers = new HashSet<>();
        for (JsonNode innerNode : dataNode) {
            BigONETicker ticker = new BigONETicker();
            boolean parsePairResult = setSymbols(innerNode.get("asset_pair_name").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setPrice(new BigDecimal(innerNode.get("close").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://github.com/binance-exchange/binance-official-api-docs/blob/master/rest-api.md#symbol-price-ticker">Binance REST API - Ticker</a>
     */
    private Set<BinanceTicker> fetchBinanceTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "ticker/price", exchangeName);
        JsonNode parentNode = new ObjectMapper().readTree(resp);
        JsonNode statusNode = parentNode.get("code");

        if (statusNode != null) {
            String message = statusNode.get("msg").asText();
            throw new UnexpectedEndpointResponseException(exchangeName, statusNode.asText(), message);
        }
        if (parentNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<BinanceTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            BinanceTicker ticker = new BinanceTicker();
            String pair = innerNode.get("symbol").asText();
            String dividedPair = symbolAdditionalData.get(exchange.getName()).get(pair);
            if (dividedPair == null) {
                continue;
            }
            boolean parsePairResult = setSymbols(dividedPair, ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setPrice(new BigDecimal(innerNode.get("price").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://docs.bitbay.net/v1.0.1-en/reference#ticker-1">BitBay REST API - Ticker</a>
     */
    private Set<BitBayTicker> fetchBitBayTickers(Exchange exchange) throws IOException {
        String exchangeName = exchange.getFullName();
        String resp = executeRequest(exchange.getBaseEndpoint(), "trading/ticker", exchangeName);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parentNode = objectMapper.readTree(resp);
        JsonNode statusNode = parentNode.get("status");

        String status = statusNode.asText();
        if (!"Ok".equalsIgnoreCase(status)) {
            List<String> errors = objectMapper.convertValue(parentNode.get("errors"), ArrayList.class);
            String message = String.join("; ", errors);
            throw new UnexpectedEndpointResponseException(exchangeName, status, message);
        }

        JsonNode itemsNode = parentNode.get("items");
        if (itemsNode == null || itemsNode.isEmpty()) {
            throw new UnexpectedEndpointResponseException(exchangeName, NO_TICKERS_FOUND_IN_RESPONSE_MSG);
        }
        Set<BitBayTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : itemsNode) {
            BitBayTicker ticker = new BitBayTicker();
            boolean parsePairResult = setSymbols(innerNode.get("market").get("code").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            String price = innerNode.get("highestBid").asText();
            if (StringUtils.isEmpty(price) || "null".equalsIgnoreCase(price)) {
                log.debug("Unable to fetch price data for pair: {} on exchange: {}. Skipping...", ticker.getPair(),
                          exchangeName);
                continue;
            }
            ticker.setPrice(new BigDecimal(price));
            ticker.setDateTime(DateUtils.getDateTimeFromEpoch(innerNode.get("time").asLong()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://docs.bitfinex.com/reference#rest-public-tickers">Bitfinex REST API - Tickers</a>
     */
    private Set<BitfinexTicker> fetchBitfinexTickers(Exchange exchange) throws IOException {
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
        Set<BitfinexTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            String pair = innerNode.get(0).asText();
            if (!pair.startsWith("t")) {
                continue;
            }
            BitfinexTicker ticker = new BitfinexTicker();
            String target = Constants.BITFINEX_TARGET_SYMBOLS.stream().filter(pair::endsWith).findFirst().orElse(null);
            if (target == null) {
                continue;
            }
            String base = pair.replace(target, "");
            ticker.setBase(base);
            ticker.setTarget(target);
            ticker.setPrice(new BigDecimal(innerNode.get(1).asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://github.com/bithumb-pro/bithumb.pro-official-api-docs/blob/master/rest-api.md#1-ticker">Bithumb REST API - Ticker</a>
     */
    private Set<BithumbTicker> fetchBithumbTickers(Exchange exchange) throws IOException {
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
        Set<BithumbTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : data) {
            BithumbTicker ticker = new BithumbTicker();
            boolean parsePairResult = setSymbols(innerNode.get("s").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setPrice(new BigDecimal(innerNode.get("c").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://developer.bitmart.com/v2/en/?http#spot-api-public-endpoints-ticker">Bitmart REST API - Ticker</a>
     */
    private Set<BitmartTicker> fetchBitmartTickers(Exchange exchange) throws IOException {
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
        Set<BitmartTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            BitmartTicker ticker = new BitmartTicker();
            boolean parsePairResult = setSymbols(innerNode.get("symbol_id").asText(), "_", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setPrice(new BigDecimal(innerNode.get("current_price").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://bitmax-exchange.github.io/bitmax-pro-api/#ticker">BitMax REST API - Ticker</a>
     */
    private Set<BitMaxTicker> fetchBitmaxTickers(Exchange exchange) throws IOException {
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
        Set<BitMaxTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : data) {
            BitMaxTicker ticker = new BitMaxTicker();
            boolean parsePairResult = setSymbols(innerNode.get("symbol").asText(), "/", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setPrice(new BigDecimal(innerNode.get("close").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://bittrex.github.io/api/v3#operation--markets-tickers-get">Bittrex REST API - Ticker</a>
     */
    private Set<BittrexTicker> fetchBittrexTickers(Exchange exchange) throws IOException {
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
        Set<BittrexTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : parentNode) {
            BittrexTicker ticker = new BittrexTicker();
            boolean parsePairResult = setSymbols(innerNode.get("symbol").asText(), "-", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setPrice(new BigDecimal(innerNode.get("lastTradeRate").asText()));
            tickers.add(ticker);
        }
        return tickers;
    }

    /**
     * @param exchange
     * @see <a href="https://github.com/bw-exchange/api_docs_en/wiki/REST_api_reference#interface-description-get--tickers-single-symbol">BW REST API - Ticker</a>
     */
    private Set<BWTicker> fetchBWTickers(Exchange exchange) throws IOException {
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

        Set<BWTicker> tickers = new HashSet<>();
        for (JsonNode innerNode : dataNode) {
            String marketId = innerNode.get(0).asText();
            String pair = symbolAdditionalData.get(exchange.getName()).get(marketId);
            if (StringUtils.isEmpty(pair)) {
                log.warn("{} exchange: Cannot find pair by marketId ({}). Skipping...", exchangeName, marketId);
                continue;
            }
            BWTicker ticker = new BWTicker();
            boolean parsePairResult = setSymbols(pair, "_", ticker);
            if (!parsePairResult) {
                continue;
            }
            ticker.setPrice(new BigDecimal(innerNode.get(1).asText()));
            tickers.add(ticker);
        }
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
            ticker.setBase(symbols[0]);
            ticker.setTarget(symbols[1]);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            log.debug("Incorrect pair input ({}) for {} exchange! Expecting to parse with split regex: {}", input,
                      ticker.getExchangeName().getFullName(), splitRegex);
            return false;
        }
        return true;
    }
}
