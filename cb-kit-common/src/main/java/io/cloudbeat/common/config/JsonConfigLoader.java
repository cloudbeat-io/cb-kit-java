package io.cloudbeat.common.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JsonConfigLoader {
    private static final String SELENIUM_URL_KEY = "seleniumUrl";
    private static final String APPIUM_URL_KEY = "appiumUrl";
    final static TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<Map<String, Object>>() {};
    final static TypeReference<Map<String, String>> mapStringTypeRef = new TypeReference<Map<String, String>>() {};
    final static TypeReference<List<String>> listStringTypeRef = new TypeReference<List<String>>() {};
    public static CbConfig load(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String json = new String(encoded, StandardCharsets.UTF_8);

        CbConfig config = new CbConfig();
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(json, JsonNode.class);
        config.runId = rootNode.get("RunId").textValue();
        config.instanceId = rootNode.get("InstanceId").textValue();
        config.instanceId = rootNode.get("InstanceId").textValue();
        config.capabilities = mapper.readValue(rootNode.get("Capabilities").toString(), mapTypeRef);
        mapFoldedCapabilities(config.capabilities, mapper);
        config.metadata = mapper.readValue(rootNode.get("Metadata").toString(), mapTypeRef);
        config.envVars = mapper.readValue(rootNode.get("EnvironmentVariables").toString(), mapTypeRef);
        config.options = mapper.readValue(rootNode.get("Options").toString(), mapStringTypeRef);
        config.tags = mapper.readValue(rootNode.get("Tags").toString(), listStringTypeRef);
        // if cases list is specified, map it to a simple list of cases FQNs
        if (rootNode.has("Cases")) {
            config.cases = new ArrayList<>();
            for (JsonNode caseNode : rootNode.get("Cases")) {
                if (caseNode.get("Fqn") != null)
                    config.cases.add(caseNode.get("Fqn").asText());
                else if (caseNode.get("Details") != null) {
                    if (caseNode.get("Details").get("FullyQualifiedName") != null)
                        config.cases.add(caseNode.get("Details").get("FullyQualifiedName").asText());
                }
            }
        }
        // set seleniumUrl, if specified
        if (config.metadata != null && config.metadata.containsKey(SELENIUM_URL_KEY))
            config.seleniumUrl = config.metadata.get(SELENIUM_URL_KEY);
        // set appiumUrl, if specified
        if (config.metadata != null && config.metadata.containsKey(APPIUM_URL_KEY))
            config.appiumUrl = config.metadata.get(APPIUM_URL_KEY);

        return config;
    }

    private static void mapFoldedCapabilities(Map<String, Object> caps, ObjectMapper mapper) {
        caps.forEach((key, value) -> {
            if (key.endsWith(":options") && value != null && value instanceof String && value.toString().startsWith("{")) {
                try {
                    Map<String, Object> options = mapper.readValue(value.toString(), mapTypeRef);
                    caps.replace(key, options);
                }
                catch (Exception e) {

                }
            }
        });
    }
}
