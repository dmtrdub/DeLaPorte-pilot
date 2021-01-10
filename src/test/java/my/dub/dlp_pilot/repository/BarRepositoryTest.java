package my.dub.dlp_pilot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.LastBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class BarRepositoryTest {
    private static final ExchangeName DEFAULT_EXCHANGE = ExchangeName.BINANCE;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BarRepository repository;

    private TestRun testRun;

    @BeforeEach
    void beforeAll() {
        testRun = persistTestRun();
    }

    @Test
    void deleteAllByTestRunIdEquals() {
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.ONE, Instant.now().minusSeconds(30), testRun);
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.TEN, Instant.now(), testRun);
        TestRun testRun2 = new TestRun();
        testRun2.setConfigParams("params2");
        testRun2.setStartTime(LocalDateTime.now());
        testRun2.setPreloadStartTime(LocalDateTime.now());
        TestRun persistedTestRun2 = entityManager.persist(testRun2);
        persistBar(ExchangeName.GATE, "C", "T2", BigDecimal.ONE, Instant.now(), persistedTestRun2);

        then(repository.deleteAllByTestRunIdEquals(testRun.getId())).isEqualTo(2L);
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void getAllBarAverages() {
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.ONE, Instant.now().minusSeconds(30), testRun);
        Instant closeTime1 = Instant.now();
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.TEN, closeTime1, testRun);
        persistBar(DEFAULT_EXCHANGE, "C", "T2", BigDecimal.valueOf(2), Instant.now().minusSeconds(30), testRun);
        Instant closeTime2 = Instant.now().minusSeconds(10);
        persistBar(DEFAULT_EXCHANGE, "C", "T2", BigDecimal.valueOf(3), closeTime2, testRun);
        TestRun testRun2 = new TestRun();
        testRun2.setConfigParams("params2");
        testRun2.setStartTime(LocalDateTime.now());
        testRun2.setPreloadStartTime(LocalDateTime.now());
        TestRun persistedTestRun2 = entityManager.persist(testRun2);
        persistBar(DEFAULT_EXCHANGE, "C", "T2", BigDecimal.ONE, Instant.now(), persistedTestRun2);

        List<BarAverage> result = repository.getAllBarAverages(testRun.getId());
        List<Long> resultEpochMillis = List.of(closeTime1.toEpochMilli(), closeTime2.toEpochMilli());
        assertThat(result).hasSize(2).matches(barAverages -> barAverages.stream()
                .allMatch(bA -> resultEpochMillis.contains(bA.getLastCloseTime().toEpochMilli())));
    }

    @Test
    void getBarAverages() {
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.ONE, Instant.now().minusSeconds(30), testRun);
        Instant closeTime = Instant.now();
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.TEN, closeTime, testRun);
        persistBar(ExchangeName.GATE, "B", "T", BigDecimal.valueOf(5), Instant.now().minusSeconds(20), testRun);
        persistBar(ExchangeName.GATE, "B", "T", BigDecimal.valueOf(5), Instant.now(), testRun);
        TestRun testRun2 = new TestRun();
        testRun2.setConfigParams("params2");
        testRun2.setStartTime(LocalDateTime.now());
        testRun2.setPreloadStartTime(LocalDateTime.now());
        TestRun persistedTestRun2 = entityManager.persist(testRun2);
        persistBar(DEFAULT_EXCHANGE, "C", "T2", BigDecimal.ONE, Instant.now(), persistedTestRun2);

        List<BarAverage> result = repository.getBarAverages(DEFAULT_EXCHANGE, testRun.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastCloseTime().toEpochMilli()).isEqualTo(closeTime.toEpochMilli());
    }

    @Test
    void getLastBars() {
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.ONE, Instant.now().minusSeconds(30), testRun);
        Instant closeTime1 = Instant.now();
        persistBar(DEFAULT_EXCHANGE, "B", "T", BigDecimal.TEN, closeTime1, testRun);
        persistBar(DEFAULT_EXCHANGE, "C", "T2", BigDecimal.valueOf(2), Instant.now().minusSeconds(30), testRun);
        Instant closeTime2 = Instant.now().minusSeconds(10);
        persistBar(DEFAULT_EXCHANGE, "C", "T2", BigDecimal.valueOf(3), closeTime2, testRun);

        List<LastBar> result = repository.getLastBars(DEFAULT_EXCHANGE, testRun.getId());
        List<Long> resultEpochMillis = List.of(closeTime1.toEpochMilli(), closeTime2.toEpochMilli());
        assertThat(result).hasSize(2).matches(barAverages -> barAverages.stream()
                .allMatch(lastBar -> resultEpochMillis.contains(lastBar.getCloseTime().toEpochMilli())));
    }

    private TestRun persistTestRun() {
        TestRun testRun = new TestRun();
        testRun.setConfigParams("params");
        testRun.setStartTime(LocalDateTime.now());
        testRun.setPreloadStartTime(LocalDateTime.now());
        return entityManager.persist(testRun);
    }

    private Bar persistBar(ExchangeName exchangeName, String base, String target, BigDecimal ohlcv, Instant closeTime,
            TestRun testRun) {
        Bar bar = new Bar(exchangeName, base, target);
        bar.setOpen(ohlcv);
        bar.setClose(ohlcv);
        bar.setHigh(ohlcv);
        bar.setLow(ohlcv);
        bar.setVolume(ohlcv);
        bar.setCloseTime(closeTime);
        bar.setTestRun(testRun);
        return entityManager.persist(bar);
    }
}