package org.ncsu.settleup.expenseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * Entry point for the expense service.  This service persists
 * expense data and publishes events so that other services can react
 * to new expenses.
 */
@SpringBootApplication
public class ExpenseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseServiceApplication.class, args);
    }

    /**
     * Configure a RestTemplate bean for calling other services (e.g. membership-service).
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
