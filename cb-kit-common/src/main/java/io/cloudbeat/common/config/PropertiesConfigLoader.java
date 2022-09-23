package io.cloudbeat.common.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertiesConfigLoader {
    public static final String CB_PROPERTIES_FILE = "cloudbeat.properties";

    @NotNull
    public static CbConfig load() {
        final Properties properties = new Properties();
        loadPropertiesFrom(ClassLoader.getSystemClassLoader(), properties);
        loadPropertiesFrom(Thread.currentThread().getContextClassLoader(), properties);
        properties.putAll(System.getProperties());
        return new CbConfig(properties);
    }

    private static void loadPropertiesFrom(final ClassLoader classLoader, final Properties properties) {
        try (InputStream stream = classLoader.getResourceAsStream(CB_PROPERTIES_FILE)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            //LOGGER.error("Error while reading allure.properties file from classpath: {}", e.getMessage());
        }
    }
}
