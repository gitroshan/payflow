package com.payflow.payout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PayoutReconciliationApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayoutReconciliationApplication.class, args);
    }
}
