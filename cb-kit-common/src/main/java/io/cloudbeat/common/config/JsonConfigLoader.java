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
import java.util.Optional;

public final class JsonConfigLoader {
    private static final String SELENIUM_URL_KEY = "seleniumUrl";
    private static final String SELENIUM_URL_PROPERTY_KEY = "CB_SELENIUM_URL";
    private static final String APPIUM_URL_KEY = "appiumUrl";
    private static final String APPIUM_URL_PROPERTY_KEY = "CB_APPIUM_URL";
    final static TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<Map<String, Object>>() {};
    final static TypeReference<Map<String, String>> mapStringTypeRef = new TypeReference<Map<String, String>>() {};
    final static TypeReference<List<String>> listStringTypeRef = new TypeReference<List<String>>() {};
    public static CbConfig load(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String json = new String(encoded, StandardCharsets.UTF_8);

        CbConfig config = new CbConfig();
        final ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readValue(json, JsonNode.class);
            config.runId = rootNode.get("RunId").textValue();
            config.instanceId = rootNode.get("InstanceId").textValue();
            config.instanceId = rootNode.get("InstanceId").textValue();
            config.capabilities = mapper.readValue(rootNode.get("Capabilities").toString(), mapTypeRef);
            mapFoldedCapabilities(config.capabilities, mapper);
            // remove technologyName capability, if presented (a left-over from legacy CB)
            if (config.capabilities != null && config.capabilities.containsKey("technologyName"))
                config.capabilities.remove("technologyName");
            // make sure browserName capability's value is in lower-case
            if (config.capabilities != null && config.capabilities.containsKey("browserName")
                    && config.capabilities.get("browserName") != null)
                config.capabilities.put("browserName", ((String)config.capabilities.get("browserName")).toLowerCase());
            config.metadata = mapper.readValue(rootNode.get("Metadata").toString(), mapStringTypeRef);
            config.envVars = mapper.readValue(rootNode.get("EnvironmentVariables").toString(), mapStringTypeRef);
            config.options = mapper.readValue(rootNode.get("Options").toString(), mapStringTypeRef);
            config.tags = mapper.readValue(rootNode.get("Tags").toString(), listStringTypeRef);
            // if cases list is specified, map it to a simple list of cases FQNs
            if (rootNode.has("Cases")) {
                config.cases = new ArrayList<>();
                for (JsonNode caseNode : rootNode.get("Cases")) {
                    CbConfig.CaseDef caseDef = new CbConfig.CaseDef();
                    config.cases.add(caseDef);
                    caseDef.setId(Optional.ofNullable(
                        caseNode.get("Id") != null ? caseNode.get("Id").asLong() : null)
                    );
                    caseDef.setFqn(caseNode.get("Fqn") != null ? caseNode.get("Fqn").asText() : null);
                    caseDef.setName(caseNode.get("Name") != null ? caseNode.get("Name").asText() : null);
                    caseDef.setIterationCount(
                            caseNode.get("IterationCount") != null ?
                                    caseNode.get("IterationCount").asInt(0) : 0);
                    caseDef.setParameterData(
                            caseNode.get("ParameterData") != null ?
                                    caseNode.get("ParameterData").asText() : null);
                    caseDef.setScriptType(
                            (short) (caseNode.get("ScriptType") != null ?
                                    caseNode.get("ScriptType").asInt(0) : 0));

                    if (caseNode.get("Details") != null) {
                        if (caseNode.get("Details").get("FullyQualifiedName") != null)
                            caseDef.setFqn(caseNode.get("Details").get("FullyQualifiedName").asText());
                        caseNode.get("Details").fields().forEachRemaining(f -> {
                            if (f.getKey().equals("FullyQualifiedName"))
                                return;
                            if (f.getValue() != null && f.getValue().canConvertToInt())
                                caseDef.details.put(f.getKey(), f.getValue().asInt());
                            else if (f.getValue() != null && f.getValue().canConvertToLong())
                                caseDef.details.put(f.getKey(), f.getValue().asLong());
                            else if (f.getValue() != null && f.getValue().isBoolean())
                                caseDef.details.put(f.getKey(), f.getValue().asBoolean());
                            else if (f.getValue() != null && !f.getValue().isNull())
                                caseDef.details.put(f.getKey(), f.getValue().asText());
                        });
                    }
                }
            }
            // set seleniumUrl, if specified
            if (config.metadata != null && config.metadata.containsKey(SELENIUM_URL_KEY))
                config.seleniumUrl = config.metadata.get(SELENIUM_URL_KEY);
            // sometimes, selenium url might be passed via -D command line argument, e.g. appear as property
            else if (System.getProperties().containsKey(SELENIUM_URL_PROPERTY_KEY))
                config.seleniumUrl = System.getProperty(SELENIUM_URL_PROPERTY_KEY);
            // set appiumUrl, if specified
            if (config.metadata != null && config.metadata.containsKey(APPIUM_URL_KEY))
                config.appiumUrl = config.metadata.get(APPIUM_URL_KEY);
            // sometimes, appium url might be passed via -D command line argument, e.g. appear as property
            else if (System.getProperties().containsKey(APPIUM_URL_PROPERTY_KEY))
                config.seleniumUrl = System.getProperty(APPIUM_URL_PROPERTY_KEY);

            return config;
        }
        catch (Throwable e) {
            System.err.println("Cannot read CB configuration file: " + e.toString());
            return null;
        }
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
