package my.dub.dlp_pilot.service.impl;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.rest.NonexistentUSDPriceException;
import my.dub.dlp_pilot.model.*;
import my.dub.dlp_pilot.model.client.Ticker;
import my.dub.dlp_pilot.repository.TradeRepository;
import my.dub.dlp_pilot.repository.container.CurrentTradeContainer;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.DateUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class TradeServiceImpl implements TradeService, InitializingBean {

    @Value("${trade_entry_min_percentage}")
    private double entryMinPercentage;
    @Value("${trade_entry_max_percentage}")
    private double entryMaxPercentage;
    @Value("${trade_exit_diff_percentage}")
    private double exitPercentageDiff;
    @Value("${exchange_deposit_sum_usd}")
    private double exchangeDepositSumUsd;
    @Value("${trade_minutes_timeout}")
    private double tradeMinutesTimeout;
    @Value("${trade_detrimental_percentage_delta}")
    private double detrimentPercentageDelta;

    private final TradeRepository repository;
    private final CurrentTradeContainer tradeContainer;
    private final TickerService tickerService;
    private final ExchangeService exchangeService;

    @Autowired
    public TradeServiceImpl(TradeRepository repository,
                            CurrentTradeContainer tradeContainer,
                            TickerService tickerService,
                            ExchangeService exchangeService) {
        this.repository = repository;
        this.tradeContainer = tradeContainer;
        this.tickerService = tickerService;
        this.exchangeService = exchangeService;
    }

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
    }

    private void validateInputParams() {
        if (entryMinPercentage <= 0.0) {
            throw new IllegalArgumentException("Trade entry min percentage cannot be <= 0!");
        }
        if (entryMaxPercentage <= entryMinPercentage) {
            throw new IllegalArgumentException("Trade entry max percentage cannot be <= entry min percentage!");
        }
        if (exitPercentageDiff >= entryMinPercentage) {
            throw new IllegalArgumentException("Trade exit percentage cannot be >= entry min percentage!");
        }
        if (exchangeDepositSumUsd <= 0) {
            throw new IllegalArgumentException("Trade sum cannot be <= 0!");
        }
        if (tradeMinutesTimeout <= 0) {
            throw new IllegalArgumentException("Trade timeout minutes cannot be <= 0!");
        }
        if (detrimentPercentageDelta <= 0) {
            throw new IllegalArgumentException("Detrimental percentage delta cannot be <= 0!");
        }
    }

    @Override
    public void searchForTrades(Exchange exchange) {
        Set<Ticker> allTickers = tickerService.getAllTickers();
        Set<Ticker> tickersToCompare = tickerService.getTickers(exchange.getName());
        allTickers.removeAll(tickersToCompare);
        allTickers.forEach(ticker -> {
            Ticker equivalentTicker = tickerService.findEquivalentTickerFromSet(ticker, tickersToCompare);
            if (equivalentTicker != null && !tickerService.checkStale(ticker) &&
                    !tickerService.checkStale(equivalentTicker)) {
                BigDecimal currentPercentageDiff =
                        Calculations.percentageDifference(ticker.getPrice(), equivalentTicker.getPrice());
                BigDecimal prevPercentageDiff = Calculations
                        .percentageDifference(ticker.getPreviousPrice(), equivalentTicker.getPreviousPrice());
                double currentPercentageDiffDouble = currentPercentageDiff.doubleValue();
                if (currentPercentageDiffDouble < entryMinPercentage ||
                        prevPercentageDiff.doubleValue() >= entryMinPercentage ||
                        currentPercentageDiffDouble > entryMaxPercentage) {
                    return;
                }
                try {
                    Trade trade = createTrade(ticker, equivalentTicker, currentPercentageDiff);
                    boolean added = tradeContainer.addTrade(trade);
                    if (added) {
                        log.debug("New {} created and added to container", trade.toShortString());
                    }
                } catch (NonexistentUSDPriceException e) {
                    log.debug(e.getMessage());
                }
            }
        });
    }

    private Trade createTrade(Ticker ticker1, Ticker ticker2, BigDecimal percentageDiff) {
        Trade trade = new Trade();
        trade.setStartTime(DateUtils.currentDateTime());
        trade.setAmount(BigDecimal.ONE);
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
        trade.setExpensesUsd(exchangeShort.getDepositFeeUsd().add(exchangeShort.getWithdrawFeeUsd()
                                                                               .add(exchangeLong.getDepositFeeUsd()
                                                                                                .add(exchangeLong
                                                                                                             .getWithdrawFeeUsd()))));
        return trade;
    }

    @Override
    @Transactional
    @Retryable(LockAcquisitionException.class)
    public void handleTrades(ExchangeName exchangeName) {
        Set<Trade> tradesInProgress = tradeContainer.getTrades(exchangeName);
        Set<Trade> tradesCompleted = new HashSet<>();
        Set<Trade> tradesToSave = new HashSet<>();
        tradesInProgress.forEach(trade -> {
            Position positionShort = trade.getPositionShort();
            Position positionLong = trade.getPositionLong();
            Ticker tickerShort = tickerService
                    .getTicker(positionShort).orElseThrow(() -> new NullPointerException(
                            String.format("Ticker for position (%s) in container is null!", positionShort.toString())));
            Ticker tickerLong = tickerService
                    .getTicker(positionLong).orElseThrow(() -> new NullPointerException(
                            String.format("Ticker for position (%s) in container is null!", positionLong.toString())));
            if (tradeMinutesTimeout != 0 && DateUtils.durationMinutes(trade.getStartTime()) > tradeMinutesTimeout) {
                closeAndAddToSets(tradesCompleted, tradesToSave, trade, tickerShort, tickerLong,
                                  TradeResultType.TIMED_OUT);
            } else {
                BigDecimal percentageDiff =
                        Calculations.percentageDifference(tickerShort.getPrice(), tickerLong.getPrice());
                BigDecimal entryPercentageDiff = trade.getEntryPercentageDiff();
                if (entryPercentageDiff.subtract(percentageDiff).compareTo(BigDecimal.valueOf(exitPercentageDiff)) >
                        0) {
                    closeAndAddToSets(tradesCompleted, tradesToSave, trade, tickerShort, tickerLong,
                                      TradeResultType.SUCCESSFUL);
                } else if (percentageDiff.subtract(entryPercentageDiff)
                                         .compareTo(BigDecimal.valueOf(detrimentPercentageDelta)) >= 0) {
                    closeAndAddToSets(tradesCompleted, tradesToSave, trade, tickerShort, tickerLong,
                                      TradeResultType.DETRIMENTAL);
                }
            }
        });
        if (!tradesToSave.isEmpty()) {
            repository.saveAll(tradesToSave);
            tradesToSave
                    .forEach(trade -> log.debug("{} closed. Reason: {}", trade.toShortString(), trade.getResultType()));
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
        BigDecimal usdPriceShort = Constants.USD_SYMBOLS.contains(tickerShort.getTarget()) ? priceShort :
                tickerService.getUsdPrice(tickerShort.getBase(), positionShort.getExchange().getName());
        positionShort.setClosePriceUsd(usdPriceShort);

        BigDecimal priceLong = tickerLong.getPrice();
        Position positionLong = trade.getPositionLong();
        positionLong.setClosePrice(priceLong);
        BigDecimal usdPriceLong = Constants.USD_SYMBOLS.contains(tickerLong.getTarget()) ? priceLong :
                tickerService.getUsdPrice(tickerLong.getBase(), positionLong.getExchange().getName());
        positionLong.setClosePriceUsd(usdPriceLong);

        trade.setEndTime(DateUtils.currentDateTime());
        trade.setResultType(resultType);
        BigDecimal amount = trade.getAmount();
        trade.setPnl(calculatePnl(positionShort, positionLong, amount));
        trade.setPnlUsd(calculatePnlUsd(positionShort, positionLong, amount));
        trade.setTotalIncome(BigDecimal.ONE);
    }

    private BigDecimal calculatePnl(Position positionShort, Position positionLong, BigDecimal amount) {
        BigDecimal pnlShort = Calculations
                .pnl(positionShort.getSide(), positionShort.getOpenPrice(), positionShort.getClosePrice(), amount);
        BigDecimal pnlLong = Calculations
                .pnl(positionLong.getSide(), positionLong.getOpenPrice(), positionLong.getClosePrice(), amount);
        return pnlShort.add(pnlLong);
    }

    private BigDecimal calculatePnlUsd(Position positionShort, Position positionLong, BigDecimal amount) {
        BigDecimal pnlShort = Calculations
                .pnl(positionShort.getSide(), positionShort.getOpenPriceUsd(), positionShort.getClosePriceUsd(),
                     amount);
        BigDecimal pnlLong = Calculations
                .pnl(positionLong.getSide(), positionLong.getOpenPriceUsd(), positionLong.getClosePriceUsd(), amount);
        return pnlShort.add(pnlLong);
    }

    private Position createPosition(PositionSide side, Ticker ticker) {
        Position position = new Position();
        position.setBase(ticker.getBase());
        position.setTarget(ticker.getTarget());
        position.setSide(side);
        BigDecimal tickerPrice = ticker.getPrice();
        position.setOpenPrice(tickerPrice);
        ExchangeName exchangeName = ticker.getExchangeName();
        BigDecimal priceUsd = Constants.USD_SYMBOLS.contains(position.getTarget()) ?
                tickerPrice : tickerService.getUsdPrice(ticker.getBase(), exchangeName);
        position.setOpenPriceUsd(priceUsd);
        Exchange exchange = exchangeService.findByName(
                exchangeName).orElseThrow(
                () -> new NullPointerException(String.format("Exchange by name %s was not found!", exchangeName)));
        position.setExchange(exchange);
        return position;
    }

}
