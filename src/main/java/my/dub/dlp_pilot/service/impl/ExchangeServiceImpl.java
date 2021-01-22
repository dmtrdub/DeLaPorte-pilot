package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.repository.ExchangeRepository;
import my.dub.dlp_pilot.service.ExchangeService;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * An implementation of {@link ExchangeService} service storing {@link Exchange} data cached in internal collections.
 */
@Service
@Transactional
public class ExchangeServiceImpl implements ExchangeService {

    private static final String EXCHANGE_NAME_PARAMETER = "exchangeName";

    private final Set<Exchange> exchanges = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<ExchangeName> faultyExchangeNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final ExchangeRepository repository;

    @Autowired
    public ExchangeServiceImpl(ExchangeRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countAll() {
        return exchanges.size();
    }

    @Override
    public Set<Exchange> findAll() {
        if (exchanges.isEmpty()) {
            exchanges.addAll(CollectionUtils.toSet(repository.findAll()));
        }
        return Collections.unmodifiableSet(exchanges);
    }

    @Override
    public Optional<Exchange> findById(@NonNull Long id) {
        checkNotNull(id, Constants.NULL_ARGUMENT_MESSAGE, "id");

        return exchanges.stream().filter(exchange -> id.equals(exchange.getId())).findFirst();
    }

    @Override
    public Exchange findByName(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);

        return findAll().stream().filter(exchange -> exchangeName.equals(exchange.getName())).findFirst()
                .orElseThrow(() -> new MissingEntityException(ExchangeName.class, exchangeName.name()));
    }

    @Override
    public BigDecimal getTotalExpenses(@NonNull ExchangeName exchangeName, @NonNull BigDecimal tradeAmount) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);
        checkNotNull(tradeAmount, Constants.NULL_ARGUMENT_MESSAGE, "tradeAmount");

        Exchange exchange = findByName(exchangeName);
        return exchange.getFixedFeesUsd()
                .add(Calculations.originalValueFromPercent(tradeAmount, exchange.getTakerFeePercentage()));
    }

    @Override
    public void updateExchangeFault(@NonNull ExchangeName exchangeName, @NonNull boolean faulty) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER);

        if (faulty && !faultyExchangeNames.contains(exchangeName)) {
            faultyExchangeNames.add(exchangeName);
        } else if (!faulty) {
            faultyExchangeNames.remove(exchangeName);
        }
    }

    @Override
    public boolean isExchangeFaulty(@NonNull ExchangeName exchangeName) {
        return faultyExchangeNames
                .contains(checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, EXCHANGE_NAME_PARAMETER));
    }
}
