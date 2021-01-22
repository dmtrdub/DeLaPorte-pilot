package my.dub.dlp_pilot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.PositionSide;
import org.springframework.lang.NonNull;

/**
 * Utility class for executing calculations with {@link BigDecimal} numbers.
 */
public final class Calculations {
    private static final RoundingMode DEFAULT_ROUND = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Calculations() {
    }

    public static BigDecimal percentageDifferenceAbs(@NonNull BigDecimal newValue, @NonNull BigDecimal origValue) {
        if (isZero(origValue)) {
            return newValue.multiply(HUNDRED);
        }

        BigDecimal result = newValue.subtract(origValue).divide(origValue, Constants.PERCENTAGE_SCALE, DEFAULT_ROUND)
                .multiply(HUNDRED);
        return result.abs();
    }

    public static BigDecimal percentageDifferencePrice(@NonNull BigDecimal priceShort, @NonNull BigDecimal priceLong) {
        return percentageDifferenceAbs(priceShort, priceLong);
    }

    public static BigDecimal originalValueFromPercent(@NonNull BigDecimal target, @NonNull BigDecimal percentage) {
        return percentage.divide(HUNDRED, Constants.PERCENTAGE_SCALE, DEFAULT_ROUND).multiply(target)
                .stripTrailingZeros();
    }

    public static BigDecimal originalValueFromPercentSum(@NonNull BigDecimal target1, @NonNull BigDecimal percentage1,
            @NonNull BigDecimal target2, @NonNull BigDecimal percentage2) {
        return originalValueFromPercent(target1, percentage1).add(originalValueFromPercent(target2, percentage2));
    }

    public static BigDecimal pnl(@NonNull PositionSide side, @NonNull BigDecimal openPrice,
            @NonNull BigDecimal closePrice, @NonNull BigDecimal amountUsd) {
        BigDecimal priceDiff;
        if (PositionSide.SHORT.equals(side)) {
            priceDiff = openPrice.subtract(closePrice);
        } else {
            priceDiff = closePrice.subtract(openPrice);
        }
        return pnl(priceDiff, openPrice, amountUsd);
    }

    public static BigDecimal pnl(@NonNull BigDecimal priceDiff, @NonNull BigDecimal openPrice,
            @NonNull BigDecimal amountUsd) {
        return priceDiff.multiply(amountUsd).divide(openPrice, Constants.PRICE_SCALE, DEFAULT_ROUND)
                .stripTrailingZeros();
    }

    public static BigDecimal income(@NonNull BigDecimal pnl1, @NonNull BigDecimal pnl2,
            @NonNull BigDecimal... expenses) {
        BigDecimal totalExpenses = Arrays.stream(expenses).reduce(BigDecimal.ZERO, BigDecimal::add);
        return pnl1.add(pnl2).subtract(totalExpenses);
    }

    public static boolean isZero(@NonNull BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isNotPositive(@NonNull BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) <= 0;
    }

    public static boolean isNotNegative(@NonNull BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0;
    }

    public static BigDecimal average(@NonNull Collection<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        return sum.divide(BigDecimal.valueOf(values.size()), DEFAULT_ROUND);
    }

    public static String decimalResult(@NonNull BigDecimal decimal) {
        return decimal.setScale(Constants.MAX_RESULT_SCALE, DEFAULT_ROUND).stripTrailingZeros().toPlainString();
    }

    public static List<String> decimalResults(@NonNull BigDecimal... decimals) {
        return Arrays.stream(decimals).map(Calculations::decimalResult).collect(Collectors.toList());
    }

    public static String originalDecimalResult(@NonNull BigDecimal decimal) {
        return decimal.stripTrailingZeros().toPlainString();
    }

    public static List<String> originalDecimalResults(@NonNull BigDecimal... decimals) {
        return Arrays.stream(decimals).map(Calculations::originalDecimalResult).collect(Collectors.toList());
    }
}
