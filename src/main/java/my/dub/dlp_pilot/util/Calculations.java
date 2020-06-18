package my.dub.dlp_pilot.util;

import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.client.Ticker;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Calculations {
    private Calculations() {
    }

    public static BigDecimal percentageDifference(BigDecimal price1, BigDecimal price2) {
        if (price1 == null || price2 == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal hundred = BigDecimal.valueOf(100);
        if (price1.compareTo(price2) > 0 && !isZero(price2)) {
            return price1.multiply(hundred).divide(price2, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                         .subtract(hundred);
        } else if (price2.compareTo(price1) > 0 && !isZero(price1)) {
            return price2.multiply(hundred).divide(price1, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                         .subtract(hundred);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal percentageDifference(Ticker tickerShort, Ticker tickerLong) {
        if (tickerLong == null || tickerShort == null || isZero(tickerLong.getPrice())) {
            return BigDecimal.ZERO;
        }
        BigDecimal hundred = BigDecimal.valueOf(100);
        return tickerShort.getPrice().multiply(hundred)
                          .divide(tickerLong.getPrice(), Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                          .subtract(hundred);
    }

    public static BigDecimal originalValueFromPercent(BigDecimal target, BigDecimal percentage) {
        return percentage.divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP).multiply(target);
    }

    public static BigDecimal originalValueFromPercentSum(BigDecimal target1, BigDecimal percentage1, BigDecimal target2,
                                                         BigDecimal percentage2) {
        return originalValueFromPercent(target1, percentage1).add(originalValueFromPercent(target2, percentage2));
    }

    public static BigDecimal pnl(PositionSide side, BigDecimal openPrice, BigDecimal closePrice, BigDecimal amountUsd) {
        BigDecimal amount = amountUsd.divide(openPrice, RoundingMode.HALF_UP);
        if (PositionSide.SHORT.equals(side)) {
            return openPrice.subtract(closePrice).multiply(amount);
        }
        return closePrice.subtract(openPrice).multiply(amount);
    }

    public static BigDecimal income(BigDecimal pnl1, BigDecimal pnl2, BigDecimal totalExpenses) {
        return pnl1.add(pnl2).subtract(totalExpenses);
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
