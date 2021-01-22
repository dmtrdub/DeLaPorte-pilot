package my.dub.dlp_pilot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TradeService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
class FileResultServiceImplTest {

    @Captor
    private ArgumentCaptor<String> filePathCaptor;

    @Mock
    private TradeService tradeService;
    @Mock
    private TestRunService testRunService;
    @Mock
    private ParametersHolder parameters;

    @InjectMocks
    private FileResultServiceImpl service;

    private TestRun testRun;

    @TempDir
    File resultDir;

    @BeforeEach
    void setUp() {
        when(parameters.getEntryAmountUsdDouble()).thenReturn(1.0d);
        testRun = new TestRun();
        testRun.setId(100L);
        testRun.setStartTime(LocalDateTime.now());
        when(testRunService.getCurrentTestRun()).thenReturn(testRun);
        when(parameters.getPathToResultDir()).thenReturn(resultDir.getAbsolutePath());
    }

    @Test
    void init() {
        service.init();
        verify(testRunService).updateResultFile(filePathCaptor.capture());
        String filePath = filePathCaptor.getValue();
        assertThat(filePath).contains(String.valueOf(testRun.getId()));
    }

    @Test
    void write_testRunEndException() {
        when(tradeService.isAllTradesClosed()).thenReturn(true);
        when(testRunService.checkTestRunEnd()).thenReturn(true);

        assertThrows(TestRunEndException.class, () -> service.write());
    }

    @Test
    void write() throws IOException {
        Trade trade = new Trade();
        trade.setBase("B");
        trade.setTarget("T");
        trade.setFixedExpensesUsd(BigDecimal.ZERO);
        trade.setStartTime(Instant.now().minusSeconds(30));
        trade.setEndTime(Instant.now());
        trade.setEntryPercentageDiff(BigDecimal.ONE);
        trade.setAveragePriceDiff(BigDecimal.ONE);
        trade.setOpenPriceDiff(BigDecimal.ONE);
        trade.setClosePriceDiff(BigDecimal.ONE);
        trade.setResultType(TradeResultType.SUCCESSFUL);
        trade.setTotalExpensesUsd(BigDecimal.ONE);
        trade.setIncomeUsd(BigDecimal.ZERO);
        trade.setWrittenToFile(false);
        trade.setPositions(createPosition(createExchange(ExchangeName.BITMAX), PositionSide.SHORT),
                           createPosition(createExchange(ExchangeName.BINANCE), PositionSide.LONG));
        List<Trade> trades = List.of(trade);
        when(tradeService.getCompletedTradesNotWrittenToFile(testRun)).thenReturn(trades);
        service.init();

        service.write();
        verify(tradeService).saveOrUpdate(trades);
        assertThat(trades).matches(trds -> trds.stream().allMatch(Trade::getWrittenToFile));
        List<String> writtenLines = Files.readAllLines((Path) ReflectionTestUtils.getField(service, "filePath"));
        assertThat(writtenLines).hasSize(2);
        assertThat(writtenLines.get(1)).contains(trade.getBase()).contains(trade.getTarget())
                .contains(trade.getResultType().name());
    }

    private Position createPosition(Exchange exchange, PositionSide side) {
        Position position = new Position();
        position.setSide(side);
        position.setOpenPrice(BigDecimal.ONE);
        position.setClosePrice(BigDecimal.ONE);
        position.setMinPnlUsd(BigDecimal.ZERO);
        position.setMinPnlTime(Instant.now().minusSeconds(10));
        position.setPnlUsd(BigDecimal.ZERO);
        position.setMaxPnlUsd(BigDecimal.ONE);
        position.setMaxPnlTime(Instant.now().minusSeconds(5));
        position.setExchange(exchange);
        return position;
    }

    private Exchange createExchange(ExchangeName exchangeName) {
        Exchange exchange = new Exchange();
        exchange.setBaseEndpoint(RandomStringUtils.randomAlphabetic(10));
        exchange.setName(exchangeName);
        exchange.setDepositFeeUsd(BigDecimal.ONE);
        exchange.setWithdrawFeeUsd(BigDecimal.ONE);
        exchange.setTakerFeePercentage(BigDecimal.ZERO);
        exchange.setMaxBarsPerRequest(100);
        exchange.setTrustScore(5);
        exchange.setApiRequestsPerMin(50);
        exchange.setApiRequestsPerMinPreload(50);
        exchange.setAscendingPreload(false);
        return exchange;
    }
}