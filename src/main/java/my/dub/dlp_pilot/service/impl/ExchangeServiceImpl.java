package my.dub.dlp_pilot.service.impl;

import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.repository.ExchangeRepository;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ExchangeServiceImpl implements ExchangeService {

    private final ExchangeRepository repository;

    @Autowired
    public ExchangeServiceImpl(ExchangeRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countAll() {
        return repository.count();
    }

    @Override
    public Set<Exchange> findAll() {
        return CollectionUtils.toSet(repository.findAll());
    }

    @Override
    public Optional<Exchange> findById(Long id) {
        if(id == null) {
            return Optional.empty();
        }
        return repository.findById(id);
    }
}
