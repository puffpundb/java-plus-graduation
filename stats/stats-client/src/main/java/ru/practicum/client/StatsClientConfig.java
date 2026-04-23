package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class StatsClientConfig {
//    @Value("${stats.service.url:http://stats-server:9090}")
//    private String statsServiceUrl;
//
//    @LoadBalanced
//    @Bean
//    public RestClient restClient() {
//        log.info("Настройка RestClient для статистики по URL: {}", statsServiceUrl);
//        return RestClient.builder()
//                .baseUrl(statsServiceUrl)
//                .defaultHeader("User-Agent", "Stats-Client/1.0")
//                .build();
//    }

    private final String statsServiceUrl = "http://stats-server";

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient restClient(RestClient.Builder loadBalancedRestClientBuilder) {
        return loadBalancedRestClientBuilder
                .baseUrl(statsServiceUrl)
                .defaultHeader("User-Agent", "Stats-Client/1.0")
                .build();
    }
}
