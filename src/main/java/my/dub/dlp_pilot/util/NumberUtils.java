package my.dub.dlp_pilot.util;

import my.dub.dlp_pilot.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
}
