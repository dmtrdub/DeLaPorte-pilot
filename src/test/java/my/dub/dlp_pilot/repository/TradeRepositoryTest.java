package my.dub.dlp_pilot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class TradeRepositoryTest {

    private static final ExchangeName DEFAULT_EXCHANGE_NAME_SHORT = ExchangeName.BINANCE;
    private static final ExchangeName DEFAULT_EXCHANGE_NAME_LONG = ExchangeName.GATE;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TradeRepository repository;

    private Exchange exchangeShort;
    private Exchange exchangeLong;

    private TestRun testRun;

    @BeforeEach
    void setUp() {
        exchangeShort = persistExchange(DEFAULT_EXCHANGE_NAME_SHORT);
        exchangeLong = persistExchange(DEFAULT_EXCHANGE_NAME_LONG);
        testRun = persistTestRun();
    }

    @Test
    void findDistinctByWrittenToFileFalseAndTestRunIdEqualsOrderByEndTimeAsc() {
        Trade trade1 =
                entityManager.persist(createTrade("B", "T", TradeResultType.SUCCESSFUL, exchangeShort, exchangeLong));
        Trade trade2 =
                entityManager.persist(createTrade("C", "T", TradeResultType.DETRIMENTAL, exchangeShort, exchangeLong));
        Trade trade3 = entityManager.persist(
                createTrade("C", "TT", TradeResultType.TIMED_OUT, exchangeShort, persistExchange(ExchangeName.BITMAX)));
        Trade trade4 = createTrade("C", "TT", TradeResultType.TEST_RUN_END, exchangeShort, exchangeLong);
        trade4.setWrittenToFile(true);
        entityManager.persist(trade4);

        then(repository.findDistinctByWrittenToFileFalseAndTestRunIdEqualsOrderByEndTimeAsc(testRun.getId())).hasSize(3)
                .containsExactly(trade1, trade2, trade3);
    }

    @Test
    void checkSimilarExists() {
        Trade trade1 =
                entityManager.persist(createTrade("B", "T", TradeResultType.SUCCESSFUL, exchangeShort, exchangeLong));
        entityManager.persist(createTrade("D", "TT", TradeResultType.DETRIMENTAL, exchangeShort, exchangeLong));

        assertThat(repository.checkSimilarExists(trade1.getBase(), trade1.getTarget(), DEFAULT_EXCHANGE_NAME_SHORT,
                                                 DEFAULT_EXCHANGE_NAME_LONG, trade1.getPositionShort().getOpenPrice(),
                                                 trade1.getPositionLong().getOpenPrice(), trade1.getResultType()))
                .isTrue();
    }

    private Trade createTrade(String base, String target, TradeResultType resultType, Exchange exchangeShort,
            Exchange exchangeLong) {
        Trade trade = new Trade();
        trade.setBase(base);
        trade.setTarget(target);
        trade.setFixedExpensesUsd(BigDecimal.ZERO);
        trade.setStartTime(Instant.now().minusSeconds(30));
        trade.setEndTime(Instant.now());
        trade.setEntryPercentageDiff(BigDecimal.ONE);
        trade.setAveragePriceDiff(BigDecimal.ONE);
        trade.setOpenPriceDiff(BigDecimal.ONE);
        trade.setClosePriceDiff(BigDecimal.ONE);
        trade.setResultType(resultType);
        trade.setTotalExpensesUsd(BigDecimal.ONE);
        trade.setIncomeUsd(BigDecimal.ZERO);
        trade.setWrittenToFile(false);
        trade.setPositions(persistPosition(exchangeShort, PositionSide.SHORT),
                           persistPosition(exchangeLong, PositionSide.LONG));
        trade.setTestRun(testRun);
        return trade;
    }

    private Position persistPosition(Exchange exchange, PositionSide side) {
        Position position = new Position();
        position.setSide(side);
        position.setOpenPrice(BigDecimal.ONE);
        position.setPnlUsd(BigDecimal.ONE);
        position.setExchange(exchange);
        return entityManager.persist(position);
    }

    private Exchange persistExchange(ExchangeName exchangeName) {
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
        return entityManager.persist(exchange);
    }

    private TestRun persistTestRun() {
        TestRun testRun = new TestRun();
        testRun.setConfigParams("params");
        testRun.setStartTime(LocalDateTime.now());
        testRun.setPreloadStartTime(LocalDateTime.now());
        return entityManager.persist(testRun);
    }
}