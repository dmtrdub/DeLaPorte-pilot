package my.dub.dlp_pilot.repository.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SymbolPairContainerTest {

    private static final List<SymbolPair> DATA =
            List.of(new SymbolPair(ExchangeName.BINANCE, "AAA/BBB"), new SymbolPair(ExchangeName.BINANCE, "AAA/CCC"),
                    new SymbolPair(ExchangeName.HUOBI, "AAA/BBB"), new SymbolPair(ExchangeName.BITFINEX, "AAA/ZZZ"));

    private SymbolPairContainer container;

    @BeforeEach
    void setUp() {
        container = new SymbolPairContainer();
    }

    @Test
    void addAll() {
        container.addAll(DATA);

        assertThat(container.getAll()).hasSize(4);
        assertThat(container.getAll(ExchangeName.BINANCE)).hasSize(2);
    }

    @Test
    void remove_exchangeNotExists() {
        container.addAll(DATA);

        int prevSize = container.getAll().size();
        container.remove(ExchangeName.GATE, 1);

        assertEquals(prevSize, container.getAll().size());
    }

    @Test
    void remove() {
        container.addAll(DATA);

        int prevSize = container.getAll().size();
        container.remove(ExchangeName.BINANCE, 0);

        assertEquals(prevSize - 1, container.getAll().size());
    }
}