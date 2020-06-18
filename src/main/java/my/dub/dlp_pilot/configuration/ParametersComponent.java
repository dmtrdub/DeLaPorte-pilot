package my.dub.dlp_pilot.configuration;

import lombok.Getter;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.util.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Getter
public class ParametersComponent implements InitializingBean {

    @Value("${init_trade_delay_ms}")
    private long initDelayMs;
    @Value("${ticker_stale_difference_seconds}")
    private int staleDifferenceSeconds;
    @Value("${usd_price_fallback_exchange_name}")
    private String fallbackExchangeParam;
    @Value("${trade_entry_min_percentage}")
    private double entryMinPercentageDouble;
    @Value("${trade_entry_max_percentage}")
    private double entryMaxPercentageDouble;
    @Value("${trade_exit_diff_percentage}")
    private double exitPercentageDiffDouble;
    @Value("${trade_entry_amounts_usd}")
    private String[] entryAmountsUsdParam;
    @Value("${trade_minutes_timeout}")
    private int tradeMinutesTimeout;
    @Value("${trade_detrimental_percentage_delta}")
    private double detrimentPercentageDeltaDouble;
    @Value("${test_run_duration}")
    private String testRunDurationParam;
    @Value("${test_run_result_csv_dir_path}")
    private String pathToResultDir;
    @Value("${test_run_forced_exit_file_path}")
    private String forcedExitFilePath;
    @Value("${test_run_forced_exit_code}")
    private String exitCode;
    @Value("${test_run_max_delay_on_exit_seconds}")
    private int exitMaxDelaySeconds;

    private ExchangeName fallbackExchangeName;
    private BigDecimal entryMinPercentage;
    private BigDecimal entryMaxPercentage;
    private BigDecimal exitPercentageDiff;
    private List<Double> entryAmounts;
    private BigDecimal detrimentPercentageDelta;
    private Duration testRunDuration;

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
        initDelayMs = initDelayMs > 0 ? initDelayMs : 0;
        fallbackExchangeName = ExchangeName.valueOf(fallbackExchangeParam);
        entryMinPercentage = BigDecimal.valueOf(entryMinPercentageDouble);
        entryMaxPercentage = BigDecimal.valueOf(entryMaxPercentageDouble);
        exitPercentageDiff = BigDecimal.valueOf(exitPercentageDiffDouble);
        entryAmounts = Arrays.stream(entryAmountsUsdParam).mapToDouble(Double::parseDouble).boxed().distinct()
                             .collect(Collectors.toList());
        detrimentPercentageDelta = BigDecimal.valueOf(detrimentPercentageDeltaDouble);
        testRunDuration = DateUtils.parseDuration(testRunDurationParam);
    }

    private void validateInputParams() {
        if (staleDifferenceSeconds < 1) {
            throw new IllegalArgumentException("Stale difference for ticker cannot be < 1 seconds!");
        }
        if (entryMinPercentageDouble <= 0.0) {
            throw new IllegalArgumentException("Trade entry min percentage cannot be <= 0!");
        }
        if (entryMaxPercentageDouble <= entryMinPercentageDouble) {
            throw new IllegalArgumentException("Trade entry max percentage cannot be <= entry min percentage!");
        }
        if (exitPercentageDiffDouble >= entryMinPercentageDouble) {
            throw new IllegalArgumentException("Trade exit percentage cannot be >= entry min percentage!");
        }
        if (ArrayUtils.isEmpty(entryAmountsUsdParam)) {
            throw new IllegalArgumentException("Trade entry sums cannot be empty!");
        }
        if (String.join(",", entryAmountsUsdParam).length() > Constants.STRING_PARAM_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Trade entry sums cannot be longer than %d chars!", Constants.STRING_PARAM_LENGTH));
        }
        if (Arrays.stream(entryAmountsUsdParam).mapToDouble(Double::parseDouble).boxed()
                  .anyMatch(aDouble -> aDouble < 10)) {
            throw new IllegalArgumentException("Trade entry sum cannot be < $10");
        }
        if (tradeMinutesTimeout <= 0) {
            throw new IllegalArgumentException("Trade timeout minutes cannot be <= 0!");
        }
        if (detrimentPercentageDeltaDouble <= 0) {
            throw new IllegalArgumentException("Detrimental percentage delta cannot be <= 0!");
        }
        if (StringUtils.isEmpty(testRunDurationParam)) {
            throw new IllegalArgumentException("Duration parameter cannot be empty!");
        }
        if (DateUtils.parseDuration(testRunDurationParam) == null) {
            throw new IllegalArgumentException("Duration parameter cannot be parsed!");
        }
        if (StringUtils.isEmpty(pathToResultDir)) {
            throw new IllegalArgumentException("Path to result dir parameter cannot be empty!");
        }
        if (pathToResultDir.length() > Constants.FILE_PATH_PARAM_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Path to result dir parameter cannot be longer than %d chars!",
                                  Constants.FILE_PATH_PARAM_LENGTH));
        }
        if (StringUtils.isNotEmpty(forcedExitFilePath) && StringUtils.isBlank(exitCode)) {
            throw new IllegalArgumentException("Exit code cannot be empty if forced exit is enabled!");
        }
        if (exitMaxDelaySeconds < 1) {
            throw new IllegalArgumentException("Max delay before end of test run cannot be < 1 seconds!");
        }
    }
}
