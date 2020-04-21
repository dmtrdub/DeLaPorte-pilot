package my.dub.dlp_pilot.service.impl;

import my.dub.dlp_pilot.service.TradeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TradeServiceImpl implements TradeService {

    @Value("${transfer_delay_seconds}")
    private int transferDelaySeconds;

    @Override
    public void trade() {

    }
}
