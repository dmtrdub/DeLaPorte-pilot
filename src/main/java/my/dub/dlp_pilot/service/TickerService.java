package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;

public interface TickerService {

    void fetchMarketData(Exchange exchange);

    boolean isPairEquivalent(String baseOriginal, String targetOriginal, String baseCompared, String targetCompared);
}
