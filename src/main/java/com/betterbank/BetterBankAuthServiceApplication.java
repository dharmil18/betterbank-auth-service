package com.betterbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BetterBankAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BetterBankAuthServiceApplication.class, args);
    }

}
