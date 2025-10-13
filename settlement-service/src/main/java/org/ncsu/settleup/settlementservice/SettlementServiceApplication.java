package org.ncsu.settleup.settlementservice;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * Entry point for the settlement service.  This service listens for
 * expense events, maintains group balances in-memory and computes
 * settlement plans on demand.
 */
@SpringBootApplication
@EnableRabbit
public class SettlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementServiceApplication.class, args);
    }

    /**
     * RestTemplate bean to call other services (e.g. membership-service).
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
