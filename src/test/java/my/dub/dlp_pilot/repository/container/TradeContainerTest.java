package my.dub.dlp_pilot.repository.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.dto.DetrimentalRecord;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

class TradeContainerTest {

    public static final ExchangeName DEFAULT_EXCHANGE_NAME = ExchangeName.BINANCE;
    private TradeContainer tradeContainer;

    @BeforeEach
    void setUp() {
        tradeContainer = new TradeContainer();
    }

    @Test
    void addTrade_similar() {
        Map<Long, Trade> trades = (Map<Long, Trade>) ReflectionTestUtils.getField(tradeContainer, "trades");
        Trade existing = createTrade();
        trades.put(0L, existing);
        Trade newTrade = SerializationUtils.clone(existing);

        then(tradeContainer.addTrade(newTrade)).isFalse();

        assertThat(tradeContainer.getTrades(DEFAULT_EXCHANGE_NAME)).hasSize(1);
    }

    @Test
    void addTrade() {
        then(tradeContainer.addTrade(createTrade())).isTrue();
        assertThat(tradeContainer.getTrades(DEFAULT_EXCHANGE_NAME)).hasSize(1);
    }

    @Test
    void tradesCount() {
        Trade trade1, trade2, trade3, trade4;
        trade1 = createTrade();
        trade2 = createTrade();
        trade3 = createTrade();
        trade4 = createTrade();
        Exchange exchange2 = new Exchange();
        exchange2.setName(ExchangeName.BIGONE);
        Exchange exchange3 = new Exchange();
        exchange3.setName(ExchangeName.BITMAX);
        trade2.getPositionLong().setExchange(exchange2);
        trade3.getPositionShort().setExchange(exchange3);
        trade4.getPositionLong().setExchange(exchange3);
        trade4.getPositionShort().setExchange(exchange2);
        tradeContainer.addTrade(trade1);
        tradeContainer.addTrade(trade2);
        tradeContainer.addTrade(trade3);
        tradeContainer.addTrade(trade4);

        Pair<Long, Long> result = tradeContainer.tradesCount(DEFAULT_EXCHANGE_NAME, exchange2.getName());
        assertEquals(3L, result.getFirst());
        assertEquals(2L, result.getSecond());
    }

    @Test
    void isSimilarPresent() {
        Map<Long, Trade> trades = (Map<Long, Trade>) ReflectionTestUtils.getField(tradeContainer, "trades");
        Trade existing = createTrade();
        trades.put(0L, existing);
        Trade existing2 = createTrade();
        existing2.setBase("C");
        Exchange exchange = new Exchange();
        exchange.setName(ExchangeName.BIGONE);
        existing2.getPositionShort().setExchange(exchange);
        trades.put(1L, existing2);

        assertTrue(tradeContainer.isSimilarPresent(existing.getBase(), existing.getTarget(),
                                                   existing.getPositionLong().getExchange().getName(),
                                                   existing.getPositionShort().getExchange().getName()));
    }

    @Test
    void checkDetrimentalRecord_invalidate() {
        Set<DetrimentalRecord> detrimentalRecords =
                (Set<DetrimentalRecord>) ReflectionTestUtils.getField(tradeContainer, "detrimentalRecords");
        DetrimentalRecord existing = new DetrimentalRecord(ExchangeName.BINANCE, ExchangeName.HUOBI, "B", "T",
                                                           Instant.now().minus(5, ChronoUnit.MINUTES));
        DetrimentalRecord existing2 =
                new DetrimentalRecord(ExchangeName.BINANCE, ExchangeName.HUOBI, "C", "T", Instant.now());
        detrimentalRecords.add(existing);
        detrimentalRecords.add(existing2);

        assertFalse(tradeContainer.checkDetrimentalRecord(ExchangeName.BINANCE, ExchangeName.HUOBI, "B", "T"));
        assertThat(detrimentalRecords).hasSize(1);
    }

    @Test
    void checkDetrimentalRecord() {
        Set<DetrimentalRecord> detrimentalRecords =
                (Set<DetrimentalRecord>) ReflectionTestUtils.getField(tradeContainer, "detrimentalRecords");
        DetrimentalRecord existing = new DetrimentalRecord(ExchangeName.BINANCE, ExchangeName.HUOBI, "B", "T",
                                                           Instant.now().plus(5, ChronoUnit.MINUTES));
        DetrimentalRecord existing2 = new DetrimentalRecord(ExchangeName.BINANCE, ExchangeName.HUOBI, "C", "T",
                                                            Instant.now().plus(5, ChronoUnit.MINUTES));
        detrimentalRecords.add(existing);
        detrimentalRecords.add(existing2);

        assertTrue(tradeContainer.checkDetrimentalRecord(ExchangeName.BINANCE, ExchangeName.HUOBI, "B", "T"));
        assertThat(detrimentalRecords).hasSize(2);
    }

    private Trade createTrade() {
        Trade existing = new Trade();
        existing.setBase("B");
        existing.setTarget("T");
        Exchange exchange1 = new Exchange();
        exchange1.setName(DEFAULT_EXCHANGE_NAME);
        Position shortPos = new Position();
        shortPos.setExchange(exchange1);
        Exchange exchange2 = new Exchange();
        exchange2.setName(ExchangeName.HUOBI);
        Position longPos = new Position();
        longPos.setExchange(exchange1);
        existing.setPositionShort(shortPos);
        existing.setPositionLong(longPos);
        return existing;
    }
}