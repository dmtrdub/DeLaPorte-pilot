package my.dub.dlp_pilot.service.impl;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeDynamicResultData;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.model.client.Ticker;
import my.dub.dlp_pilot.repository.TradeRepository;
import my.dub.dlp_pilot.repository.container.CurrentTradeContainer;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.DateUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TradeServiceImpl implements TradeService {

    private final TradeRepository repository;
    private final CurrentTradeContainer tradeContainer;
    private final TickerService tickerService;
    private final ExchangeService exchangeService;
    private final ParametersComponent parameters;
    private final TestRunService testRunService;

    @Autowired
    public TradeServiceImpl(TradeRepository repository,
                            CurrentTradeContainer tradeContainer,
                            TickerService tickerService,
                            ExchangeService exchangeService,
                            ParametersComponent parameters, TestRunService testRunService) {
        this.repository = repository;
        this.tradeContainer = tradeContainer;
        this.tickerService = tickerService;
        this.exchangeService = exchangeService;
        this.parameters = parameters;
        this.testRunService = testRunService;
    }

    @Override
    public void searchForTrades(Exchange exchange) {
        if (testRunService.isTickerDataCapture() || testRunService.isTradeStopped() || testRunService.isTestRunEnd()) {
            return;
        }
        Set<Ticker> allTickers = tickerService.getAllTickers(true);
        Set<Ticker> tickersToCompare = tickerService.getTickers(exchange.getName());
        allTickers.removeAll(tickersToCompare);
        allTickers.forEach(ticker -> {
            Ticker equivalentTicker = tickerService.findEquivalentTickerFromSet(ticker, tickersToCompare);
            if (canEnterTrade(ticker, equivalentTicker)) {
                BigDecimal currentPercentageDiff =
                        Calculations.percentageDifference(ticker.getPrice(), equivalentTicker.getPrice());
                BigDecimal prevPercentageDiff = Calculations
                        .percentageDifference(ticker.getPreviousPrice(), equivalentTicker.getPreviousPrice());
                if (currentPercentageDiff.compareTo(parameters.getEntryMinPercentage()) < 0 ||
                        prevPercentageDiff.compareTo(parameters.getEntryMinPercentage()) >= 0 ||
                        currentPercentageDiff.compareTo(parameters.getEntryMaxPercentage()) > 0) {
                    return;
                }
                if (!checkProfitability(ticker, equivalentTicker)) {
                    return;
                }
                Assert.isTrue(ticker.getBase().equals(equivalentTicker.getBase()) &&
                                      ticker.getTarget().equals(equivalentTicker.getTarget()),
                              "Base and target should be equal!");
                Trade trade = createTrade(ticker, equivalentTicker, currentPercentageDiff);
                boolean added = tradeContainer.addTrade(trade);
                if (added) {
                    log.debug("New {} created and added to container", trade.toShortString());
                }
            }
        });
    }

    private boolean canEnterTrade(Ticker ticker, Ticker equivalentTicker) {
        if (equivalentTicker == null) {
            return false;
        }
        if (ticker.isStale() && equivalentTicker.isStale()) {
            return false;
        }
        if(Calculations.isNotPositive(ticker.getPrice()) || Calculations.isNotPositive(equivalentTicker.getPrice())) {
            return false;
        }
        if (tradeContainer.isSimilarPresent(ticker.getBase(), ticker.getTarget(), ticker.getExchangeName(),
                                            equivalentTicker.getExchangeName())) {
            return false;
        }
        Pair<Long, Long> tradesCount =
                tradeContainer.tradesCount(ticker.getExchangeName(), equivalentTicker.getExchangeName());
        int parallelTradesNumber = parameters.getParallelTradesNumber();
        return tradesCount.getFirst() <= parallelTradesNumber && tradesCount.getSecond() <=
                parallelTradesNumber;
    }

    private boolean checkProfitability(Ticker ticker, Ticker equivalentTicker) {
        BigDecimal priceLong;
        BigDecimal priceShort;
        if (ticker.getPrice().compareTo(equivalentTicker.getPrice()) > 0) {
            priceLong = equivalentTicker.getPrice();
            priceShort = ticker.getPrice();
        } else {
            priceLong = ticker.getPrice();
            priceShort = equivalentTicker.getPrice();
        }
        BigDecimal minEntryAmount = BigDecimal.valueOf(parameters.getEntryAmounts().get(0));
        BigDecimal expenses1 = exchangeService.getTotalExpenses(ticker.getExchangeName(), minEntryAmount);
        BigDecimal expenses2 = exchangeService.getTotalExpenses(equivalentTicker.getExchangeName(), minEntryAmount);
        BigDecimal expectedIncome = Calculations.expectedIncome(priceLong, priceShort, minEntryAmount,
                                                                parameters.getExitPercentageDiff(),
                                                                expenses1.add(expenses2));
        return expectedIncome.compareTo(BigDecimal.ZERO) >= 0;
    }

    @Override
    @Transactional
    @Retryable(LockAcquisitionException.class)
    public void handleTrades(ExchangeName exchangeName) {
        if (testRunService.isTickerDataCapture()) {
            return;
        }
        Set<Trade> tradesInProgress = tradeContainer.getTrades(exchangeName);
        Set<Trade> tradesCompleted = new HashSet<>();
        Set<Trade> tradesToSave = new HashSet<>();
        boolean isTestRunEnd = testRunService.isTestRunEnd();
        tradesInProgress.forEach(trade -> {
            Position positionShort = trade.getPositionShort();
            Position positionLong = trade.getPositionLong();
            Ticker tickerShort = tickerService
                    .getTicker(positionShort.getExchange().getName(), trade.getBase(), trade.getTarget())
                    .orElseThrow(() -> new NullPointerException(
                            String.format("Ticker for position (%s) in container is null!", positionShort.toString())));
            Ticker tickerLong = tickerService
                    .getTicker(positionLong.getExchange().getName(), trade.getBase(), trade.getTarget())
                    .orElseThrow(() -> new NullPointerException(
                            String.format("Ticker for position (%s) in container is null!", positionLong.toString())));
            if (isTestRunEnd) {
                closeAndAddToSets(tradesCompleted, tradesToSave, trade, tickerShort, tickerLong,
                                  TradeResultType.TEST_RUN_END);
            } else if (DateUtils.durationMinutes(trade.getStartTime()) > parameters.getTradeMinutesTimeout()) {
                closeAndAddToSets(tradesCompleted, tradesToSave, trade, tickerShort, tickerLong,
                                  TradeResultType.TIMED_OUT);
            } else {
                BigDecimal percentageDiff = Calculations.percentageDifference(tickerShort, tickerLong);
                BigDecimal entryPercentageDiff = trade.getEntryPercentageDiff();
                if (entryPercentageDiff.subtract(percentageDiff).compareTo(
                        parameters.getExitPercentageDiff(DateUtils.durationSeconds(trade.getStartTime()))) >= 0) {
                    closeAndAddToSets(tradesCompleted, tradesToSave, trade, tickerShort, tickerLong,
                                      TradeResultType.SUCCESSFUL);
                } else if (percentageDiff.subtract(entryPercentageDiff)
                                         .compareTo(parameters.getDetrimentPercentageDelta()) >= 0) {
                    closeAndAddToSets(tradesCompleted, tradesToSave, trade, tickerShort, tickerLong,
                                      TradeResultType.DETRIMENTAL);
                }
            }
        });
        if (!tradesToSave.isEmpty()) {
            repository.saveAll(tradesToSave);
            tradesToSave
                    .forEach(trade -> log.debug("{} closed. Reason: {}", trade.toShortString(), trade.getResultType()));
            if (isTestRunEnd) {
                log.info("Closed all opened trades for {} exchange due to the end of Test Run",
                         exchangeName.getFullName());
            }
        }
        if (!tradesCompleted.isEmpty()) {
            tradeContainer.removeTrades(tradesCompleted);
        }
    }

    private void closeAndAddToSets(Set<Trade> tradesCompleted, Set<Trade> tradesToSave, Trade trade, Ticker tickerShort,
                                   Ticker tickerLong, TradeResultType resultType) {
        Trade tradeCopy = SerializationUtils.clone(trade);
        closeTrade(tradeCopy, resultType, tickerShort, tickerLong);
        tradesCompleted.add(trade);
        tradesToSave.add(tradeCopy);
    }

    private void closeTrade(Trade trade, TradeResultType resultType, Ticker tickerShort, Ticker tickerLong) {
        BigDecimal priceShort = tickerShort.getPrice();
        Position positionShort = trade.getPositionShort();
        positionShort.setClosePrice(priceShort);

        BigDecimal priceLong = tickerLong.getPrice();
        Position positionLong = trade.getPositionLong();
        positionLong.setClosePrice(priceLong);

        trade.setEndTime(DateUtils.currentDateTime());
        trade.setResultType(resultType);
        List<TradeDynamicResultData> resultData =
                parameters.getEntryAmounts().stream()
                          .map(amount -> createDynamicResultData(trade, positionShort, positionLong, amount))
                          .collect(Collectors.toList());
        trade.setResultData(resultData);
    }

    private Trade createTrade(Ticker ticker1, Ticker ticker2, BigDecimal percentageDiff) {
        Trade trade = new Trade();
        trade.setBase(ticker1.getBase());
        trade.setTarget(ticker1.getTarget());
        trade.setEntryPercentageDiff(percentageDiff);
        BigDecimal price1 = ticker1.getPrice();
        BigDecimal price2 = ticker2.getPrice();
        Position longPos;
        Position shortPos;
        if (price1.compareTo(price2) > 0) {
            shortPos = createPosition(PositionSide.SHORT, ticker1);
            longPos = createPosition(PositionSide.LONG, ticker2);
        } else {
            longPos = createPosition(PositionSide.LONG, ticker1);
            shortPos = createPosition(PositionSide.SHORT, ticker2);
        }
        trade.setPositions(shortPos, longPos);
        trade.setResultType(TradeResultType.IN_PROGRESS);
        Exchange exchangeShort = shortPos.getExchange();
        Exchange exchangeLong = longPos.getExchange();
        trade.setFixedExpensesUsd(exchangeShort.getFixedFeesUsd().add(exchangeLong.getFixedFeesUsd()));
        trade.setTestRun(testRunService.getCurrentTestRun());
        trade.setWrittenToFile(false);
        trade.setStartTime(DateUtils.currentDateTime());
        return trade;
    }

    private Position createPosition(PositionSide side, Ticker ticker) {
        Position position = new Position();
        position.setSide(side);
        BigDecimal tickerPrice = ticker.getPrice();
        position.setOpenPrice(tickerPrice);
        ExchangeName exchangeName = ticker.getExchangeName();
        Exchange exchange = exchangeService.findByName(
                exchangeName).orElseThrow(
                () -> new NullPointerException(String.format("Exchange by name %s was not found!", exchangeName)));
        position.setExchange(exchange);
        return position;
    }

    private TradeDynamicResultData createDynamicResultData(Trade trade, Position positionShort, Position positionLong,
                                                           Double amount) {
        TradeDynamicResultData dynamicResultData = new TradeDynamicResultData();
        BigDecimal amountUsd = BigDecimal.valueOf(amount);
        dynamicResultData.setAmountUsd(amountUsd);
        dynamicResultData.setPnlUsdShort(Calculations.pnl(positionShort.getSide(), positionShort.getOpenPrice(),
                                                          positionShort.getClosePrice(), amountUsd));
        dynamicResultData.setPnlUsdLong(Calculations.pnl(positionLong.getSide(), positionLong.getOpenPrice(),
                                                         positionLong.getClosePrice(), amountUsd));
        BigDecimal variableExpenses = Calculations.originalValueFromPercentSum(amountUsd, positionShort.getExchange()
                                                                                                       .getTakerFeePercentage(),
                                                                               amountUsd, positionLong.getExchange()
                                                                                                      .getTakerFeePercentage());
        dynamicResultData.setTotalExpensesUsd(trade.getFixedExpensesUsd().add(variableExpenses));
        dynamicResultData.setIncomeUsd(Calculations.income(dynamicResultData.getPnlUsdShort(),
                                                           dynamicResultData.getPnlUsdLong(),
                                                           dynamicResultData.getTotalExpensesUsd()));
        dynamicResultData.setTrade(trade);
        return dynamicResultData;
    }

    @Override
    @Transactional
    public Set<Trade> getCompletedTradesNotWrittenToFile() {
        return repository
                .findByWrittenToFileFalseAndTestRunIdEquals(testRunService.getCurrentTestRun().getId());
    }

    @Override
    @Transactional
    public void updateTradesWrittenToFile(Collection<Trade> trades) {
        if (CollectionUtils.isEmpty(trades)) {
            return;
        }
        repository.updateTradesSetWrittenToFileTrue(trades.stream().map(Trade::getId).collect(Collectors.toSet()));
    }

    @Override
    public boolean allTradesClosed() {
        return tradeContainer.isEmpty();
    }
}
