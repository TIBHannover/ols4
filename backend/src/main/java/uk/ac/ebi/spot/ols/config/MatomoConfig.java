package uk.ac.ebi.spot.ols.config;

import org.matomo.java.tracking.MatomoTracker;
import org.matomo.java.tracking.TrackerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class MatomoConfig {
    @Bean
    public MatomoTracker matomoTracker() {
        TrackerConfiguration config = TrackerConfiguration.builder()
                .apiEndpoint(URI.create("https://support.tib.eu/piwik/matomo.php"))
                .defaultSiteId(35)
                .defaultAuthToken("your-token-here") // Set your default Site ID here
                .build();


        return new MatomoTracker(config);
    }
}
