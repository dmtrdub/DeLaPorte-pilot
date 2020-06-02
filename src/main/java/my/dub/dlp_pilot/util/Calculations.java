package my.dub.dlp_pilot.util;

import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.PositionSide;

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
        if (price1.compareTo(price2) > 0) {
            return price1.multiply(hundred).divide(price2, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                    .subtract(hundred);
        } else if (price2.compareTo(price1) > 0) {
            return price2.multiply(hundred).divide(price1, Constants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                    .subtract(hundred);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal pnl(PositionSide side, BigDecimal openPrice, BigDecimal closePrice, BigDecimal amount) {
        switch (side) {
            case SHORT:
                return openPrice.subtract(closePrice).multiply(amount);
            case LONG:
                return closePrice.subtract(openPrice).multiply(amount);
        }
        return BigDecimal.ZERO;
    }

}
