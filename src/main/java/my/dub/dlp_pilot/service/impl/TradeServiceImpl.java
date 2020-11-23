package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeDynamicResultData;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.TradeRepository;
import my.dub.dlp_pilot.repository.container.TradeContainer;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.util.DateUtils;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
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
    private final ParametersHolder parameters;

    @Autowired
    public TradeServiceImpl(TradeRepository repository, TradeContainer tradeContainer, TickerService tickerService,
            ExchangeService exchangeService, ParametersHolder parameters) {
        this.repository = repository;
        this.tradeContainer = tradeContainer;
        this.tickerService = tickerService;
        this.exchangeService = exchangeService;
        this.parameters = parameters;
    }

    @Override
    public void checkTradeOpen(Ticker tickerShort, Ticker tickerLong, BigDecimal averagePriceDifference,
            TestRun testRun) {
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
        BigDecimal currentPriceDifference = tickerShort.getPriceBid().subtract(tickerLong.getPriceAsk());
        if (isOpenDetrimental(tickerShort, tickerLong) || !isOpenProfitable(tickerShort, tickerLong,
                                                                            averagePriceDifference,
                                                                            currentPriceDifference)) {
            return;
        }

        Trade trade = createTrade(tickerShort, tickerLong, currentPercentageDiff, testRun, currentPriceDifference,
                                  averagePriceDifference);
        boolean tradeCreated = tradeContainer.addTrade(trade);
        if (tradeCreated) {
            log.info(
                    "New #{} {} created and added to container. Current price difference: {}; average price "
                            + "difference: {}",
                    trade.hashCode(), trade.toShortString(),
                    currentPriceDifference.stripTrailingZeros().toPlainString(),
                    averagePriceDifference.stripTrailingZeros().toPlainString());
        }
    }

    @Override
    @Transactional
    @Retryable(LockAcquisitionException.class)
    public void handleTrades(ExchangeName exchangeName) {
        Set<Trade> tradesInProgress = tradeContainer.getTrades(exchangeName);
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
            Duration tradeTimeoutDuration = parameters.getTradeTimeoutDuration();
            if (!tradeTimeoutDuration.isZero() && DateUtils.durationMillis(trade.getStartTime()) > tradeTimeoutDuration
                    .toMillis()) {
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
        });
    }

    @Override
    @Transactional
    @Retryable(LockAcquisitionException.class)
    public void closeTrades(@NonNull ExchangeName exchangeName, @NonNull TradeResultType tradeResultType) {
        checkNotNull(exchangeName, "Cannot close trades if exchangeName is null!");
        checkNotNull(tradeResultType, "Cannot close trades if tradeResultType is null!");

        tradeContainer.getTrades(exchangeName).forEach(trade -> {
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
            handleClose(trade, tickerShort, tickerLong, tradeResultType);
        });
    }

    @Override
    @Transactional
    public Set<Trade> getCompletedTradesNotWrittenToFile(@NonNull TestRun testRun) {
        checkNotNull(testRun, "Cannot get completed trades if Test run is null!");

        return repository.findByWrittenToFileFalseAndTestRunIdEqualsOrderByEndTimeAsc(testRun.getId());
    }

    @Override
    @Transactional
    public void saveOrUpdate(Collection<Trade> trades) {
        if (CollectionUtils.isEmpty(trades)) {
            return;
        }
        repository.saveAll(trades);
    }

    @Override
    public boolean isAllTradesClosed() {
        return tradeContainer.isEmpty();
    }

    private boolean canEnterTrade(Ticker tickerShort, Ticker tickerLong) {
        if (tickerService.checkStale(tickerShort, tickerLong, parameters.getStaleIntervalDuration())) {
            return false;
        }
        String base = tickerShort.getBase();
        String target = tickerShort.getTarget();
        if (tradeContainer
                .isSimilarPresent(base, target, tickerShort.getExchangeName(), tickerLong.getExchangeName())) {
            return false;
        }
        if (exchangeService.isExchangeFaulty(tickerShort.getExchangeName()) || exchangeService
                .isExchangeFaulty(tickerLong.getExchangeName())) {
            return false;
        }
        if (tradeContainer
                .checkHasDetrimentalRecord(tickerShort.getExchangeName(), tickerLong.getExchangeName(), base, target)) {
            return false;
        }
        int parallelTradesNumber = parameters.getParallelTradesNumber();
        if (parallelTradesNumber != 0) {
            Pair<Long, Long> tradesCount =
                    tradeContainer.tradesCount(tickerShort.getExchangeName(), tickerLong.getExchangeName());
            return tradesCount.getFirst() <= parallelTradesNumber && tradesCount.getSecond() <= parallelTradesNumber;
        }
        return true;
    }

    private boolean isDetrimentalSyncCondition(BigDecimal pnlShort, BigDecimal pnlLong) {
        if (isZero(parameters.getPostponeDetrimentalExitPnlPercentageDiff())) {
            return false;
        }
        BigDecimal absPnlShort = pnlShort.abs();
        BigDecimal absPnlLong = pnlLong.abs();
        return (absPnlShort.compareTo(absPnlLong) > 0 && percentageDifference(absPnlShort, absPnlLong)
                .compareTo(parameters.getPostponeDetrimentalExitPnlPercentageDiff()) > 0) || (
                absPnlLong.compareTo(absPnlShort) > 0 && percentageDifference(absPnlLong, absPnlShort)
                        .compareTo(parameters.getPostponeDetrimentalExitPnlPercentageDiff()) > 0);
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
                log.error("#{} {} was not removed from container!", prevHash, trade.toShortString());
            }

            log.info("#{} {} closed. Reason: {}", prevHash, trade.toShortString(), trade.getResultType());
            if (TradeResultType.DETRIMENTAL.equals(tradeResultType)) {
                recordDetrimental(trade);
            }
        }
    }

    private boolean isExistingSuccessful(BigDecimal income, Instant tradeStartTime) {
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        BigDecimal exitPercentage = parameters.getProfitPercentageOnExitSum(DateUtils.durationSeconds(tradeStartTime));
        BigDecimal profitValue = originalValueFromPercent(amountUsd, exitPercentage);
        return income.subtract(profitValue).compareTo(BigDecimal.ZERO) >= 0;
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

    private boolean isOpenProfitable(Ticker tickerShort, Ticker tickerLong, BigDecimal averagePriceDifference,
            BigDecimal currentPriceDifference) {
        BigDecimal avgOpenPrice = average(List.of(tickerShort.getPriceOnOpen(PositionSide.SHORT),
                                                  tickerLong.getPriceOnOpen(PositionSide.LONG)));
        BigDecimal amountUsd = parameters.getMinEntryAmount();
        BigDecimal expensesShort = exchangeService.getTotalExpenses(tickerShort.getExchangeName(), amountUsd);
        BigDecimal expensesLong = exchangeService.getTotalExpenses(tickerLong.getExchangeName(), amountUsd);
        BigDecimal expectedProfitPriceDiff = currentPriceDifference.subtract(averagePriceDifference);
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
        ExchangeName exchangeShort = trade.getPositionShort().getExchange().getName();
        ExchangeName exchangeLong = trade.getPositionLong().getExchange().getName();
        Instant invalidationDateTime = trade.getEndTime().plus(parameters.getSuspenseAfterDetrimentalTradeDuration());
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

        trade.setEndTime(Instant.now());
        trade.setResultType(resultType);
        trade.setClosePriceDiff(priceShort.subtract(priceLong).setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP));
        List<TradeDynamicResultData> resultData = parameters.getEntryAmounts().stream()
                .map(amount -> createDynamicResultData(trade, positionShort, positionLong, amount))
                .collect(Collectors.toList());
        trade.setResultData(resultData);
    }

    private Trade createTrade(Ticker tickerShort, Ticker tickerLong, BigDecimal percentageDiff, TestRun testRun,
            BigDecimal openPriceDiff, BigDecimal averagePriceDiff) {
        Trade trade = new Trade();
        // tickerShort and tickerLong have equal base and target
        trade.setBase(tickerShort.getBase());
        trade.setTarget(tickerShort.getTarget());
        trade.setEntryPercentageDiff(percentageDiff);
        trade.setOpenPriceDiff(openPriceDiff.setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP));
        trade.setAveragePriceDiff(averagePriceDiff.setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP));
        Position shortPos = createPosition(PositionSide.SHORT, tickerShort);
        Position longPos = createPosition(PositionSide.LONG, tickerLong);
        trade.setPositions(shortPos, longPos);
        trade.setResultType(TradeResultType.IN_PROGRESS);
        Exchange exchangeShort = shortPos.getExchange();
        Exchange exchangeLong = longPos.getExchange();
        trade.setFixedExpensesUsd(exchangeShort.getFixedFeesUsd().add(exchangeLong.getFixedFeesUsd()));
        trade.setTestRun(testRun);
        trade.setWrittenToFile(false);
        trade.setStartTime(Instant.now());
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
