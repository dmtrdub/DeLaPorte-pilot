package my.dub.dlp_pilot.service.impl;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.*;
import my.dub.dlp_pilot.repository.TradeRepository;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.service.TransferService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static my.dub.dlp_pilot.util.Calculations.*;
import static my.dub.dlp_pilot.util.NumberUtils.getPercentResult;

@Slf4j
@Service
public class TradeServiceImpl implements TradeService, InitializingBean {

    @Value("${trade_entry_diff_percentage}")
    private double entryPercentage;
    @Value("${trade_exit_pnl_percentage}")
    private double exitPercentage;
    @Value("${exchange_deposit_sum_usd}")
    private double exchangeDepositSumUsd;
    @Value("${trade_minutes_timeout}")
    private double tradeMinutesTimeout;

    private BigDecimal entrySumUsd;
    private BigDecimal exitSumUsd;

    private final TradeRepository repository;
    private final TickerService tickerService;
    private final TransferService transferService;

    @Autowired
    public TradeServiceImpl(TradeRepository repository,
                            TickerService tickerService, TransferService transferService) {
        this.repository = repository;
        this.tickerService = tickerService;
        this.transferService = transferService;
    }

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
        entrySumUsd = getPercentResult(exchangeDepositSumUsd, entryPercentage);
        exitSumUsd = getPercentResult(exchangeDepositSumUsd, exitPercentage);
    }

    private void validateInputParams() {
        if (entryPercentage <= 0.0) {
            throw new IllegalArgumentException("Trade entry percentage cannot be <= 0!");
        }
        if (exitPercentage >= entryPercentage) {
            throw new IllegalArgumentException("Trade exit percentage cannot be >= entry percentage!");
        }
        if (exchangeDepositSumUsd <= 0) {
            throw new IllegalArgumentException("Trade sum cannot be <= 0!");
        }
        if (tradeMinutesTimeout < 0) {
            throw new IllegalArgumentException("Trade timeout minutes cannot be < 0!");
        }
    }

    @Transactional
    @Override
    public void save(Collection<Trade> trades) {
        if (CollectionUtils.isEmpty(trades)) {
            return;
        }
        repository.saveAll(trades);
    }

    @Override
    public Set<Trade> findTradesInProgress() {
        return repository.findByEndTimeIsNullAndResultTypeEquals(TradeResultType.IN_PROGRESS);
    }

    @Override
    public void trade() {
        handleTransfers();
        handleTrades();
    }

    @Transactional
    @Override
    public void handleTransfers() {
        Set<Transfer> endingTransfers = transferService.findEndingTransfers();
        Set<Trade> newTrades = new HashSet<>();
        for (Transfer transfer : endingTransfers) {
            Exchange recipient1 = transfer.getRecipient1();
            Exchange recipient2 = transfer.getRecipient2();
            Trade trade = new Trade();
            ZonedDateTime currentDateTime = DateUtils.currentDateTime();
            trade.setStartTime(currentDateTime);
            Ticker ticker1 = tickerService.getTicker(recipient1.getId(), transfer.getBase1(), transfer.getTarget1())
                    .orElseThrow(() -> new AssertionError("Ticker in container is null!"));
            Ticker ticker2 = tickerService.getTicker(recipient2.getId(), transfer.getBase2(), transfer.getTarget2())
                    .orElseThrow(() -> new AssertionError("Ticker in container is null!"));
            BigDecimal priceUsd1 = ticker1.getPriceUsd();
            BigDecimal priceUsd2 = ticker2.getPriceUsd();
            BigDecimal amount = calculateAmount(priceUsd1, priceUsd2, exchangeDepositSumUsd);
            if (isEntryProfitable(ticker1, ticker2, amount, exchangeDepositSumUsd, entrySumUsd) &&
                    tickersSafe(ticker1, ticker2)) {
                BigDecimal expenses =
                        calculateInitialTradeFeesUsd(recipient1, recipient2, exchangeDepositSumUsd);
                trade.setExpensesUsd(expenses);
                trade.setAmount(amount);
                Position longPos;
                Position shortPos;
                if (priceUsd1.compareTo(priceUsd2) > 0) {
                    shortPos = createPosition(PositionSide.SHORT, ticker1);
                    longPos = createPosition(PositionSide.LONG, ticker2);
                } else {
                    longPos = createPosition(PositionSide.LONG, ticker1);
                    shortPos = createPosition(PositionSide.SHORT, ticker2);
                }
                trade.setPositions(shortPos, longPos);
                trade.setResultType(TradeResultType.IN_PROGRESS);
            } else {
                trade.setExpensesUsd(getDepositWithdrawalFeesUsd(recipient1, recipient2));
                trade.setResultType(TradeResultType.IRRELEVANT);
                trade.setEndTime(currentDateTime);
                trade.setTotalIncome(trade.getPnlUsd().subtract(trade.getExpensesUsd()));
            }
            newTrades.add(trade);
            transfer.setStatus(TransferStatus.DONE);
        }
        transferService.save(endingTransfers);
        save(newTrades);
    }

    private boolean tickersSafe(Ticker ticker1, Ticker ticker2) {
        return ticker1 != null && !(ticker1.isStale() && ticker1.isAnomaly()) && ticker2 != null &&
                !(ticker2.isStale() && ticker2.isAnomaly());
    }

    private boolean canExitTrade(Trade trade) {
        if (trade == null) {
            return false;
        }
        return trade.getPnlUsd().subtract(trade.getWithdrawFeesTotal()).compareTo(exitSumUsd) >= 0;
    }

    @Transactional
    @Override
    public void handleTrades() {
        Set<Trade> tradesInProgress = findTradesInProgress();

        for (Trade trade : tradesInProgress) {
            if (tradeMinutesTimeout != 0 &&
                    DateUtils.durationMinutes(trade.getStartTime(), trade.getEndTime()) > tradeMinutesTimeout) {
                closeTrade(trade, TradeResultType.TIMED_OUT);
            } else if (canExitTrade(trade)) {
                closeTrade(trade, TradeResultType.SUCCESSFUL);
            } else {
                recalculatePnl(trade);
            }
        }
        //update trades in db
        repository.saveAll(tradesInProgress);
    }

    private void closeTrade(Trade trade, TradeResultType resultType) {
        Position positionShort = trade.getPositionShort();
        Position positionLong = trade.getPositionLong();
        Ticker tickerShort = tickerService
                .getTicker(positionShort).orElseThrow(() -> new AssertionError("Ticker in container is null!"));
        Ticker tickerLong = tickerService
                .getTicker(positionLong).orElseThrow(() -> new AssertionError("Ticker in container is null!"));
        recalculatePnl(trade, tickerShort, tickerLong);
        BigDecimal priceShort = tickerShort.getPrice();
        BigDecimal priceUsdShort = tickerShort.getPriceUsd();
        BigDecimal priceLong = tickerLong.getPrice();
        BigDecimal priceUsdLong = tickerLong.getPriceUsd();

        positionShort.setClosePrice(priceShort);
        positionShort.setClosePriceUsd(priceUsdShort);
        positionLong.setClosePrice(priceLong);
        positionLong.setClosePriceUsd(priceUsdLong);

        trade.setPnlMin(positionShort.getPnlMin().add(positionLong.getPnlMin()));
        trade.setPnlMinUsd(positionShort.getPnlMinUsd().add(positionLong.getPnlMinUsd()));
        trade.addExpensesUsd(trade.getWithdrawFeesTotal());
        trade.setTotalIncome(trade.getPnlUsd().subtract(trade.getExpensesUsd()));
        trade.setEndTime(DateUtils.currentDateTime());
        trade.setResultType(resultType);
    }

    private void recalculatePnl(Trade trade, Ticker tickerShort, Ticker tickerLong) {
        Position positionShort = trade.getPositionShort();
        Position positionLong = trade.getPositionLong();
        BigDecimal amount = trade.getAmount();
        handlePriceChangeForPosition(positionShort, amount, tickerShort.getPrice(), tickerShort.getPriceUsd());
        handlePriceChangeForPosition(positionLong, amount, tickerLong.getPrice(), tickerLong.getPriceUsd());
        trade.setPnl(positionShort.getPnl().add(positionLong.getPnl()));
        trade.setPnlUsd(positionShort.getPnlUsd().add(positionLong.getPnlUsd()));
    }

    private void recalculatePnl(Trade trade) {
        Ticker tickerShort = tickerService
                .getTicker(trade, PositionSide.SHORT)
                .orElseThrow(() -> new AssertionError("Ticker in container is null!"));
        Ticker tickerLong = tickerService
                .getTicker(trade, PositionSide.LONG)
                .orElseThrow(() -> new AssertionError("Ticker in container is null!"));
        recalculatePnl(trade, tickerShort, tickerLong);
    }

    private void handlePriceChangeForPosition(Position position, BigDecimal amount, BigDecimal currentPrice,
                                              BigDecimal currentPriceUsd) {
        Assert.state(position.getClosePrice() != null || position.getClosePriceUsd() != null,
                     "Handled position cannot be already closed!");
        BigDecimal openPrice = position.getOpenPrice();
        BigDecimal openPriceUsd = position.getOpenPriceUsd();
        BigDecimal priceDiff;
        BigDecimal priceDiffUsd;
        if (PositionSide.LONG.equals(position.getSide())) {
            priceDiff = currentPrice.subtract(openPrice);
            priceDiffUsd = currentPriceUsd.subtract(openPriceUsd);
        } else {
            priceDiff = openPrice.subtract(currentPrice);
            priceDiffUsd = openPriceUsd.subtract(currentPriceUsd);
        }
        BigDecimal pnl = amount.multiply(priceDiff);
        BigDecimal pnlUsd = amount.multiply(priceDiffUsd);
        position.setPnl(pnl);
        position.setPnlUsd(pnlUsd);
        if (pnl.compareTo(position.getPnlMin()) < 0) {
            position.setPnlMin(pnl);
        }
        if (pnlUsd.compareTo(position.getPnlMinUsd()) < 0) {
            position.setPnlMin(pnl);
        }
    }

    private Position createPosition(PositionSide side, Ticker ticker) {
        Position position = new Position();
        position.setBase(ticker.getBase());
        position.setTarget(ticker.getTarget());
        position.setSide(side);
        position.setOpenPrice(ticker.getPrice());
        position.setOpenPriceUsd(ticker.getPriceUsd());
        position.setExchange(ticker.getExchange());
        return position;
    }

}
