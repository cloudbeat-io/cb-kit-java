package io.cloudbeat.common.config;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class CbConfig {
    public static final String DEFAULT_WEBDRIVER_URL = "http://localhost:4444/wd/hub";
    public static final String DEFAULT_API_URL = "https://api.cloudbeat.io";
    public static final String CB_API_KEY = "CB_API_KEY";
    public static final String CB_API_URL = "CB_API_URL";
    public static final String CB_PROJECT_ID = "CB_PROJECT_ID";
    public static final String CB_RUN_ID = "CB_RUN_ID";
    public static final String CB_RUN_GROUP = "CB_RUN_GROUP";
    public static final String CB_INSTANCE_ID = "CB_INSTANCE_ID";
    public static final String CB_CAPS_PREFIX = "CB_CAPS.";
    public static final String CB_META_PREFIX = "CB_META.";
    public static final String CB_ENV_PREFIX = "CB_ENV.";
    public static final String CB_OPT_PREFIX = "CB_OPT.";
    public static final String CB_SELENIUM_URL = "CB_SELENIUM_URL";
    public static final String CB_APPIUM_URL = "CB_APPIUM_URL";
    public static final String CB_GATEWAY_TOKEN_PROP_NAME = "testmonitortoken";
    public static final String CB_GATEWAY_URL_PROP_NAME = "testmonitorurl";
    public static final String CB_GATEWAY_TOKEN = "CB_GATEWAY_TOKEN";
    public static final String CB_GATEWAY_URL = "CB_GATEWAY_URL";

    final Properties props;
    String runId;
    String runGroup;
    String instanceId;
    String projectId;
    String gatewayToken;
    String gatewayUrl;
    String apiToken;
    String apiEndpointUrl;
    String seleniumUrl;
    String appiumUrl;
    Map<String, String> metadata;
    Map<String, Object> capabilities;
    Map<String, String> envVars;
    Map<String, String> options;
    List<String> tags;
    List<CaseDef> cases;

    public CbConfig() {
        this.props = null;
        loadGatewaySettingsFromProps();
    }

    public CbConfig(
            String runId,
            String instanceId,
            String projectId,
            String apiEndpointUrl,
            String apiToken,
            String gatewayUrl,
            String gatewayToken,
            String seleniumUrl,
            String appiumUrl,
            Map<String, String> metadata,
            Map<String, String> capabilities,
            Map<String, String> envVars,
            Map<String, String> options
    ) {
        this.props = null;
    }
    public CbConfig(Properties props) {
        this.props = props;
        loadConfigFromProps();
    }

    private void loadGatewaySettingsFromProps() {
        if (StringUtils.isNotEmpty(System.getProperty(CB_GATEWAY_TOKEN_PROP_NAME)))
            gatewayToken = System.getProperty(CB_GATEWAY_TOKEN_PROP_NAME);
        if (StringUtils.isNotEmpty(System.getProperty(CB_GATEWAY_URL_PROP_NAME)))
            gatewayUrl = System.getProperty(CB_GATEWAY_URL_PROP_NAME);
    }
    private void loadConfigFromProps() {
        apiToken = getProperty(CB_API_KEY);
        apiEndpointUrl = getProperty(CB_API_URL, DEFAULT_API_URL);
        projectId = getProperty(CB_PROJECT_ID);
        runId = getProperty(CB_RUN_ID);
        runGroup = getProperty(CB_RUN_GROUP);
        instanceId = getProperty(CB_INSTANCE_ID);
        seleniumUrl = getProperty(CB_SELENIUM_URL);
        appiumUrl = getProperty(CB_APPIUM_URL);
        // load capabilities
        loadMapFromPrefixedCaps(CB_CAPS_PREFIX, capabilities);
        // load metadata
        loadMapFromPrefixedProps(CB_META_PREFIX, metadata);
        // load options
        loadMapFromPrefixedProps(CB_OPT_PREFIX, options);
        // load environment variables
        loadMapFromPrefixedProps(CB_ENV_PREFIX, envVars);
    }

    private String getEnv(final String name, final String defaultValue) {
        final String possibleValFromEnv = System.getenv(name);
        if (StringUtils.isNotEmpty(possibleValFromEnv))
            return possibleValFromEnv;
        return defaultValue;
    }
    private String getProperty(final String key, final String defaultValue) {
        final String propertyName = getPropertyNameFromConfigKey(key);
        if (props.containsKey(propertyName))
            return props.getProperty(propertyName);
        return getEnv(key, defaultValue);
    }

    private String getProperty(final String key) {
        return getProperty(key, null);
    }

    private void loadMapFromPrefixedProps(String prefix, Map<String, String> map) {
        final Set<String> propertyNames = props.stringPropertyNames();
        propertyNames.stream()
                .filter(name -> name.startsWith(prefix))
                .forEach(name -> {
                    final String noPrefixPropName = name.substring(prefix.length());
                    final String propVal = props.getProperty(name);
                    this.capabilities.put(noPrefixPropName, propVal);
                });
    }
    private String getPropertyNameFromConfigKey(final String key) {
        return key.replace('_', '.').toLowerCase();
    }
    private void loadMapFromPrefixedCaps(String prefix, Map<String, Object> map) {
        final Set<String> propertyNames = props.stringPropertyNames();
        propertyNames.stream()
            .filter(name -> name.startsWith(prefix))
            .forEach(name -> {
                final String noPrefixPropName = name.substring(prefix.length());
                final String propVal = props.getProperty(name);
                this.capabilities.put(noPrefixPropName, propVal);
            });
    }

    public Properties getProperties() {
        return props;
    }

    public Map<String, Object> getCapabilities() { return capabilities; }

    public Map<String, String> getMetadata() { return metadata; }

    public Map<String, String> getOptions() { return options; }

    public Map<String, String> getEnvironmentVariables() { return envVars; }

    public String getGatewayToken() {
        if (gatewayToken == null)
            return getEnv(CB_GATEWAY_TOKEN, null);
        return gatewayToken;
    }
    public String getGatewayUrl() {
        if (gatewayUrl == null)
            return getEnv(CB_GATEWAY_URL, null);
        return gatewayUrl;
    }
    public String getApiToken() {
        if (apiToken == null)
            return getEnv(CB_API_KEY, null);
        return apiToken;
    }

    public String getApiEndpointUrl() {
        if (apiEndpointUrl == null)
            return getEnv(CB_API_URL, DEFAULT_API_URL);
        return apiEndpointUrl;
    }

    public String getProjectId() { return projectId; }

    public String getInstanceId() { return instanceId; }

    public String getRunId() { return runId; }
    public String getRunGroup() { return runGroup; }

    public boolean isRunningInCb() {
        return runId != null && instanceId != null;
    }

    public String getSeleniumUrl() { return seleniumUrl; }
    public String getAppiumUrl() { return seleniumUrl; }

    public String getSeleniumOrAppiumUrl() { return seleniumUrl != null && seleniumUrl.length() > 0 ? seleniumUrl : appiumUrl; }

    public List<CaseDef> getCases() {
        return cases;
    }

    public static class CaseDef {
        Optional<Long> id;
        String name;
        String fqn;
        short scriptType;
        String parameterData;
        short parameterMode;
        int iterationCount;
        short iterationMode;
        Map<String, Object> details = new HashMap<>();

        public Optional<Long> getId() {
            return id;
        }

        public void setId(Optional<Long> id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFqn() {
            return fqn;
        }

        public void setFqn(String fqn) {
            this.fqn = fqn;
        }

        public String getParameterData() {
            return parameterData;
        }

        public void setParameterData(String parameterData) {
            this.parameterData = parameterData;
        }

        public short getParameterMode() {
            return parameterMode;
        }

        public void setParameterMode(short parameterMode) {
            this.parameterMode = parameterMode;
        }

        public int getIterationCount() {
            return iterationCount;
        }

        public void setIterationCount(int iterationCount) {
            this.iterationCount = iterationCount;
        }

        public short getIterationMode() {
            return iterationMode;
        }

        public void setIterationMode(short iterationMode) {
            this.iterationMode = iterationMode;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }

        public short getScriptType() {
            return scriptType;
        }

        public void setScriptType(short scriptType) {
            this.scriptType = scriptType;
        }
    }
}
