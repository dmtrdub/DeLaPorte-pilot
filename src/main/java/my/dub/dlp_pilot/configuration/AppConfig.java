package my.dub.dlp_pilot.configuration;

import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "my.dub.dlp_pilot")
public class AppConfig {

    @Bean
    public CoinGeckoApiClientImpl coinGeckoApiClient() {
        return new CoinGeckoApiClientImpl();
    }
}
