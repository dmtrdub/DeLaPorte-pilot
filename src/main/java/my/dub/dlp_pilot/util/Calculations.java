package my.dub.dlp_pilot.util;

import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.Ticker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static my.dub.dlp_pilot.util.NumberUtils.getPercentResult;

public final class Calculations {
    private Calculations() {
    }

    public static BigDecimal calculateNetDiff(Ticker ticker1, Ticker ticker2, double exchangeDepositSumUsd) {
        BigDecimal absDiff = ticker1.getPriceUsd().subtract(ticker2.getPriceUsd()).abs();
        Exchange exchange1 = ticker1.getExchange();
        Exchange exchange2 = ticker2.getExchange();
        return absDiff.subtract(getTotalTradeFeesUsd(exchange1, exchange2, exchangeDepositSumUsd));
    }

    public static BigDecimal calculateInitialTradeFeesUsd(Exchange exchange, double exchangeDepositSumUsd) {
        BigDecimal takerFeeUsd = getPercentResult(exchangeDepositSumUsd, exchange.getTakerFeePercentage());
        BigDecimal depositFeeUsd = exchange.getDepositFeeUsd();
        return takerFeeUsd.add(depositFeeUsd);
    }

    public static BigDecimal calculateInitialTradeFeesUsd(Exchange exchange1, Exchange exchange2,
                                                          double exchangeDepositSumUsd) {
        return calculateInitialTradeFeesUsd(exchange1, exchangeDepositSumUsd)
                .add(calculateInitialTradeFeesUsd(exchange2, exchangeDepositSumUsd));
    }

    public static BigDecimal getTotalTradeFeesUsd(Exchange exchange1, Exchange exchange2,
                                                  double exchangeDepositSumUsd) {
        BigDecimal initialExpenses = calculateInitialTradeFeesUsd(exchange1, exchange2, exchangeDepositSumUsd);
        return initialExpenses.add(exchange1.getWithdrawFeeUsd()).add(exchange2.getWithdrawFeeUsd());
    }

    public static BigDecimal getDepositWithdrawalFeesUsd(Exchange exchange1, Exchange exchange2) {
        return exchange1.getDepositFeeUsd().add(exchange2.getDepositFeeUsd()).add(exchange1.getWithdrawFeeUsd())
                .add(exchange2.getWithdrawFeeUsd());
    }

    public static boolean isEntryProfitable(Ticker ticker1, Ticker ticker2, double exchangeDepositSumUsd,
                                            BigDecimal entrySumUsd) {
        Objects.requireNonNull(ticker1, "Ticker cannot be null when calculating entry profitability");
        Objects.requireNonNull(ticker2, "Ticker cannot be null when calculating entry profitability");
        Objects.requireNonNull(entrySumUsd, "Entry sum USD cannot be null when calculating entry profitability");

        BigDecimal amount = calculateAmount(ticker1.getPriceUsd(), ticker2.getPriceUsd(), exchangeDepositSumUsd);
        return calculateNetDiff(ticker1, ticker2, exchangeDepositSumUsd).multiply(amount).compareTo(entrySumUsd) >= 0;
    }

    public static boolean isEntryProfitable(Ticker ticker1, Ticker ticker2, BigDecimal amount,
                                            double exchangeDepositSumUsd, BigDecimal entrySumUsd) {
        Objects.requireNonNull(ticker1, "Ticker cannot be null when calculating entry profitability");
        Objects.requireNonNull(ticker2, "Ticker cannot be null when calculating entry profitability");
        Objects.requireNonNull(entrySumUsd, "Entry sum USD cannot be null when calculating entry profitability");

        return calculateNetDiff(ticker1, ticker2, exchangeDepositSumUsd).multiply(amount).compareTo(entrySumUsd) >= 0;
    }

    public static BigDecimal calculateAmount(BigDecimal priceUsd1,
                                             BigDecimal priceUsd2, double exchangeDepositSumUsd) {
        BigDecimal tradeSumUsd = BigDecimal.valueOf(exchangeDepositSumUsd);
        BigDecimal amountShort = tradeSumUsd.divide(priceUsd1, Constants.AMOUNT_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal amountLong = tradeSumUsd.divide(priceUsd2, Constants.AMOUNT_SCALE, RoundingMode.HALF_EVEN);
        return amountShort.min(amountLong);
    }
}
