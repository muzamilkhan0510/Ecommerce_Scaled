package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CronConfig {

    @Value("${cron.expression}")
    private String cronValue;

    @Value("${cron.responder.expression}")
    private String cronResponderValue;

    @Bean
    public String cronBean() {
        return this.cronValue;
    }

    @Bean
    public String cronBeanResponder() {
        return this.cronResponderValue;
    }
}
