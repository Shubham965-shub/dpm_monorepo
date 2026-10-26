package com.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public final class ConfigReader {

    private static final Logger LOGGER =Logger.getLogger(ConfigReader.class.getName());
    private static final String CONFIG_FILE ="config.properties";
    private static final Properties PROPERTIES =loadProperties();
    private ConfigReader() {
        throw new UnsupportedOperationException("ConfigReader is a utility class");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = Thread.currentThread() .getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException( "Configuration file '" + CONFIG_FILE+ "' was not found on the classpath" );
            }
            properties.load(input);
            LOGGER.info("Configuration loaded successfully");
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException( "Failed to load " + CONFIG_FILE,exception);
        }
    }

    public static String get(String key) {
        Objects.requireNonNull(key, "Configuration key must not be null");
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String envKey = key.toUpperCase(Locale.ROOT) .replace('.', '_').replace('-', '_');
        String environmentValue = System.getenv(envKey);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }
        String fileValue = PROPERTIES.getProperty(key);
        if (fileValue == null || fileValue.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + key );
        }
        return fileValue.trim();
    }

    public static String get(String key, String defaultValue) {
        Objects.requireNonNull(key, "Configuration key must not be null");
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String envKey = key.toUpperCase(Locale.ROOT) .replace('.', '_') .replace('-', '_');
        String environmentValue = System.getenv(envKey);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }
        String fileValue = PROPERTIES.getProperty(key);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue.trim();
        }
        return Objects.requireNonNull( defaultValue,"Default value must not be null");
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, String.valueOf(defaultValue));

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Configuration '" + key  + "' must be a valid integer",exception);
        }
    }

    public static boolean getBoolean(String key,boolean defaultValue ) {
        String value = get(key, String.valueOf(defaultValue));
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException("Configuration '" + key  + "' must be true or false");
    }

    public static Duration getDuration(String key,Duration defaultValue) {
        String value = get(key, defaultValue.toString());
        try {
            Duration duration = Duration.parse(value);
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("Duration must be greater than zero");
            }
            return duration;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Configuration '" + key  + "' must be a valid ISO-8601 duration, " + "for example PT15S or PT1M",exception);
        }
    }
}