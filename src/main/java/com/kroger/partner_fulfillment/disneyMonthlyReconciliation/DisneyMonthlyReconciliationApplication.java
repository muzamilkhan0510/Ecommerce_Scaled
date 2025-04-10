package com.kroger.partner_fulfillment.disneyMonthlyReconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DisneyMonthlyReconciliationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DisneyMonthlyReconciliationApplication.class, args);
	}

}
