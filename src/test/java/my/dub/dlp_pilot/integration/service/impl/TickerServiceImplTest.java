package my.dub.dlp_pilot.integration.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import my.dub.dlp_pilot.Main;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TickerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = Main.class)
class TickerServiceImplTest {

    @MockBean
    private TickerContainer container;

    @Autowired
    private TickerService service;

    @Test
    void getTickerWithRetry() {
        ExchangeName exchangeName = ExchangeName.BITMAX;
        when(container.getTicker(any(ExchangeName.class), anyString(), anyString()))
                .thenThrow(MissingEntityException.class).thenThrow(MissingEntityException.class)
                .thenReturn(Optional.of(new Ticker(exchangeName)));
        service.getTickerWithRetry(exchangeName, "B", "T");
        verify(container, times(3)).getTicker(exchangeName, "B", "T");
    }
}
