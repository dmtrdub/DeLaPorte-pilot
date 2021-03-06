package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static my.dub.dlp_pilot.util.Calculations.average;
import static my.dub.dlp_pilot.util.Calculations.income;
import static my.dub.dlp_pilot.util.Calculations.isNotNegative;
import static my.dub.dlp_pilot.util.Calculations.isZero;
import static my.dub.dlp_pilot.util.Calculations.originalValueFromPercent;
import static my.dub.dlp_pilot.util.Calculations.originalValueFromPercentSum;
import static my.dub.dlp_pilot.util.Calculations.percentageDifferenceAbs;
import static my.dub.dlp_pilot.util.Calculations.percentageDifferencePrice;
import static my.dub.dlp_pilot.util.Calculations.pnl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.TradeRepository;
import my.dub.dlp_pilot.repository.container.TradeContainer;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link TradeService} service.
 */
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
    public void checkTradeOpen(@NonNull Ticker tickerShort, @NonNull Ticker tickerLong,
            @NonNull BigDecimal averagePriceDifference, @NonNull TestRun testRun) {
        checkNotNull(tickerShort, Constants.NULL_ARGUMENT_MESSAGE, "tickerShort");
        checkNotNull(tickerLong, Constants.NULL_ARGUMENT_MESSAGE, "tickerLong");
        checkNotNull(averagePriceDifference, Constants.NULL_ARGUMENT_MESSAGE, "averagePriceDifference");
        checkNotNull(testRun, Constants.NULL_ARGUMENT_MESSAGE, "testRun");

        BigDecimal shortPrice = tickerShort.getPriceBid();
        BigDecimal longPrice = tickerLong.getPriceAsk();
        checkState(shortPrice.compareTo(longPrice) > 0,
                   "BID price of SHORT ticker should be greater than ASK price of LONG ticker!");

        if (!canEnterTrade(tickerShort, tickerLong)) {
            return;
        }

        BigDecimal currentPercentageDiff = percentageDifferencePrice(shortPrice, longPrice);
        if (currentPercentageDiff.compareTo(parameters.getEntryMinPercentageDiff()) < 0
                || currentPercentageDiff.compareTo(parameters.getEntryMaxPercentageDiff()) > 0) {
            return;
        }
        BigDecimal currentPriceDifference = shortPrice.subtract(longPrice);
        if (isOpenDetrimental(tickerShort, tickerLong) || !isOpenProfitable(tickerShort, tickerLong,
                                                                            averagePriceDifference,
                                                                            currentPriceDifference)) {
            return;
        }

        Trade trade = createTrade(tickerShort, tickerLong, currentPercentageDiff, testRun, currentPriceDifference,
                                  averagePriceDifference);
        boolean tradeCreated = tradeContainer.addTrade(trade);
        if (tradeCreated) {
            log.info("New #{} {} opened. Current price difference: {}; average price " + "difference: {}",
                     trade.getLocalId(), trade.toShortString(),
                     Calculations.originalDecimalResult(currentPriceDifference),
                     Calculations.originalDecimalResult(averagePriceDifference));
        } else {
            log.error("Unable to locally save new #{} {}", trade.getLocalId(), trade.toShortString());
        }
    }

    @Override
    @Transactional
    public void handleTrades(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");

        Set<Trade> tradesInProgress = tradeContainer.getTrades(exchangeName);
        tradesInProgress.forEach(trade -> {
            Position positionShort = trade.getPositionShort();
            Position positionLong = trade.getPositionLong();
            Ticker tickerShort = tickerService
                    .getTickerWithRetry(positionShort.getExchange().getName(), trade.getBase(), trade.getTarget());
            Ticker tickerLong = tickerService
                    .getTickerWithRetry(positionLong.getExchange().getName(), trade.getBase(), trade.getTarget());
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
                checkExtremumPnl(positionShort, pnlShort);
                checkExtremumPnl(positionLong, pnlLong);
                BigDecimal income = calculateCurrentIncome(pnlShort, tickerShort.getExchangeName(), pnlLong,
                                                           tickerLong.getExchangeName());
                if (isExistingSuccessful(income, trade.getStartTime())) {
                    handleClose(trade, tickerShort, tickerLong, TradeResultType.SUCCESSFUL);
                } else if (isExistingDetrimental(income) && !checkDetrimentalSyncCondition(trade, pnlShort, pnlLong)) {
                    handleClose(trade, tickerShort, tickerLong, TradeResultType.DETRIMENTAL);
                }
            }
        });
    }

    @Override
    @Transactional
    public void closeTrades(@NonNull ExchangeName exchangeName, @NonNull TradeResultType tradeResultType) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");
        checkNotNull(tradeResultType, Constants.NULL_ARGUMENT_MESSAGE, "tradeResultType");

        tradeContainer.getTrades(exchangeName).forEach(trade -> {
            Position positionShort = trade.getPositionShort();
            Position positionLong = trade.getPositionLong();
            Ticker tickerShort = tickerService
                    .getTickerWithRetry(positionShort.getExchange().getName(), trade.getBase(), trade.getTarget());
            Ticker tickerLong = tickerService
                    .getTickerWithRetry(positionLong.getExchange().getName(), trade.getBase(), trade.getTarget());
            handleClose(trade, tickerShort, tickerLong, tradeResultType);
        });
    }

    @Override
    @Transactional
    public List<Trade> getCompletedTradesNotWrittenToFile(@NonNull TestRun testRun) {
        checkNotNull(testRun, Constants.NULL_ARGUMENT_MESSAGE, "testRun");

        return repository.findDistinctByWrittenToFileFalseAndTestRunIdEqualsOrderByEndTimeAsc(testRun.getId());
    }

    @Override
    @Transactional
    public void saveOrUpdate(@Nullable Collection<Trade> trades) {
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
                .checkDetrimentalRecord(tickerShort.getExchangeName(), tickerLong.getExchangeName(), base, target)) {
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

    private void checkExtremumPnl(Position position, BigDecimal pnl) {
        if (position.getMinPnlUsd() == null || pnl.compareTo(position.getMinPnlUsd()) < 0) {
            position.setMinPnlUsd(pnl);
            position.setMinPnlTime(Instant.now());
        }
        if (position.getMaxPnlTime() == null || pnl.compareTo(position.getMaxPnlUsd()) > 0) {
            position.setMaxPnlUsd(pnl);
            position.setMaxPnlTime(Instant.now());
        }
    }

    private boolean checkDetrimentalSyncCondition(Trade trade, BigDecimal pnlShort, BigDecimal pnlLong) {
        if (isZero(parameters.getDetrimentalCloseOnMaxPnlDiffPercentage())) {
            return false;
        }
        // detrimental sync stays until trade is closed as SUCCESSFUL / TIMED_OUT / TEST_RUN_END
        if (trade.isDetrimentalSync()) {
            return true;
        }
        BigDecimal absPnlShort = pnlShort.abs();
        BigDecimal absPnlLong = pnlLong.abs();
        boolean isDetrimentalSyncCondition = (absPnlShort.compareTo(absPnlLong) > 0 &&
                percentageDifferenceAbs(pnlShort, pnlLong)
                        .compareTo(parameters.getDetrimentalCloseOnMaxPnlDiffPercentage()) > 0) || (
                absPnlLong.compareTo(absPnlShort) > 0 && percentageDifferenceAbs(pnlLong, pnlShort)
                        .compareTo(parameters.getDetrimentalCloseOnMaxPnlDiffPercentage()) > 0);
        if (isDetrimentalSyncCondition) {
            log.info("#{} Trade has entered a detrimental sync condition at {}. PnL Short: {} USD | PnL Long {} USD",
                     trade.getLocalId(), DateUtils.formatDateTime(Instant.now()), pnlShort, pnlLong);
            trade.setDetrimentalSync(true);
            return true;
        }
        return false;
    }

    private synchronized void handleClose(Trade trade, Ticker tickerShort, Ticker tickerLong,
            TradeResultType tradeResultType) {
        if (tradeContainer.isSimilarPresent(trade) && !repository
                .checkSimilarExists(trade.getBase(), trade.getTarget(), tickerShort.getExchangeName(),
                                    tickerLong.getExchangeName(), trade.getPositionShort().getOpenPrice(),
                                    trade.getPositionLong().getOpenPrice(), tradeResultType)) {
            closeTrade(trade, tradeResultType, tickerShort, tickerLong);
            repository.save(trade);
            Long localId = trade.getLocalId();
            boolean removed = tradeContainer.remove(localId);
            if (!removed) {
                log.error("#{} {} was not removed from container!", localId, trade.toShortString());
            }

            log.info("#{} {} closed", localId, trade.toShortString());
            if (TradeResultType.DETRIMENTAL.equals(tradeResultType)) {
                recordDetrimental(trade);
            }
        }
    }

    private boolean isExistingSuccessful(BigDecimal income, Instant tradeStartTime) {
        BigDecimal amountUsd = parameters.getEntryAmount();
        BigDecimal exitPercentage = parameters.getProfitPercentageOnExitSum(DateUtils.durationMillis(tradeStartTime));
        BigDecimal profitValue = originalValueFromPercent(amountUsd, exitPercentage);
        return isNotNegative(income.subtract(profitValue));
    }

    private boolean isExistingDetrimental(BigDecimal income) {
        return isDetrimental(parameters.getDetrimentAmountPercentage(), income);
    }

    private boolean isOpenDetrimental(Ticker tickerShort, Ticker tickerLong) {
        BigDecimal amountUsd = parameters.getEntryAmount();
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
        BigDecimal amountUsd = parameters.getEntryAmount();
        BigDecimal expensesShort = exchangeService.getTotalExpenses(tickerShort.getExchangeName(), amountUsd);
        BigDecimal expensesLong = exchangeService.getTotalExpenses(tickerLong.getExchangeName(), amountUsd);
        BigDecimal expectedProfitPriceDiff = currentPriceDifference.subtract(averagePriceDifference);
        BigDecimal income =
                income(pnl(expectedProfitPriceDiff, avgOpenPrice, amountUsd), BigDecimal.ZERO, expensesShort,
                       expensesLong);

        BigDecimal entryProfitValue = originalValueFromPercent(amountUsd, parameters.getEntryProfitPercentage());
        return isNotNegative(income.subtract(entryProfitValue));
    }

    private boolean isDetrimental(BigDecimal detrimentalEntryPercentage, BigDecimal income) {
        BigDecimal amountUsd = parameters.getEntryAmount();
        return income.compareTo(originalValueFromPercent(amountUsd, detrimentalEntryPercentage).negate()) <= 0;
    }

    private Pair<BigDecimal, BigDecimal> calculatePnlForSides(BigDecimal positionShortOpenPrice,
            BigDecimal tickerShortPrice, BigDecimal positionLongOpenPrice, BigDecimal tickerLongPrice) {
        BigDecimal amountUsd = parameters.getEntryAmount();
        BigDecimal pnlShort = pnl(PositionSide.SHORT, positionShortOpenPrice, tickerShortPrice, amountUsd);
        BigDecimal pnlLong = pnl(PositionSide.LONG, positionLongOpenPrice, tickerLongPrice, amountUsd);
        return Pair.of(pnlShort, pnlLong);
    }

    private BigDecimal calculateCurrentIncome(BigDecimal pnlShort, ExchangeName exchangeShort, BigDecimal pnlLong,
            ExchangeName exchangeLong) {
        BigDecimal amountUsd = parameters.getEntryAmount();
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
                  DateUtils.formatDateTime(invalidationDateTime));
    }

    private void closeTrade(Trade trade, TradeResultType resultType, Ticker tickerShort, Ticker tickerLong) {
        BigDecimal amountUsd = parameters.getEntryAmount();

        BigDecimal priceShort = tickerShort.getPriceAsk();
        Position positionShort = trade.getPositionShort();
        positionShort.setClosePrice(priceShort);
        positionShort.setPnlUsd(pnl(positionShort.getSide(), positionShort.getOpenPrice(), priceShort, amountUsd));

        BigDecimal priceLong = tickerLong.getPriceBid();
        Position positionLong = trade.getPositionLong();
        positionLong.setClosePrice(priceLong);
        positionLong.setPnlUsd(pnl(positionLong.getSide(), positionLong.getOpenPrice(), priceLong, amountUsd));

        BigDecimal variableExpenses =
                originalValueFromPercentSum(amountUsd, positionShort.getExchange().getTakerFeePercentage(), amountUsd,
                                            positionLong.getExchange().getTakerFeePercentage());
        trade.setTotalExpensesUsd(trade.getFixedExpensesUsd().add(variableExpenses));
        trade.setIncomeUsd(income(positionShort.getPnlUsd(), positionLong.getPnlUsd(), trade.getTotalExpensesUsd()));
        trade.setEndTime(Instant.now());
        trade.setResultType(resultType);
        trade.setClosePriceDiff(priceShort.subtract(priceLong).setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP));
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
}
