package io.cloudbeat.common.wrapper.webdriver;

import io.cloudbeat.common.har.model.HarEntry;
import io.cloudbeat.common.har.model.HarLog;
import io.cloudbeat.common.model.HttpNetworkEntry;
import io.cloudbeat.common.reporter.model.LogMessage;

import java.util.List;
import java.util.Map;

public interface AbstractWebDriver {
    public Map<String, Object> getCapabilities();
    public boolean isChromeDriverInstance();
    public boolean hasDevTools();
    public boolean isPerformanceLoggingOn();
    public void addNavigationTimingStats(Map<String, Number> stats);
    public void addDevToolsPerformanceStats(Map<String, Number> stats);
    public void addBrowserLogs(final List<LogMessage> logs);
    public void addLogcatLogs(final List<LogMessage> logs);
    public void openDevToolsSession();
    public void closeDevToolsSession();
    public void enableDevToolsPerformance();
    public void disableDevToolsPerformance();
    public void enableDevToolsConsoleLogs();
    public void enableDevToolsNetworkCapturing();
    public void disableDevToolsNetworkCapturing();
    public HarLog getHarLog();
    public String getScreenshot();
}
