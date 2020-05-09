package my.dub.dlp_pilot.service.impl;

import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.model.Transfer;
import my.dub.dlp_pilot.model.TransferStatus;
import my.dub.dlp_pilot.repository.TransferRepository;
import my.dub.dlp_pilot.service.TickerService;
import my.dub.dlp_pilot.service.TransferService;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.DateUtils;
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
    @Value("${trade_ticker_max_difference_seconds}")
    private int tickersTimeMaxDifferenceSeconds;

    private BigDecimal entrySumUsd;

    private final TransferRepository repository;
    private final TickerService tickerService;

    @Autowired
    public TransferServiceImpl(TransferRepository repository,
                               TickerService tickerService) {
        this.repository = repository;
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
    public void createTransfers() {
        Map<Long, Set<Ticker>> tickersMap = tickerService.getExchangeIDTickersMap();
        Iterator<Set<Ticker>> iterator = tickersMap.values().iterator();
        Set<Transfer> transfers = new HashSet<>();
        while (iterator.hasNext()) {
            Set<Ticker> next = iterator.next();
            for (Set<Ticker> value : tickersMap.values()) {
                if (next.equals(value)) {
                    continue;
                }
                transfers.addAll(compareExchangeTickers(next, value));
            }
        }
        save(transfers);
    }

    //TODO: fix concurrency exception
    private List<Transfer> compareExchangeTickers(Collection<Ticker> tickersExchange1,
                                                  Collection<Ticker> tickersExchange2) {
        List<Transfer> transfers = new ArrayList<>();
        for (Ticker ticker : tickersExchange1) {
            tickersExchange2.stream().filter(ticker2 -> canBeCompared(ticker, ticker2))
                    .forEach(result2 -> transfers.add(create(ticker, result2)));
        }
        return transfers;
    }

    private boolean canBeCompared(Ticker ticker, Ticker ticker2) {
        return tickerService
                .isPairEquivalent(ticker.getBase(), ticker.getTarget(), ticker2.getBase(), ticker2.getTarget()) &&
                tickersSafe(ticker, ticker2) &&
                Calculations.isEntryProfitable(ticker, ticker2, exchangeDepositSumUsd, entrySumUsd) && DateUtils
                .durationSeconds(ticker.getTime(), ticker2.getTime()) <= tickersTimeMaxDifferenceSeconds;
    }

    private boolean tickersSafe(Ticker ticker1, Ticker ticker2) {
        return ticker1 != null && !(ticker1.isStale() && ticker1.isAnomaly()) && ticker2 != null &&
                !(ticker2.isStale() && ticker2.isAnomaly());
    }
}
