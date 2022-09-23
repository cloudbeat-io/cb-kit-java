package io.cloudbeat.common.config;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;

public final class CbConfigLoader {
    public static CbConfig load() throws IOException {
        // if payloadpath property is specified, it means the test is executed by CB Runner
        if (StringUtils.isNotEmpty(System.getProperty("payloadpath"))) {
            String jsonConfigFilePath = System.getProperty("payloadpath");
            return JsonConfigLoader.load(jsonConfigFilePath);
        }
        else {
            URL resource = ClassLoader.getSystemClassLoader().getResource(PropertiesConfigLoader.CB_PROPERTIES_FILE);
            // if cloudbeat.properties file not found, return null
            if (resource == null)
                return null;
            return PropertiesConfigLoader.load();
        }
    }
}
