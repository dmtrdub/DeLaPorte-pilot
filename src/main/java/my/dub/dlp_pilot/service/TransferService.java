package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.model.Transfer;

import java.util.Collection;
import java.util.Set;

public interface TransferService {
    void save(Collection<Transfer> transfers);

    Transfer create(Ticker ticker1, Ticker ticker2);

    Set<Transfer> findEndingTransfers();

    void createTransfers();
}
