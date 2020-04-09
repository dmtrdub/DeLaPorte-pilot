package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;

import java.util.Optional;
import java.util.Set;

public interface ExchangeService {
    long countAll();

    Set<Exchange> findAll();

    Optional<Exchange> findById(Long id);
}
