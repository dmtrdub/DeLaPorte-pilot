package my.dub.dlp_pilot.service.impl;

import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.model.Transfer;
import my.dub.dlp_pilot.model.TransferStatus;
import my.dub.dlp_pilot.repository.TransferRepository;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TransferService;
import my.dub.dlp_pilot.util.Calculations;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

import static my.dub.dlp_pilot.util.DateUtils.currentDateTime;
import static my.dub.dlp_pilot.util.NumberUtils.getPercentResult;

@Service
public class TransferServiceImpl implements TransferService, InitializingBean {

    @Value("${transfer_delay_seconds}")
    private int transferDelaySeconds;
    @Value("${exchange_deposit_sum_usd}")
    private double exchangeDepositSumUsd;
    @Value("${trade_entry_diff_percentage}")
    private double entryPercentage;

    private BigDecimal entrySumUsd;

    private final TransferRepository repository;
    private final TickerContainer container;
    private final TickerService tickerService;

    @Autowired
    public TransferServiceImpl(TransferRepository repository,
                               TickerContainer container, TickerService tickerService) {
        this.repository = repository;
        this.container = container;
        this.tickerService = tickerService;
    }

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
        entrySumUsd = getPercentResult(exchangeDepositSumUsd, entryPercentage);
    }

    private void validateInputParams() {
        if (transferDelaySeconds < 0) {
            throw new IllegalArgumentException("Transfer delay cannot be < 0!");
        }
    }

    @Transactional
    @Override
    public void save(Collection<Transfer> transfers) {
        if (CollectionUtils.isEmpty(transfers)) {
            return;
        }
        repository.saveAll(transfers);
    }

    @Override
    public Transfer create(Ticker ticker1, Ticker ticker2) {
        Transfer transfer = new Transfer();
        transfer.setBase1(ticker1.getBase());
        transfer.setBase2(ticker2.getBase());
        transfer.setTarget1(ticker1.getTarget());
        transfer.setTarget2(ticker2.getTarget());
        transfer.setSumEach(BigDecimal.valueOf(exchangeDepositSumUsd));
        transfer.setBeginTime(currentDateTime());
        transfer.setEndTime(transfer.getBeginTime().plusSeconds(transferDelaySeconds));
        transfer.setRecipient1(ticker1.getExchange());
        transfer.setRecipient2(ticker2.getExchange());
        transfer.setStatus(TransferStatus.PROGRESS);
        return transfer;
    }

    @Transactional
    @Override
    public Set<Transfer> findEndingTransfers() {
        return repository.findByStatusEqualsAndEndTimeBefore(TransferStatus.PROGRESS, currentDateTime());
    }

    @Override
    public void handleTradeOpportunities() {
        Map<Long, List<Ticker>> tickersMap = container.getExchangeIDTickersMap();
        Iterator<List<Ticker>> iterator = tickersMap.values().iterator();
        Set<Transfer> transfers = new HashSet<>();
        while (iterator.hasNext()) {
            List<Ticker> next = iterator.next();
            for (List<Ticker> value : tickersMap.values()) {
                if (next.equals(value)) {
                    continue;
                }
                transfers.addAll(compareExchangeTickers(next, value));
            }
        }
        save(transfers);
    }

    private List<Transfer> compareExchangeTickers(Collection<Ticker> tickersExchange1,
                                                  Collection<Ticker> tickersExchange2) {
        List<Transfer> transfers = new ArrayList<>();
        tickersExchange1.forEach(ticker -> tickersExchange2.stream().filter(ticker2 -> tickerService
                .isPairEquivalent(ticker.getBase(), ticker.getTarget(), ticker2.getBase(), ticker2.getTarget()) &&
                Calculations.isEntryProfitable(ticker, ticker2, exchangeDepositSumUsd, entrySumUsd))
                .forEach(result2 -> transfers.add(create(ticker, result2))));
        return transfers;
    }
}
