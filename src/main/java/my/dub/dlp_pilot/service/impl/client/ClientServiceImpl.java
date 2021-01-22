package my.dub.dlp_pilot.service.impl.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.util.CollectionUtils;

/**
 * An implementation of {@link ClientService} service.
 */
@Slf4j
@Service
public class ClientServiceImpl implements ClientService {

    private static final String EXCHANGE_NAME_PARAMETER = "exchangeName";

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
        checkNotNull(exchangeNames, Constants.NULL_ARGUMENT_MESSAGE, "exchangeNames");

        List<SymbolPair> allSymbolPairs = exchangeNames.stream().map(this::loadSymbolPairs).flatMap(Collection::stream)
                .collect(Collectors.toList());
        log.info("Loaded {} symbol pairs", allSymbolPairs.size());
        Set<SymbolPair> relevantSymbolPairs = findRelevantSymbolPairs(allSymbolPairs);
        if (CollectionUtils.isEmpty(relevantSymbolPairs)) {
            throw new TestRunEndException("No relevant symbol pairs were found for Preload! Exiting...");
        }
        log.info("Filtered {} relevant symbol pairs for preload", relevantSymbolPairs.size());
        symbolPairContainer.addAll(relevantSymbolPairs);
    }

    @Override
    public int getSymbolPairsCount(@NonNull ExchangeName exchangeName) {
        return symbolPairContainer
                .size(checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER));
    }

    @Override
    public void removeSymbolPair(@NonNull ExchangeName exchangeName, int index) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);
        checkArgument(index >= 0, "Invalid symbol pair index passed for removal!");

        symbolPairContainer.remove(exchangeName, index);
    }

    @Override
    public Set<Ticker> fetchTickers(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);

        ExchangeClientService exchangeClientService = getExchangeClientService(exchangeName);
        Set<Ticker> tickers = new HashSet<>();
        try {
            tickers = exchangeClientService.fetchAllTickers(symbolPairContainer.getAll(exchangeName));
            log.trace("Successfully fetched {} tickers from {} exchange", tickers.size(), exchangeName.getFullName());
            if (exchangeService.isExchangeFaulty(exchangeName)) {
                log.info("Fault for {} exchange was resolved", exchangeName);
                exchangeService.updateExchangeFault(exchangeName, false);
            }
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            if (!exchangeService.isExchangeFaulty(exchangeName)) {
                log.warn(e.getMessage());
                exchangeService.updateExchangeFault(exchangeName, true);
            }
        } catch (IOException e) {
            if (!exchangeService.isExchangeFaulty(exchangeName)) {
                log.error("Unable to fetch tickers on {} exchange! Details: {}", exchangeName, e.toString());
                exchangeService.updateExchangeFault(exchangeName, true);
            }
        }
        return tickers;
    }

    @Override
    public List<Bar> fetchBarsPreload(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame,
            @NonNull Instant startTime, int symbolPairIndex, @NonNull Instant endTime) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);
        checkNotNull(timeFrame, Constants.NULL_ARGUMENT_MESSAGE, "timeFrame");
        checkNotNull(startTime, Constants.NULL_ARGUMENT_MESSAGE, "startTime");
        checkNotNull(endTime, Constants.NULL_ARGUMENT_MESSAGE, "endTime");

        ExchangeClientService exchangeClientService = getExchangeClientService(exchangeName);
        SymbolPair symbolPair = symbolPairContainer.get(exchangeName, symbolPairIndex);
        List<Bar> fetchedBars = new ArrayList<>();
        try {
            fetchedBars = exchangeClientService.fetchBars(symbolPair, timeFrame, startTime, endTime);
            log.trace("Successfully fetched {} bars from {} exchange", fetchedBars.size(), exchangeName.getFullName());
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.warn("{} Symbol pair {} {} will be excluded from preload and future trading!", e.getMessage(),
                     symbolPair.getPair(), symbolPair.getPair().equalsIgnoreCase(symbolPair.getName())
                             ? ""
                             : "(" + symbolPair.getName() + ")");
        } catch (IOException e) {
            log.error("Unable to fetch bars on {} exchange! Details: {}", exchangeName, e.toString());
            throw new TestRunEndException(e);
        }
        return fetchedBars;
    }

    @Override
    public List<Bar> fetchBars(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame, int symbolPairIndex,
            @NonNull Collection<LastBar> lastBars) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);
        checkNotNull(timeFrame, Constants.NULL_ARGUMENT_MESSAGE, "timeFrame");
        checkNotNull(lastBars, Constants.NULL_ARGUMENT_MESSAGE, "lastBars");

        ExchangeClientService exchangeClientService = getExchangeClientService(exchangeName);
        SymbolPair symbolPair = symbolPairContainer.get(exchangeName, symbolPairIndex);
        List<Bar> fetchedBars = new ArrayList<>();
        LastBar lastBar = lastBars.stream().filter(lB -> lB.isSimilar(symbolPair)).findFirst().orElse(null);
        if (lastBar == null) {
            log.error("No similar Last bar was found for base:{} target:{}", symbolPair.getBase(),
                      symbolPair.getTarget());
            return fetchedBars;
        }
        if (!DateUtils.isCurrentDurationLonger(lastBar.getCloseTime(), timeFrame.getDuration())) {
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
            log.warn("{} Symbol pair: {} ", e.getMessage(), symbolPair.getPair());
        } catch (IOException e) {
            log.error("Unable to fetch bars on {} exchange! Details: {}", exchangeName, e.toString());
        }
        return fetchedBars;
    }

    @Override
    public void updateLoadedSymbolPairs() {
        List<SymbolPair> allSymbolPairs = symbolPairContainer.getAll();
        Set<SymbolPair> relevantSymbolPairs = findRelevantSymbolPairs(allSymbolPairs);
        if (CollectionUtils.isEmpty(relevantSymbolPairs)) {
            throw new TestRunEndException("No relevant symbol pairs were found for Test Run! Exiting...");
        }
        symbolPairContainer.removeAll();
        symbolPairContainer.addAll(relevantSymbolPairs);
        log.info("Filtered {} relevant symbol pairs for Test Run", relevantSymbolPairs.size());
    }

    private List<SymbolPair> loadSymbolPairs(ExchangeName exchangeName) {
        try {
            List<SymbolPair> symbolPairs = getExchangeClientService(exchangeName).fetchSymbolPairs();
            log.trace("Successfully fetched {} symbol pairs from {} exchange", symbolPairs.size(),
                      exchangeName.getFullName());
            return symbolPairs;
        } catch (IOException e) {
            log.error("Unable to fetch additional symbol data for Exchange: {}! Caused by: {}",
                      exchangeName.getFullName(), e.getMessage());
            throw new TestRunEndException(e);
        } catch (UnexpectedEndpointResponseException | UnexpectedResponseStatusCodeException e) {
            log.error(e.getMessage());
            throw new TestRunEndException(e);
        }
    }

    private Set<SymbolPair> findRelevantSymbolPairs(List<SymbolPair> symbolPairs) {
        Set<SymbolPair> relevantSP = new HashSet<>();
        for (int i = 0; i < symbolPairs.size() - 1; i++) {
            for (int j = i + 1; j < symbolPairs.size(); j++) {
                SymbolPair sP1 = symbolPairs.get(i);
                SymbolPair sP2 = symbolPairs.get(j);
                if (relevantSP.contains(sP2)) {
                    continue;
                }
                if (!sP1.getExchangeName().equals(sP2.getExchangeName()) && sP1.getBase().equals(sP2.getBase()) && sP1
                        .getTarget().equals(sP2.getTarget())) {
                    relevantSP.add(sP1);
                    relevantSP.add(sP2);
                }
            }
        }
        return relevantSP;
    }

    private ExchangeClientService getExchangeClientService(ExchangeName exchangeName) {
        return exchangeClientServices.get(exchangeName.getSimpleName() + Constants.CLIENT_SERVICE_BEAN_NAME_SUFFIX);
    }
}
