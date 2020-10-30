package my.dub.dlp_pilot.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;

public interface ExchangeService {
    long countAll();

    Set<Exchange> findAll();

    Optional<Exchange> findById(Long id);

    Exchange findByName(ExchangeName exchangeName);

    BigDecimal getTotalExpenses(ExchangeName exchangeName, BigDecimal tradeAmount);

    void updateExchangeFault(ExchangeName exchangeName, boolean faulty);

    boolean isExchangeFaulty(ExchangeName exchangeName);
}
