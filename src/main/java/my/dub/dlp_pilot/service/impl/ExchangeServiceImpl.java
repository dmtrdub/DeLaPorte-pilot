package my.dub.dlp_pilot.service.impl;

import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.repository.ExchangeRepository;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeServiceImpl implements ExchangeService, InitializingBean {

    private final Set<Exchange> exchanges = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final ExchangeRepository repository;

    @Autowired
    public ExchangeServiceImpl(ExchangeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void afterPropertiesSet() {
        exchanges.addAll(findAll());
    }

    @Override
    public long countAll() {
        return exchanges.size();
    }

    @Override
    @Transactional
    public Set<Exchange> findAll() {
        return CollectionUtils.toSet(repository.findAll());
    }

    @Override
    public Optional<Exchange> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return exchanges.stream().filter(exchange -> id.equals(exchange.getId())).findFirst();
    }

    @Override
    public Optional<Exchange> findByName(ExchangeName exchangeName) {
        if (exchangeName == null) {
            return Optional.empty();
        }
        return exchanges.stream().filter(exchange -> exchangeName.equals(exchange.getName())).findFirst();
    }

    @Override
    public BigDecimal getTotalExpenses(ExchangeName exchangeName, BigDecimal tradeAmount) {
        Exchange exchange = findByName(exchangeName).orElseThrow();
        return exchange.getFixedFeesUsd()
                       .add(Calculations.originalValueFromPercent(tradeAmount, exchange.getTakerFeePercentage()));
    }
}
