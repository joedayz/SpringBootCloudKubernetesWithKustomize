package se.magnus.springcloud.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;

import static java.util.logging.Level.FINE;

@Configuration
public class HealthCheckConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckConfiguration.class);

    private HealthAggregator healthAggregator;

    private final WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @Autowired
    public HealthCheckConfiguration(
        WebClient.Builder webClientBuilder,
        HealthAggregator healthAggregator
    ) {
        this.webClientBuilder = webClientBuilder;
        this.healthAggregator = healthAggregator;
    }

    @Bean
    ReactiveHealthIndicator healthcheckMicroservices() {

        ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry(new LinkedHashMap<>());

        registry.register("auth-server",       () -> getHealth("http://auth-server:4004"));
        registry.register("product",           () -> getHealth("http://product:4004"));
        registry.register("recommendation",    () -> getHealth("http://recommendation:4004"));
        registry.register("review",            () -> getHealth("http://review:4004"));
        registry.register("product-composite", () -> getHealth("http://product-composite:4004"));

        return new CompositeReactiveHealthIndicator(healthAggregator, registry);
   	}

    private Mono<Health> getHealth(String url) {
        url += "/actuator/health";
        LOG.debug("Will call the Health API on URL: {}", url);
        return getWebClient().get().uri(url).retrieve().bodyToMono(String.class)
            .map(s -> new Health.Builder().up().build())
            .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
            .log(null, FINE);
    }

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder.build();
        }
        return webClient;
    }
}
