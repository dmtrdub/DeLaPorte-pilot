package my.dub.dlp_pilot.configuration;

import static my.dub.dlp_pilot.util.DateUtils.formatDuration;
import static my.dub.dlp_pilot.util.DateUtils.parseDuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.TimeFrame;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Getter
@Slf4j
public class ParametersHolder implements InitializingBean {

    private static final String DISABLED_MESSAGE = "DISABLED";

    @Value("${price_data_stale_interval_duration}")
    private String staleInterval;
    @Value("${price_data_capture_period_duration}")
    private String dataCapturePeriod;
    @Value("${price_data_capture_timeFrame}")
    private String dataCaptureTimeFrameParam;

    @Value("${trade_entry_profit_percentage}")
    private double entryProfitPercentageDouble;
    @Value("${trade_entry_min_percentage_diff}")
    private double entryMinPercentageDiffDouble;
    @Value("${trade_entry_max_percentage_diff}")
    private double entryMaxPercentageDiffDouble;
    @Value("${trade_exit_profit_percentage}")
    private double exitProfitPercentageDouble;
    @Value("${trade_decrease_expected_profit_percentage_after_duration}")
    private String profitPercentageDecreaseAfter;
    @Value("${trade_decrease_expected_profit_percentage_by}")
    private double profitPercentageDecreaseByDouble;
    @Value("${trade_entry_amounts_usd}")
    private String[] entryAmountsUsdParam;
    @Value("${trade_timeout_duration}")
    private String tradeTimeout;
    @Value("${trade_detrimental_amount_percentage}")
    private double detrimentAmountPercentageDouble;
    @Value("${trade_parallel_number}")
    private int parallelTradesNumber;
    @Value("${trade_suspense_after_detrimental_duration}")
    private String suspenseAfterDetrimentalTradeDurationParam;

    @Value("${test_run_duration}")
    private String testRunDurationParam;
    @Value("${test_run_result_csv_dir_path}")
    private String pathToResultDir;
    @Value("${test_run_forced_exit_file_path}")
    private String forcedExitFilePath;
    @Value("${test_run_forced_exit_code}")
    private String exitCode;
    @Value("${test_run_exit_delay_duration}")
    private String exitDelay;
    @Value("${test_run_delete_bars_on_exit}")
    private boolean deleteBarsOnExit;

    private Duration staleIntervalDuration;
    private Duration dataCapturePeriodDuration;
    private Duration profitPercentageDecreaseAfterDuration;
    private Duration tradeTimeoutDuration;
    private Duration suspenseAfterDetrimentalTradeDuration;
    private Duration testRunDuration;
    private Duration exitDelayDuration;

    private TimeFrame dataCaptureTimeFrame;
    private long profitPercentageDecreaseAfterDurationMillis;
    private BigDecimal entryProfitPercentage;
    private BigDecimal entryMinPercentageDiff;
    private BigDecimal entryMaxPercentageDiff;
    private BigDecimal exitProfitPercentage;
    private BigDecimal profitPercentageDecreaseBy;
    private List<Double> entryAmounts;
    private BigDecimal detrimentAmountPercentage;

    @Override
    public void afterPropertiesSet() {
        parseDurationParams();
        validateInputParams();
        setDefaultValues();
        logParams();
    }

    public BigDecimal getProfitPercentageOnExitSum(long tradeDurationSeconds) {
        BigDecimal totalProfitPercentage = entryProfitPercentage.add(exitProfitPercentage);
        if (profitPercentageDecreaseAfterDurationMillis <= 0 || profitPercentageDecreaseByDouble <= 0) {
            return totalProfitPercentage;
        }
        long decreaseTimes = tradeDurationSeconds / profitPercentageDecreaseAfterDurationMillis;
        if (decreaseTimes <= 0) {
            return totalProfitPercentage;
        }
        BigDecimal resultPerc =
                totalProfitPercentage.subtract(profitPercentageDecreaseBy.multiply(BigDecimal.valueOf(decreaseTimes)));
        return resultPerc.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : resultPerc;
    }

    public Optional<String> getConfiguration() {
        Properties prop = new Properties();
        try {
            prop.load(new ClassPathResource("application.properties").getInputStream());
        } catch (IOException e) {
            log.error("Unable to load application.properties file! Details: " + e.getMessage());
            return Optional.empty();
        }
        List<String> configProperties = prop.entrySet().stream().filter(entry -> {
            String key = (String) entry.getKey();
            return key.startsWith("price") || key.startsWith("trade");
        }).map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.toList());
        return Optional.of(String.join(";", configProperties));
    }

    public BigDecimal getMinEntryAmount() {
        return BigDecimal.valueOf(entryAmounts.get(0));
    }

    private void setDefaultValues() {
        profitPercentageDecreaseAfterDuration = getDefaultDuration(profitPercentageDecreaseAfterDuration);
        profitPercentageDecreaseAfterDurationMillis = profitPercentageDecreaseAfterDuration.toMillis();
        tradeTimeoutDuration = getDefaultDuration(tradeTimeoutDuration);
        suspenseAfterDetrimentalTradeDuration = getDefaultDuration(suspenseAfterDetrimentalTradeDuration);
        exitDelayDuration = getDefaultDuration(exitDelayDuration);

        entryProfitPercentage = BigDecimal.valueOf(entryProfitPercentageDouble);
        entryMinPercentageDiffDouble = entryMinPercentageDiffDouble > 0 ? entryMinPercentageDiffDouble : 0;
        entryMinPercentageDiff = BigDecimal.valueOf(entryMinPercentageDiffDouble);
        entryMaxPercentageDiffDouble = entryMaxPercentageDiffDouble > 0 ? entryMaxPercentageDiffDouble : 0;
        entryMaxPercentageDiff = BigDecimal.valueOf(entryMaxPercentageDiffDouble);
        exitProfitPercentage = BigDecimal.valueOf(exitProfitPercentageDouble);
        profitPercentageDecreaseByDouble = profitPercentageDecreaseByDouble > 0 ? profitPercentageDecreaseByDouble : 0;
        profitPercentageDecreaseBy = BigDecimal.valueOf(profitPercentageDecreaseByDouble);
        entryAmounts = Arrays.stream(entryAmountsUsdParam).mapToDouble(Double::parseDouble).boxed().distinct().sorted()
                .collect(Collectors.toList());
        detrimentAmountPercentage = BigDecimal.valueOf(detrimentAmountPercentageDouble);
        parallelTradesNumber = parallelTradesNumber > 0 ? parallelTradesNumber : 0;
        testRunDuration = parseDuration(testRunDurationParam.toUpperCase());
    }

    private void parseDurationParams() {
        staleIntervalDuration = parseDuration(staleInterval);
        dataCapturePeriodDuration = parseDuration(dataCapturePeriod);
        dataCaptureTimeFrame = TimeFrame.parse(dataCaptureTimeFrameParam);
        profitPercentageDecreaseAfterDuration = parseDuration(profitPercentageDecreaseAfter);
        tradeTimeoutDuration = parseDuration(tradeTimeout);
        suspenseAfterDetrimentalTradeDuration = parseDuration(suspenseAfterDetrimentalTradeDurationParam);
        testRunDuration = parseDuration(testRunDurationParam);
        exitDelayDuration = parseDuration(exitDelay);
    }

    private void validateInputParams() {
        if (isInvalidRequiredDurationParam(staleIntervalDuration)) {
            throw new IllegalArgumentException("Stale difference for ticker should be > 0!");
        }
        if (isInvalidRequiredDurationParam(dataCapturePeriodDuration)) {
            throw new IllegalArgumentException("Price data capture period should be > 0!");
        }
        if (dataCaptureTimeFrame == null) {
            throw new IllegalArgumentException(
                    String.format("Price data capture TimeFrame cannot be parsed! Valid values are: %s",
                                  Arrays.stream(TimeFrame.values()).map(Enum::name).collect(Collectors.joining(", "))));
        }
        if (dataCapturePeriodDuration.compareTo(dataCaptureTimeFrame.getDuration()) < 0) {
            throw new IllegalArgumentException("Price data capture period should be > Price data capture TimeFrame!");
        }
        if (entryProfitPercentageDouble < 0.0d) {
            throw new IllegalArgumentException("Trade entry profit percentage cannot be < 0!");
        }
        if (entryMinPercentageDiffDouble > entryMaxPercentageDiffDouble) {
            throw new IllegalArgumentException(
                    "Trade entry min percentage diff cannot be > entry max percentage diff!");
        }
        if (profitPercentageDecreaseByDouble >= (exitProfitPercentageDouble + entryProfitPercentageDouble)) {
            throw new IllegalArgumentException(
                    "Profit percentage decrease by cannot be >= entry + exit profit percentages!");
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
            throw new IllegalArgumentException("Trade entry sum cannot be < 10 USD");
        }
        if (detrimentAmountPercentageDouble <= 0) {
            throw new IllegalArgumentException("Detrimental exit amount percentage cannot be <= 0!");
        }
        if (detrimentAmountPercentageDouble >= 100) {
            throw new IllegalArgumentException("Detrimental exit amount percentage cannot be >= 100!");
        }
        if (isInvalidRequiredDurationParam(testRunDuration)) {
            throw new IllegalArgumentException("Test Run Duration parameter should be > 0!");
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
    }

    private void logParams() {
        log.info("--- APPLICATION PARAMETERS ---");

        log.info("Price data stale interval:  {}", formatDuration(staleIntervalDuration));
        log.info("Price data capture period:  {}", formatDuration(dataCapturePeriodDuration));
        log.info("Price data capture timeFrame:  {}", dataCaptureTimeFrame);
        log.info("----------------------------------------");
        log.info("Trade entry amounts (USD):  {}",
                 entryAmounts.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        log.info("Trade entry profit percentage:  {}", entryProfitPercentageDouble);
        log.info("Trade exit profit percentage:  {}", exitProfitPercentageDouble);
        log.info("Trade entry MIN percentage difference:  {}", entryMinPercentageDiffDouble);
        log.info("Trade entry MAX percentage difference:  {}", entryMaxPercentageDiffDouble);
        log.info("Trade decrease expected profit percentage after period:  {}",
                 profitPercentageDecreaseAfterDuration.isZero() || profitPercentageDecreaseByDouble == 0
                         ? DISABLED_MESSAGE
                         : formatDuration(profitPercentageDecreaseAfterDuration));
        log.info("Trade decrease expected profit percentage by:  {}",
                 profitPercentageDecreaseByDouble == 0 || profitPercentageDecreaseAfterDuration.isZero()
                         ? DISABLED_MESSAGE
                         : profitPercentageDecreaseByDouble);
        log.info("Trade timeout after period:  {}", formatDuration(tradeTimeoutDuration));
        log.info("Trade detrimental entry amount percentage:  {}", detrimentAmountPercentageDouble);
        log.info("Trade MAX parallel number:  {}", parallelTradesNumber == 0 ? DISABLED_MESSAGE : parallelTradesNumber);
        log.info("Trade suspense period after detrimental close:  {}", suspenseAfterDetrimentalTradeDuration.isZero()
                ? DISABLED_MESSAGE
                : formatDuration(suspenseAfterDetrimentalTradeDuration));
        log.info("----------------------------------------");
        log.info("Test Run result file (.csv) path:  {}", pathToResultDir);
        log.info("Test Run force exit code:  {}", exitCode);
        log.info("Test Run force exit file path:  {}", forcedExitFilePath);
        log.info("Test Run delay on exit period:  {}", formatDuration(exitDelayDuration));
        log.info("Test Run delete bars on exit: {}", deleteBarsOnExit);
        log.info("Test Run Duration:  {}\n", formatDuration(testRunDuration));
    }

    private boolean isInvalidRequiredDurationParam(Duration param) {
        return param == null || param.compareTo(Duration.ZERO) <= 0;
    }

    private Duration getDefaultDuration(Duration param) {
        if (param == null || param.compareTo(Duration.ZERO) < 0) {
            return Duration.ZERO;
        }
        return param;
    }
}
