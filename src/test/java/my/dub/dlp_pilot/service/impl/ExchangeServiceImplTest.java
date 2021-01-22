package my.dub.dlp_pilot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Set;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.repository.ExchangeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
class ExchangeServiceImplTest {

    @Mock
    private ExchangeRepository repository;

    @InjectMocks
    private ExchangeServiceImpl service;

    @Test
    void findAll() {
        Exchange exchange = new Exchange();
        exchange.setName(ExchangeName.BITMAX);
        Exchange exchange2 = new Exchange();
        exchange2.setName(ExchangeName.HUOBI);
        when(repository.findAll()).thenReturn(Set.of(exchange, exchange2));

        Set<Exchange> result = service.findAll();
        assertThat(result).contains(exchange, exchange2);
        assertThat((Set<Exchange>) ReflectionTestUtils.getField(service, "exchanges")).contains(exchange, exchange2);
        assertThrows(UnsupportedOperationException.class, () -> result.remove(exchange));
    }

    @Test
    void findByName_missingEntityException() {
        Exchange exchange = new Exchange();
        exchange.setName(ExchangeName.BITMAX);
        Exchange exchange2 = new Exchange();
        exchange2.setName(ExchangeName.HUOBI);
        Set<Exchange> exchanges = (Set<Exchange>) ReflectionTestUtils.getField(service, "exchanges");
        exchanges.addAll(Set.of(exchange, exchange2));

        assertThrows(MissingEntityException.class, () -> service.findByName(ExchangeName.GATE));
    }

    @Test
    void updateExchangeFault() {
        service.updateExchangeFault(ExchangeName.GATE, true);
        service.updateExchangeFault(ExchangeName.BINANCE, true);

        service.updateExchangeFault(ExchangeName.GATE, false);
        assertThat(service.isExchangeFaulty(ExchangeName.GATE)).isFalse();
        assertThat((Set<ExchangeName>) ReflectionTestUtils.getField(service, "faultyExchangeNames")).hasSize(1);
    }
}