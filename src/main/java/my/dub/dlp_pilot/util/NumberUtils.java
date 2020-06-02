package my.dub.dlp_pilot.util;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public final class NumberUtils {
    private NumberUtils() {
    }

    public static BigDecimal getDecimal(String value, int scale) {
        BigDecimal decimal = new BigDecimal(value);
        return decimal.setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal getDecimal(double value, int scale) {
        BigDecimal decimal = BigDecimal.valueOf(value);
        return decimal.setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal getVolumeDecimal(String value) {
        return getDecimal(value, Constants.VOLUME_SCALE);
    }

    public static BigDecimal getPriceDecimal(String value) {
        return getDecimal(value, Constants.PRICE_SCALE);
    }

    public static BigDecimal getVolumeDecimal(double value) {
        return getDecimal(value, Constants.VOLUME_SCALE);
    }

    public static BigDecimal getPriceDecimal(double value) {
        return getDecimal(value, Constants.PRICE_SCALE);
    }

    public static BigDecimal getPercentageDecimal(double value) {
        return getDecimal(value, Constants.PERCENTAGE_SCALE);
    }

    public static int integerDigits(BigDecimal decimal) {
        BigDecimal noTrailZerosDecimal = decimal.stripTrailingZeros();
        return noTrailZerosDecimal.precision() - noTrailZerosDecimal.scale();
    }

    public static BigDecimal getPercentResult(double target, BigDecimal percentage) {
        if (target == 0 || percentage == null) {
            return BigDecimal.ZERO;
        }
        return percentage.divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(target));
    }

    public static BigDecimal getPercentResult(double target, double percentage) {
        return BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(target));
    }

    public static BigDecimal getPercentResult(BigDecimal target, BigDecimal percentage) {
        return percentage.divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP).multiply(target);
    }
}
