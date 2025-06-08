package com.betterbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);    // minimum number of threads to keep alive
        executor.setMaxPoolSize(10);    // maximum number of threads that can be created
        executor.setQueueCapacity(50);  // queue capacity for tasks
        executor.setThreadNamePrefix("Auth-Service-Async-Keycloak-Tasks");
        executor.initialize();

        return executor;
    }
}
