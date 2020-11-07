package my.dub.dlp_pilot.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.PriceData;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public abstract class AbstractExchangeClientService implements InitializingBean {

    protected final ExchangeService exchangeService;

    protected final ExchangeName exchangeName;
    protected final String exchangeFullName;

    protected Exchange exchange;

    protected AbstractExchangeClientService(ExchangeService exchangeService, ExchangeName exchangeName) {
        this.exchangeService = exchangeService;
        this.exchangeName = exchangeName;
        this.exchangeFullName = exchangeName.getFullName();
    }

    @Override
    public void afterPropertiesSet() {
        exchange = exchangeService.findByName(exchangeName);
    }

    protected void logInvalidPriceData(String pair, String priceType) {
        log.trace("Unable to fetch {} price data for pair: {} on exchange: {}. Skipping...", priceType, pair,
                  exchangeFullName);
    }

    protected Optional<BigDecimal> parsePrice(JsonNode priceNode) {
        if (priceNode == null) {
            log.trace("Null or empty price node found in response from {} exchange. Skipping...", exchangeFullName);
            return Optional.empty();
        }
        String price = priceNode.asText();
        try {
            return Optional.of(new BigDecimal(price).setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP));
        } catch (NumberFormatException e) {
            log.trace("Wrong price value found in response ({}) from {} exchange. Skipping...", price,
                      exchangeFullName);
            return Optional.empty();
        }
    }

    protected Optional<BigDecimal> parseVolume(JsonNode volumeNode) {
        if (volumeNode == null) {
            log.trace("Null or empty volume node found in response from {} exchange. Skipping...", exchangeFullName);
            return Optional.empty();
        }
        String volume = volumeNode.asText();
        try {
            return Optional.of(new BigDecimal(volume).setScale(Constants.VOLUME_SCALE, RoundingMode.HALF_UP));
        } catch (NumberFormatException e) {
            log.trace("Wrong volume value found in response ({}) from {} exchange. Skipping...", volume,
                      exchangeFullName);
            return Optional.empty();
        }
    }

    protected Optional<ZonedDateTime> parseDateTime(JsonNode dateTimeNode) {
        if (dateTimeNode == null) {
            log.trace("Null or empty dateTime node found in response from {} exchange. Skipping...", exchangeFullName);
            return Optional.empty();
        }
        String dateTimeStr = dateTimeNode.asText();

        try {
            if (dateTimeNode.isInt() || dateTimeNode.isLong()) {
                return Optional.of(DateUtils.dateTimeFromEpochMilli(dateTimeNode.asLong()));
            }
            return Optional.of(DateUtils.parseDefaultZoneDateTime(dateTimeStr));
        } catch (DateTimeParseException e) {
            log.trace("Wrong dateTime found in response ({}) from {} exchange. Skipping...", dateTimeStr,
                      exchangeFullName);
            return Optional.empty();
        }
    }

    protected void setTickerDateTime(JsonNode innerNode, Ticker ticker, String fieldName, ChronoUnit epochChronoUnit) {
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

    protected boolean setSymbols(String input, String splitRegex, PriceData priceData) {
        String[] symbols;
        try {
            symbols = input.split(splitRegex, 2);
            priceData.setBase(parseSymbol(symbols[0].toUpperCase()));
            priceData.setTarget(parseSymbol(symbols[1].toUpperCase()));
        } catch (ArrayIndexOutOfBoundsException e) {
            log.trace("Incorrect pair input ({}) for {} exchange! Expecting to parse with split regex: {}", input,
                      priceData.getExchangeName().getFullName(), splitRegex);
            return false;
        }
        return true;
    }

    protected String parseSymbol(String rawSymbol) {
        if (Constants.BITCOIN_SYMBOLS.contains(rawSymbol)) {
            return Constants.BITCOIN_SYMBOLS.get(0);
        }
        if (Constants.BITCOIN_CASH_SYMBOLS.contains(rawSymbol)) {
            return Constants.BITCOIN_CASH_SYMBOLS.get(0);
        }
        if (Constants.BITCOIN_SV_SYMBOLS.contains(rawSymbol)) {
            return Constants.BITCOIN_SV_SYMBOLS.get(0);
        }
        if (Constants.STELLAR_SYMBOLS.contains(rawSymbol)) {
            return Constants.STELLAR_SYMBOLS.get(0);
        }
        return rawSymbol;
    }

    protected long checkBarsLimit(long barsLimit) {
        Integer maxBarsPerRequest = exchange.getMaxBarsPerRequest();
        if (barsLimit > maxBarsPerRequest) {
            log.warn("Bars limit ({}) exceeds allowed size ({}). Setting to max value.", barsLimit, maxBarsPerRequest);
            barsLimit = maxBarsPerRequest;
        }
        return barsLimit;
    }
}
