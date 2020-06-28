package my.dub.dlp_pilot.util;

import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.client.Ticker;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Calculations {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Calculations() {
    }

    public static BigDecimal percentageDifference(BigDecimal price1, BigDecimal price2) {
        if (price1 == null || price2 == null) {
            return BigDecimal.ZERO;
        }
        if (price1.compareTo(price2) > 0 && !isZero(price2)) {
            return price1.multiply(HUNDRED).divide(price2, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                         .subtract(HUNDRED);
        } else if (price2.compareTo(price1) > 0 && !isZero(price1)) {
            return price2.multiply(HUNDRED).divide(price1, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                         .subtract(HUNDRED);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal percentageDifference(Ticker tickerShort, Ticker tickerLong) {
        if (tickerLong == null || tickerShort == null || isZero(tickerLong.getPrice())) {
            return BigDecimal.ZERO;
        }
        return tickerShort.getPrice().multiply(HUNDRED)
                          .divide(tickerLong.getPrice(), Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                          .subtract(HUNDRED);
    }

    public static BigDecimal expectedClosePriceLong(BigDecimal priceShort, BigDecimal expectedPriceDiffPercentage) {
        if (priceShort == null || expectedPriceDiffPercentage == null || isZero(priceShort)) {
            return BigDecimal.ZERO;
        }
        return priceShort.multiply(HUNDRED)
                         .divide(expectedPriceDiffPercentage.add(HUNDRED), Constants.PRICE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal originalValueFromPercent(BigDecimal target, BigDecimal percentage) {
        return percentage.divide(HUNDRED, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP).multiply(target)
                         .stripTrailingZeros();
    }

    public static BigDecimal originalValueFromPercentSum(BigDecimal target1, BigDecimal percentage1, BigDecimal target2,
                                                         BigDecimal percentage2) {
        return originalValueFromPercent(target1, percentage1).add(originalValueFromPercent(target2, percentage2));
    }

    public static BigDecimal pnl(PositionSide side, BigDecimal openPrice, BigDecimal closePrice, BigDecimal amountUsd) {
        BigDecimal amount = amountUsd.divide(closePrice, Constants.PRICE_SCALE, RoundingMode.HALF_UP);
        if (PositionSide.SHORT.equals(side)) {
            return openPrice.subtract(closePrice).multiply(amount).setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP)
                            .stripTrailingZeros();
        }
        return closePrice.subtract(openPrice).multiply(amount).setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP)
                         .stripTrailingZeros();
    }

    public static BigDecimal expectedPnlLong(BigDecimal openPriceLong, BigDecimal openPriceShort, BigDecimal amountUsd,
                                             BigDecimal expectedPriceDiffPercentage) {
        if (openPriceLong == null || openPriceShort == null || isZero(openPriceLong) || isZero(openPriceShort)) {
            return BigDecimal.ZERO;
        }
        BigDecimal expectedClosePriceLong = expectedClosePriceLong(openPriceShort, expectedPriceDiffPercentage);
        return pnl(PositionSide.LONG, openPriceLong, expectedClosePriceLong, amountUsd);
    }

    public static BigDecimal income(BigDecimal pnl1, BigDecimal pnl2, BigDecimal totalExpenses) {
        return pnl1.add(pnl2).subtract(totalExpenses);
    }

    public static BigDecimal expectedIncome(BigDecimal openPriceLong, BigDecimal openPriceShort, BigDecimal amountUsd,
                                            BigDecimal expectedPriceDiffPercentage, BigDecimal totalExpenses) {
        return income(expectedPnlLong(openPriceLong, openPriceShort, amountUsd, expectedPriceDiffPercentage),
                      BigDecimal.ZERO, totalExpenses);
    }

    public static BigDecimal getDecimal(String value, int scale) {
        BigDecimal decimal = new BigDecimal(value);
        return decimal.setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal getDecimal(double value, int scale) {
        BigDecimal decimal = BigDecimal.valueOf(value);
        return decimal.setScale(scale, RoundingMode.HALF_UP);
    }

    public static boolean isZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
}
