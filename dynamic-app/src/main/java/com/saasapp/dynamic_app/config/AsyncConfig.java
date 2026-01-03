package com.saasapp.dynamic_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async Configuration
 * Enables asynchronous method execution using @Async annotation
 * Used for non-blocking operations like email sending
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Configuration class for async support
}

