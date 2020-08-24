package my.dub.dlp_pilot.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;

public interface ExchangeService {
    long countAll();

    Set<Exchange> loadAll();

    Optional<Exchange> findById(Long id);

    Exchange findByName(ExchangeName exchangeName);

    BigDecimal getTotalExpenses(ExchangeName exchangeName, BigDecimal tradeAmount);

    void updateCachedExchangeFault(Exchange exchange, boolean faulty);

    boolean isExchangeFaulty(ExchangeName exchangeName);
}
