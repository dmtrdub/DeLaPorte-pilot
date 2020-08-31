package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkState;
import static my.dub.dlp_pilot.util.Calculations.average;
import static my.dub.dlp_pilot.util.Calculations.income;
import static my.dub.dlp_pilot.util.Calculations.isZero;
import static my.dub.dlp_pilot.util.Calculations.originalValueFromPercent;
import static my.dub.dlp_pilot.util.Calculations.originalValueFromPercentSum;
import static my.dub.dlp_pilot.util.Calculations.percentageDifference;
import static my.dub.dlp_pilot.util.Calculations.percentageDifferencePrice;
import static my.dub.dlp_pilot.util.Calculations.pnl;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.PriceDifference;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeDynamicResultData;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.repository.TradeRepository;
import my.dub.dlp_pilot.repository.container.TradeContainer;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.util.DateUtils;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class TradeServiceImpl implements TradeService {

    private final TradeRepository repository;
    private final TradeContainer tradeContainer;
    private final TickerService tickerService;
    private final ExchangeService exchangeService;
    private final ParametersComponent parameters;
    private final TestRunService testRunService;

    @Autowired
    public TradeServiceImpl(TradeRepository repository, TradeContainer tradeContainer, TickerService tickerService,
            ExchangeService exchangeService, ParametersComponent parameters, TestRunService testRunService) {
        this.repository = repository;
        this.tradeContainer = tradeContainer;
        this.tickerService = tickerService;
        this.exchangeService = exchangeService;
        this.parameters = parameters;
        this.testRunService = testRunService;
    }

    @Override
    public void checkTradeOpen(PriceDifference priceDifference, Ticker tickerShort, Ticker tickerLong) {
        checkState(tickerShort.getPriceBid().compareTo(tickerLong.getPriceAsk()) > 0,
                   "BID price of SHORT ticker should be greater than ASK price of LONG ticker!");

        if (!canEnterTrade(tickerShort, tickerLong)) {
            return;
        }

        BigDecimal currentPercentageDiff =
                percentageDifferencePrice(tickerShort.getPriceBid(), tickerLong.getPriceAsk());
        if (currentPercentageDiff.compareTo(parameters.getEntryMinPercentageDiff()) < 0
                || currentPercentageDiff.compareTo(parameters.getEntryMaxPercentageDiff()) > 0) {
            return;
        }
        if (isOpenDetrimental(tickerShort, tickerLong) || !isOpenProfitable(priceDifference, tickerShort, tickerLong)) {
            return;
        }

        Trade trade = createTrade(tickerShort, tickerLong, currentPercentageDiff);
        tradeContainer.addTrade(trade);
        log.info("New {} created and added to container. Current price difference: {}; average price difference: {}",
                 trade.toShortString(), priceDifference.getCurrentValue().stripTrailingZeros().toPlainString(),
                 priceDifference.getAvgValue().stripTrailingZeros().toPlainString());
    }

    @Override
    @Transactional
    @Retryable(LockAcquisitionException.class)
    public void handleTrades(ExchangeName exchangeName) {
        if (testRunService.checkInitialDataCapture()) {
            return;
        }
        Set<Trade> tradesInProgress = tradeContainer.getTrades(exchangeName);
        boolean isTestRunEnd = testRunService.checkTestRunEnd();
        tradesInProgress.forEach(trade -> {
            Position positionShort = trade.getPositionShort();
            Position positionLong = trade.getPositionLong();
            Ticker tickerShort =
                    tickerService.getTicker(positionShort.getExchange().getName(), trade.getBase(), trade.getTarget())
                            .orElseThrow(() -> new MissingEntityException(Ticker.class.getSimpleName(),
                                                                          positionShort.toString()));
            Ticker tickerLong =
                    tickerService.getTicker(positionLong.getExchange().getName(), trade.getBase(), trade.getTarget())
                            .orElseThrow(() -> new MissingEntityException(Ticker.class.getSimpleName(),
                                                                          positionLong.toString()));
            if (isTestRunEnd) {
                handleClose(trade, tickerShort, tickerLong, TradeResultType.TEST_RUN_END);
            } else {
                int tradeMinutesTimeout = parameters.getTradeMinutesTimeout();
                if (tradeMinutesTimeout != 0 && DateUtils.durationMinutes(trade.getStartTime()) > tradeMinutesTimeout) {
                    handleClose(trade, tickerShort, tickerLong, TradeResultType.TIMED_OUT);
                } else {
                    Pair<BigDecimal, BigDecimal> pnlSides =
                            calculatePnlForSides(positionShort.getOpenPrice(), tickerShort.getPriceAsk(),
                                                 positionLong.getOpenPrice(), tickerLong.getPriceBid());
                    BigDecimal pnlShort = pnlSides.getFirst();
                    BigDecimal pnlLong = pnlSides.getSecond();
                    BigDecimal income = calculateCurrentIncome(pnlShort, tickerShort.getExchangeName(), pnlLong,
                                                               tickerLong.getExchangeName());
                    if (isExistingSuccessful(income, trade.getStartTime())) {
                        handleClose(trade, tickerShort, tickerLong, TradeResultType.SUCCESSFUL);
                    } else if (isExistingDetrimental(income) && !isDetrimentalSyncCondition(pnlShort, pnlLong)) {
                        handleClose(trade, tickerShort, tickerLong, TradeResultType.DETRIMENTAL);
                    }
                }
            }
        });
    }

    @Override
    @Transactional
    public Set<Trade> getCompletedTradesNotWrittenToFile() {
        return repository.findByWrittenToFileFalseAndTestRunIdEqualsOrderByEndTimeAsc(
                testRunService.getCurrentTestRun().getId());
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
    public boolean isAllTradesClosed() {
        return tradeContainer.isEmpty();
    }

    private boolean canEnterTrade(Ticker ticker1, Ticker ticker2) {
        if (tickerService.checkStale(ticker1, ticker2)) {
            return false;
        }
        String base = ticker1.getBase();
        String target = ticker1.getTarget();
        if (tradeContainer.isSimilarPresent(base, target, ticker1.getExchangeName(), ticker2.getExchangeName())) {
            return false;
        }
        if (exchangeService.isExchangeFaulty(ticker1.getExchangeName()) || exchangeService
                .isExchangeFaulty(ticker2.getExchangeName())) {
            return false;
        }
        if (tradeContainer
                .checkHasDetrimentalRecord(ticker1.getExchangeName(), ticker2.getExchangeName(), base, target)) {
            return false;
        }
        int parallelTradesNumber = parameters.getParallelTradesNumber();
        if (parallelTradesNumber != 0) {
            Pair<Long, Long> tradesCount =
                    tradeContainer.tradesCount(ticker1.getExchangeName(), ticker2.getExchangeName());
            return tradesCount.getFirst() <= parallelTradesNumber && tradesCount.getSecond() <= parallelTradesNumber;
        }
        return true;
    }

    private boolean isDetrimentalSyncCondition(BigDecimal pnlShort, BigDecimal pnlLong) {
        if (isZero(parameters.getExitSyncOnPnlPercentageDiff())) {
            return false;
        }
        BigDecimal absPnlShort = pnlShort.abs();
        BigDecimal absPnlLong = pnlLong.abs();
        return (absPnlShort.compareTo(absPnlLong) > 0
                && percentageDifference(absPnlShort, absPnlLong).compareTo(parameters.getExitSyncOnPnlPercentageDiff())
                > 0) || (absPnlLong.compareTo(absPnlShort) > 0
                && percentageDifference(absPnlLong, absPnlShort).compareTo(parameters.getExitSyncOnPnlPercentageDiff())
                > 0);
    }

    private synchronized void handleClose(Trade trade, Ticker tickerShort, Ticker tickerLong,
            TradeResultType tradeResultType) {
        if (!tradeContainer.isSimilarPresent(trade) || repository
                .checkSimilarExists(trade.getBase(), trade.getTarget(), trade.getStartTime(),
                                    tickerShort.getExchangeName(), tickerLong.getExchangeName(),
                                    trade.getPositionShort().getOpenPrice(), trade.getPositionLong().getOpenPrice(),
                                    tradeResultType)) {
            log.trace("Attempting to close Trade that is already closed!. Details: {}", trade.toShortString());
        } else {
            int prevHash = trade.hashCode();
            closeTrade(trade, tradeResultType, tickerShort, tickerLong);
            repository.save(trade);
            boolean removed = tradeContainer.remove(prevHash);
            if (!removed) {
                log.error("{} was not removed from container!", trade.toShortString());
            }

            log.info("{} closed. Reason: {}", trade.toShortString(), trade.getResultType());
            if (TradeResultType.DETRIMENTAL.equals(tradeResultType)) {
                recordDetrimental(trade);
            }
        }
    }

    private boolean isExistingSuccessful(BigDecimal income, ZonedDateTime tradeStartTime) {
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        BigDecimal exitPercentage = parameters.getExitProfitPercentage(DateUtils.durationSeconds(tradeStartTime));
        BigDecimal entryProfitValue = originalValueFromPercent(amountUsd, parameters.getEntryProfitPercentage());
        BigDecimal exitProfitValue = originalValueFromPercent(amountUsd, exitPercentage);
        return income.subtract(entryProfitValue).subtract(exitProfitValue).compareTo(BigDecimal.ZERO) >= 0;
    }

    private boolean isExistingDetrimental(BigDecimal income) {
        return isDetrimental(parameters.getDetrimentAmountPercentage(), income);
    }

    private boolean isOpenDetrimental(Ticker tickerShort, Ticker tickerLong) {
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        BigDecimal expensesShort = exchangeService.getTotalExpenses(tickerShort.getExchangeName(), amountUsd);
        BigDecimal expensesLong = exchangeService.getTotalExpenses(tickerLong.getExchangeName(), amountUsd);
        BigDecimal pnlShort =
                pnl(tickerShort.getSpread().negate(), tickerShort.getPriceOnOpen(PositionSide.SHORT), amountUsd);
        BigDecimal pnlLong =
                pnl(tickerLong.getSpread().negate(), tickerLong.getPriceOnOpen(PositionSide.LONG), amountUsd);
        BigDecimal income = income(pnlShort, pnlLong, expensesShort, expensesLong);
        return income.compareTo(originalValueFromPercent(amountUsd, parameters.getDetrimentAmountPercentage()).negate())
                <= 0;
    }

    private boolean isOpenProfitable(PriceDifference priceDifference, Ticker tickerShort, Ticker tickerLong) {
        BigDecimal avgOpenPrice = average(List.of(tickerShort.getPriceOnOpen(PositionSide.SHORT),
                                                  tickerLong.getPriceOnOpen(PositionSide.LONG)));
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        BigDecimal expensesShort = exchangeService.getTotalExpenses(tickerShort.getExchangeName(), amountUsd);
        BigDecimal expensesLong = exchangeService.getTotalExpenses(tickerLong.getExchangeName(), amountUsd);
        BigDecimal expectedProfitPriceDiff =
                priceDifference.getCurrentValue().subtract(priceDifference.getBreakThroughAvgPriceDiff());
        BigDecimal income =
                income(pnl(expectedProfitPriceDiff, avgOpenPrice, amountUsd), BigDecimal.ZERO, expensesShort,
                       expensesLong);

        BigDecimal entryProfitValue = originalValueFromPercent(amountUsd, parameters.getEntryProfitPercentage());
        return income.subtract(entryProfitValue).compareTo(BigDecimal.ZERO) >= 0;
    }

    private boolean isDetrimental(BigDecimal detrimentalEntryPercentage, BigDecimal income) {
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        return income.compareTo(originalValueFromPercent(amountUsd, detrimentalEntryPercentage).negate()) <= 0;
    }

    private Pair<BigDecimal, BigDecimal> calculatePnlForSides(BigDecimal positionShortOpenPrice,
            BigDecimal tickerShortPrice, BigDecimal positionLongOpenPrice, BigDecimal tickerLongPrice) {
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        BigDecimal pnlShort = pnl(PositionSide.SHORT, positionShortOpenPrice, tickerShortPrice, amountUsd);
        BigDecimal pnlLong = pnl(PositionSide.LONG, positionLongOpenPrice, tickerLongPrice, amountUsd);
        return Pair.of(pnlShort, pnlLong);
    }

    private BigDecimal calculateCurrentIncome(BigDecimal pnlShort, ExchangeName exchangeShort, BigDecimal pnlLong,
            ExchangeName exchangeLong) {
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        BigDecimal expensesShort = exchangeService.getTotalExpenses(exchangeShort, amountUsd);
        BigDecimal expensesLong = exchangeService.getTotalExpenses(exchangeLong, amountUsd);
        return income(pnlShort, pnlLong, expensesShort, expensesLong);
    }

    private void recordDetrimental(Trade trade) {
        int suspenseAfterDetrimentalSeconds = parameters.getSuspenseAfterDetrimentalSeconds();
        ExchangeName exchangeShort = trade.getPositionShort().getExchange().getName();
        ExchangeName exchangeLong = trade.getPositionLong().getExchange().getName();
        ZonedDateTime invalidationDateTime = trade.getEndTime().plusSeconds(suspenseAfterDetrimentalSeconds);
        String base = trade.getBase();
        String target = trade.getTarget();
        tradeContainer.addDetrimentalRecord(exchangeShort, exchangeLong, base, target, invalidationDateTime);
        log.debug("Added new detrimental record for {} (SHORT) and {} (LONG) exchanges, base: {}, target {}. "
                          + "Similar trades will be suspended until {}", exchangeShort, exchangeLong, base, target,
                  invalidationDateTime);
    }

    private void closeTrade(Trade trade, TradeResultType resultType, Ticker tickerShort, Ticker tickerLong) {
        BigDecimal priceShort = tickerShort.getPriceAsk();
        Position positionShort = trade.getPositionShort();
        positionShort.setClosePrice(priceShort);

        BigDecimal priceLong = tickerLong.getPriceBid();
        Position positionLong = trade.getPositionLong();
        positionLong.setClosePrice(priceLong);

        trade.setEndTime(DateUtils.currentDateTime());
        trade.setResultType(resultType);
        List<TradeDynamicResultData> resultData = parameters.getEntryAmounts().stream()
                .map(amount -> createDynamicResultData(trade, positionShort, positionLong, amount))
                .collect(Collectors.toList());
        trade.setResultData(resultData);
    }

    private Trade createTrade(Ticker tickerShort, Ticker tickerLong, BigDecimal percentageDiff) {
        Trade trade = new Trade();
        // tickerShort and tickerLong have equal base and target
        trade.setBase(tickerShort.getBase());
        trade.setTarget(tickerShort.getTarget());
        trade.setEntryPercentageDiff(percentageDiff);
        Position shortPos = createPosition(PositionSide.SHORT, tickerShort);
        Position longPos = createPosition(PositionSide.LONG, tickerLong);
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
        BigDecimal tickerPrice = ticker.getPriceOnOpen(side);
        position.setOpenPrice(tickerPrice);
        ExchangeName exchangeName = ticker.getExchangeName();
        Exchange exchange = exchangeService.findByName(exchangeName);
        position.setExchange(exchange);
        return position;
    }

    private TradeDynamicResultData createDynamicResultData(Trade trade, Position positionShort, Position positionLong,
            Double amount) {
        TradeDynamicResultData dynamicResultData = new TradeDynamicResultData();
        BigDecimal amountUsd = BigDecimal.valueOf(amount);
        dynamicResultData.setAmountUsd(amountUsd);
        dynamicResultData.setPnlUsdShort(
                pnl(positionShort.getSide(), positionShort.getOpenPrice(), positionShort.getClosePrice(), amountUsd));
        dynamicResultData.setPnlUsdLong(
                pnl(positionLong.getSide(), positionLong.getOpenPrice(), positionLong.getClosePrice(), amountUsd));
        BigDecimal variableExpenses =
                originalValueFromPercentSum(amountUsd, positionShort.getExchange().getTakerFeePercentage(), amountUsd,
                                            positionLong.getExchange().getTakerFeePercentage());
        dynamicResultData.setTotalExpensesUsd(trade.getFixedExpensesUsd().add(variableExpenses));
        dynamicResultData.setIncomeUsd(income(dynamicResultData.getPnlUsdShort(), dynamicResultData.getPnlUsdLong(),
                                              dynamicResultData.getTotalExpensesUsd()));
        dynamicResultData.setTrade(trade);
        return dynamicResultData;
    }
}
