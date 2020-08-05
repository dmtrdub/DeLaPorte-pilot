package my.dub.dlp_pilot.service.impl;

import my.dub.dlp_pilot.exception.MissingEntityException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

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
        exchanges.addAll(loadAll());
    }

    @Override
    public long countAll() {
        return exchanges.size();
    }

    @Override
    @Transactional
    public Set<Exchange> loadAll() {
        return CollectionUtils.toSet(repository.findAll());
    }

    @Override
    public Optional<Exchange> findById(Long id) {
        checkNotNull(id, "Exchange ID cannot be null when searching Exchange by ID!");

        return exchanges.stream().filter(exchange -> id.equals(exchange.getId())).findFirst();
    }

    @Override
    public Optional<Exchange> findByName(ExchangeName exchangeName) {
        checkNotNull(exchangeName, "Exchange name cannot be null when searching Exchange by name!");

        return exchanges.stream().filter(exchange -> exchangeName.equals(exchange.getName())).findFirst();
    }

    @Override
    public BigDecimal getTotalExpenses(ExchangeName exchangeName, BigDecimal tradeAmount) {
        Exchange exchange = findByName(exchangeName).orElseThrow();
        return exchange.getFixedFeesUsd()
                       .add(Calculations.originalValueFromPercent(tradeAmount, exchange.getTakerFeePercentage()));
    }

    @Override
    public void updateCachedExchangeFault(Exchange exchange, boolean faulty) {
        if (faulty == exchange.isFaulty()) {
            return;
        }
        exchange.setFaulty(faulty);
        boolean removed = exchanges.removeIf(exch -> Objects.equals(exch.getId(), exchange.getId()) &&
                Objects.equals(exch.getName(), exchange.getName()));
        if (removed) {
            exchanges.add(exchange);
        }
    }

    @Override
    public boolean isExchangeFaulty(ExchangeName exchangeName) {
        Exchange exchange = findByName(exchangeName)
                .orElseThrow(() -> new MissingEntityException(Exchange.class.getSimpleName(), exchangeName.name()));
        return exchange.isFaulty();
    }
}
