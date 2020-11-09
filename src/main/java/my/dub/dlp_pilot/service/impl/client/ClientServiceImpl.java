package my.dub.dlp_pilot.service.impl.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.exception.client.UnexpectedEndpointResponseException;
import my.dub.dlp_pilot.exception.client.UnexpectedResponseStatusCodeException;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.LastBar;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.SymbolPairContainer;
import my.dub.dlp_pilot.service.ExchangeClientService;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.client.ClientService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClientServiceImpl implements ClientService {

    private final SymbolPairContainer symbolPairContainer;
    private final ExchangeService exchangeService;
    private final Map<String, ExchangeClientService> exchangeClientServices;

    @Autowired
    public ClientServiceImpl(SymbolPairContainer symbolPairContainer, ExchangeService exchangeService,
            Map<String, ExchangeClientService> exchangeClientServices) {
        this.symbolPairContainer = symbolPairContainer;
        this.exchangeService = exchangeService;
        this.exchangeClientServices = exchangeClientServices;
    }

    @Override
    public void loadAllSymbolPairs(@NonNull Collection<ExchangeName> exchangeNames) {
        checkNotNull(exchangeNames, "Cannot load all SymbolPairs if exchangeNames collection is null!");

        exchangeNames.forEach(this::loadSymbolPairs);
    }

    @Override
    public int getSymbolPairsCount(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, "Cannot get symbol pairs count if exchangeName is null!");

        return symbolPairContainer.size(exchangeName);
    }

    @Override
    public void removeSymbolPair(@NonNull ExchangeName exchangeName, int index) {
        checkNotNull(exchangeName, "Cannot remove symbol pair if exchangeName is null!");
        checkArgument(index >= 0, "Invalid symbol pair index passed for removal!");
        symbolPairContainer.remove(exchangeName, index);
    }

    @Override
    public Set<Ticker> fetchTickers(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, "Cannot fetch Tickers if exchangeName is null!");

        ExchangeClientService exchangeClientService = getExchangeClientService(exchangeName);
        Set<Ticker> tickers = new HashSet<>();
        try {
            tickers = exchangeClientService.fetchAllTickers(symbolPairContainer.getAll(exchangeName));
            log.trace("Successfully fetched {} tickers from {} exchange", tickers.size(), exchangeName.getFullName());
            exchangeService.updateExchangeFault(exchangeName, false);
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.warn(e.getMessage());
            exchangeService.updateExchangeFault(exchangeName, true);
        } catch (IOException e) {
            log.error("Unable to fetch tickers on {} exchange! Details: {}", exchangeName, e.toString());
            exchangeService.updateExchangeFault(exchangeName, true);
        }
        return tickers;
    }

    @Override
    public List<Bar> fetchBars(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame,
            @NonNull ZonedDateTime startTime, int symbolPairIndex, @NonNull ZonedDateTime endTime) {
        checkNotNull(exchangeName, "Cannot fetch Bars if exchangeName is null!");
        checkNotNull(timeFrame, "Cannot fetch Bars if timeFrame is null!");
        checkNotNull(startTime, "Cannot fetch Bars if startTime is null!");
        checkNotNull(startTime, "Cannot fetch Bars if endTime is null!");

        ExchangeClientService exchangeClientService = getExchangeClientService(exchangeName);
        SymbolPair symbolPair = symbolPairContainer.get(exchangeName, symbolPairIndex);
        List<Bar> fetchedBars = new ArrayList<>();
        try {
            fetchedBars = exchangeClientService.fetchBars(symbolPair, timeFrame, startTime, endTime);
            log.trace("Successfully fetched {} bars from {} exchange", fetchedBars.size(), exchangeName.getFullName());
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.warn("{} Symbol pair {} will be excluded from preload and future trading!", e.getMessage(),
                     symbolPair.getPair());
        } catch (IOException e) {
            log.error("Unable to fetch bars on {} exchange! Details: {}", exchangeName, e.toString());
            throw new TestRunEndException(e);
        }
        return fetchedBars;
    }

    @Override
    public List<Bar> fetchBars(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame, int symbolPairIndex,
            @NonNull Collection<LastBar> lastBars) {
        checkNotNull(exchangeName, "Cannot fetch single Bar if exchangeName is null!");
        checkNotNull(timeFrame, "Cannot fetch single Bar if timeFrame is null!");

        ExchangeClientService exchangeClientService = getExchangeClientService(exchangeName);
        SymbolPair symbolPair = symbolPairContainer.get(exchangeName, symbolPairIndex);
        List<Bar> fetchedBars = new ArrayList<>();
        LastBar lastBar = lastBars.stream().filter(lB -> lB.isSimilar(symbolPair)).findFirst().orElse(null);
        if (lastBar == null) {
            log.error("No similar Last bar was found for base:{} target:{}", symbolPair.getBase(),
                      symbolPair.getTarget());
            return fetchedBars;
        }
        if (!DateUtils
                .isDurationLonger(lastBar.getCloseTime(), DateUtils.currentDateTimeUTC(), timeFrame.getDuration())) {
            return fetchedBars;
        }
        long barsToLoad = DateUtils.durationMillis(lastBar.getCloseTime()) / timeFrame.getDuration().toMillis();
        if (barsToLoad <= 0) {
            return fetchedBars;
        }
        try {
            fetchedBars = exchangeClientService.fetchBars(symbolPair, timeFrame, ++barsToLoad);
            log.trace("Successfully fetched {} bars from {} exchange", fetchedBars.size(), exchangeName.getFullName());
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.warn(e.getMessage());
        } catch (IOException e) {
            log.error("Unable to fetch bars on {} exchange! Details: {}", exchangeName, e.toString());
        }
        return fetchedBars;
    }

    private void loadSymbolPairs(ExchangeName exchangeName) {
        try {
            List<SymbolPair> symbolPairs = getExchangeClientService(exchangeName).fetchSymbolPairs();
            log.trace("Successfully fetched {} symbol pairs from {} exchange", symbolPairs.size(),
                      exchangeName.getFullName());
            symbolPairContainer.add(symbolPairs);
        } catch (IOException e) {
            log.error("Unable to fetch additional symbol data for Exchange: {}! Caused by: {}",
                      exchangeName.getFullName(), e.getMessage());
            throw new TestRunEndException(e);
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.error(e.getMessage());
            throw new TestRunEndException(e);
        }

    }

    private ExchangeClientService getExchangeClientService(ExchangeName exchangeName) {
        return exchangeClientServices.get(exchangeName.getSimpleName() + Constants.CLIENT_SERVICE_BEAN_NAME_SUFFIX);
    }
}