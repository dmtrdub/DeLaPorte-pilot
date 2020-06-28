package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public interface ExchangeService {
    long countAll();

    Set<Exchange> findAll();

    Optional<Exchange> findById(Long id);

    Optional<Exchange> findByName(ExchangeName exchangeName);

    BigDecimal getTotalExpenses(ExchangeName exchangeName, BigDecimal tradeAmount);
}
