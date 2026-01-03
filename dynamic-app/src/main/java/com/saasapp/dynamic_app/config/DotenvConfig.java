package com.saasapp.dynamic_app.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Load environment variables from .env file
 * This class uses both static initialization (for early loading) and
 * ApplicationContextInitializer (for adding to Spring environment)
 * This ensures .env variables are available as soon as possible
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    // Static block runs first, before anything else
    static {
        loadDotenv();
    }

    private static void loadDotenv() {
        try {
            System.out.println("🔍 Attempting to load .env file...");
            System.out.println("📂 Current working directory: " + System.getProperty("user.dir"));

            Dotenv dotenv = Dotenv.configure()
                    .directory(".")  // Explicitly load from current directory
                    .filename(".env")  // Explicitly specify filename
                    .ignoreIfMissing()
                    .load();

            // Set all variables as system properties immediately
            System.out.println("\n========== LOADING .ENV VARIABLES ==========");
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                System.setProperty(key, value);

                // Mask sensitive values
                String displayValue = value;
                if (key.contains("PASSWORD") || key.contains("SECRET") || key.contains("KEY")) {
                    displayValue = "***" + (value.length() > 4 ? value.substring(value.length() - 4) : "") + "***";
                } else if (value.length() > 40) {
                    displayValue = value.substring(0, 40) + "...";
                }

                System.out.println("✓ " + key + " = " + displayValue);
            });

            int loadedCount = dotenv.entries().size();
            System.out.println("==========================================");

            if (loadedCount > 0) {
                System.out.println("✓ Successfully loaded " + loadedCount + " environment variables from .env\n");
            } else {
                System.err.println("⚠️ .env file found but no variables loaded - check file format\n");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not load .env file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();

        // Load .env file again to add to Spring's property sources
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Convert dotenv entries to a property source with highest priority
        Map<String, Object> props = new HashMap<>();
        dotenv.entries().forEach(entry -> {
            props.put(entry.getKey(), entry.getValue());
        });

        // Add to environment with highest priority (index 0)
        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(
                new MapPropertySource("dotenv", props)
            );
            System.out.println("✓ .env variables added to Spring environment");
        }
    }
}


