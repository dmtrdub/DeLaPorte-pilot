package my.dub.dlp_pilot.configuration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.util.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
@Getter
@Slf4j
public class ParametersComponent implements InitializingBean {

    @Value("${init_trade_delay_ms}")
    private long initDelayMs;
    @Value("${ticker_stale_difference_seconds}")
    private int staleDifferenceSeconds;
    @Value("${trade_entry_min_percentage}")
    private double entryMinPercentageDouble;
    @Value("${trade_entry_max_percentage}")
    private double entryMaxPercentageDouble;
    @Value("${trade_exit_amount_percentage}")
    private double exitAmountPercentageDouble;
    @Value("${trade_decrease_exit_amount_percentage_after_seconds}")
    private int exitAmountPercentageDecreaseAfterSeconds;
    @Value("${trade_decrease_exit_amount_percentage_by}")
    private double exitAmountPercentageDecreaseByDouble;
    @Value("${trade_entry_amounts_usd}")
    private String[] entryAmountsUsdParam;
    @Value("${trade_minutes_timeout}")
    private int tradeMinutesTimeout;
    @Value("${trade_entry_detrimental_amount_percentage}")
    private double entryDetrimentAmountPercentageDouble;
    @Value("${trade_detrimental_amount_percentage}")
    private double detrimentAmountPercentageDouble;
    @Value("${trade_parallel_number}")
    private int parallelTradesNumber;
    @Value("${trade_suspense_after_detrimental_duration_seconds}")
    private int suspenseAfterDetrimentalSeconds;
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

    private BigDecimal entryMinPercentage;
    private BigDecimal entryMaxPercentage;
    private BigDecimal exitPercentage;
    private BigDecimal exitPercentageDecreaseBy;
    private List<Double> entryAmounts;
    private BigDecimal entryDetrimentPercentage;
    private BigDecimal detrimentAmountPercentage;
    private Duration testRunDuration;

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
        initDelayMs = initDelayMs > 0 ? initDelayMs : 0;
        entryMinPercentage = BigDecimal.valueOf(entryMinPercentageDouble);
        entryMaxPercentage = BigDecimal.valueOf(entryMaxPercentageDouble);
        exitPercentage = BigDecimal.valueOf(exitAmountPercentageDouble);
        exitPercentageDecreaseBy = BigDecimal.valueOf(exitAmountPercentageDecreaseByDouble);
        entryAmounts = Arrays.stream(entryAmountsUsdParam).mapToDouble(Double::parseDouble).boxed().distinct().sorted()
                             .collect(Collectors.toList());
        entryDetrimentPercentage = BigDecimal.valueOf(entryDetrimentAmountPercentageDouble);
        detrimentAmountPercentage = BigDecimal.valueOf(detrimentAmountPercentageDouble);
        tradeMinutesTimeout = tradeMinutesTimeout > 0 ? tradeMinutesTimeout : 0;
        parallelTradesNumber = parallelTradesNumber > 0 ? parallelTradesNumber : 0;
        suspenseAfterDetrimentalSeconds = suspenseAfterDetrimentalSeconds > 0 ? suspenseAfterDetrimentalSeconds : 0;
        testRunDuration = DateUtils.parseDuration(testRunDurationParam.toUpperCase());
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
        if (exitAmountPercentageDouble >= entryMinPercentageDouble) {
            throw new IllegalArgumentException("Trade exit percentage diff cannot be >= entry min percentage!");
        }
        if (exitAmountPercentageDecreaseByDouble >= exitAmountPercentageDouble) {
            throw new IllegalArgumentException("Trade exit percentage diff decrease cannot be >= exit percentage!");
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
        if (entryDetrimentAmountPercentageDouble <= 0) {
            throw new IllegalArgumentException("Entry detrimental percentage cannot be <= 0!");
        }
        if (entryDetrimentAmountPercentageDouble >= 100) {
            throw new IllegalArgumentException("Entry detrimental percentage cannot be >= 100!");
        }
        if (detrimentAmountPercentageDouble <= 0) {
            throw new IllegalArgumentException("Detrimental exit amount percentage cannot be <= 0!");
        }
        if (detrimentAmountPercentageDouble >= 100) {
            throw new IllegalArgumentException("Detrimental exit amount percentage cannot be >= 100!");
        }
        if (detrimentAmountPercentageDouble <= entryDetrimentAmountPercentageDouble) {
            throw new IllegalArgumentException(
                    "Detrimental exit amount percentage cannot be <= Entry detrimental percentage!");
        }
        if (StringUtils.isEmpty(testRunDurationParam)) {
            throw new IllegalArgumentException("Duration parameter cannot be empty!");
        }
        Duration duration = DateUtils.parseDuration(testRunDurationParam);
        if (duration == null || duration.equals(Duration.ZERO)) {
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

    public BigDecimal getExitPercentage(long tradeDurationSeconds) {
        if (exitAmountPercentageDecreaseAfterSeconds <= 0 || exitAmountPercentageDecreaseByDouble <= 0) {
            return exitPercentage;
        }
        long decreaseTimes = tradeDurationSeconds / exitAmountPercentageDecreaseAfterSeconds;
        if (decreaseTimes <= 0) {
            return exitPercentage;
        }
        BigDecimal exitPerc =
                exitPercentage.subtract(exitPercentageDecreaseBy.multiply(BigDecimal.valueOf(decreaseTimes)));
        return exitPerc.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.valueOf(0.01d) : exitPerc;
    }

    public Optional<String> getConfiguration() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(ResourceUtils.getFile("classpath:application.properties")));
        } catch (IOException e) {
            log.error("Unable to load application.properties file! Details: " + e.getMessage());
            return Optional.empty();
        }
        List<String> configProperties = prop.entrySet().stream().filter(entry -> {
            String key = (String) entry.getKey();
            return key.startsWith("ticker") || key.startsWith("trade");
        }).map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.toList());
        return Optional.of(String.join(";", configProperties));
    }
}
