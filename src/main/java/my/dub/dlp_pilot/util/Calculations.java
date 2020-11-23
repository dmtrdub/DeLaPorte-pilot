package my.dub.dlp_pilot.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.PositionSide;

public final class Calculations {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Calculations() {
    }

    public static BigDecimal percentageDifference(BigDecimal newValue, BigDecimal origValue) {
        checkNotNull(newValue, "New value should not be null when calculating percentage difference!");
        checkNotNull(origValue, "Original value should not be null when calculating percentage difference!");

        if (isZero(origValue)) {
            return newValue.multiply(HUNDRED);
        }

        BigDecimal result =
                newValue.subtract(origValue).divide(origValue, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                        .multiply(HUNDRED);
        return origValue.compareTo(newValue) > 0 && !isNotPositive(result) ? result.negate() : result;
    }

    public static BigDecimal percentageDifferencePrice(BigDecimal priceShort, BigDecimal priceLong) {
        return percentageDifference(priceShort, priceLong);
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
        BigDecimal amount = amountUsd.divide(openPrice, Constants.PRICE_SCALE, RoundingMode.HALF_UP);
        if (PositionSide.SHORT.equals(side)) {
            return openPrice.subtract(closePrice).multiply(amount).setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP)
                    .stripTrailingZeros();
        }
        return closePrice.subtract(openPrice).multiply(amount).setScale(Constants.PRICE_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    public static BigDecimal pnl(BigDecimal priceDiff, BigDecimal openPrice, BigDecimal amountUsd) {
        return priceDiff.multiply(amountUsd).divide(openPrice, Constants.PRICE_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    public static BigDecimal income(BigDecimal pnl1, BigDecimal pnl2, BigDecimal... expenses) {
        BigDecimal totalExpenses = Arrays.stream(expenses).reduce(BigDecimal.ZERO, BigDecimal::add);
        return pnl1.add(pnl2).subtract(totalExpenses);
    }

    public static boolean isZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isNotPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) <= 0;
    }

    public static BigDecimal average(Collection<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        return sum.divide(BigDecimal.valueOf(values.size()), RoundingMode.HALF_UP);
    }

    public static String decimalResult(BigDecimal decimal) {
        return decimal.setScale(Constants.MAX_RESULT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static List<String> decimalResults(BigDecimal... decimals) {
        return Arrays.stream(decimals).map(Calculations::decimalResult).collect(Collectors.toList());
    }

    public static String originalDecimalResult(BigDecimal decimal) {
        return decimal.stripTrailingZeros().toPlainString();
    }

    public static List<String> originalDecimalResults(BigDecimal... decimals) {
        return Arrays.stream(decimals).map(Calculations::originalDecimalResult).collect(Collectors.toList());
    }
}
