package io.cloudbeat.selenium;

import io.cloudbeat.common.reporter.model.LogMessage;
import io.cloudbeat.common.webdriver.AbstractWebDriver;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.Map;

public class Selenium4WebDriver implements AbstractWebDriver {
    private final WebDriver driver;
    public Selenium4WebDriver(WebDriver driver) {
        this.driver = driver;
    }
    @Override
    public Map<String, Object> getCapabilities() {
        if (driver != null && driver instanceof HasCapabilities) {
            Capabilities caps = ((HasCapabilities)driver).getCapabilities();
            return caps.asMap();
        }
        return null;
    }

    @Override
    public boolean isChromeDriverInstance() {
        return driver != null && driver instanceof ChromeDriver;
    }

    @Override
    public boolean isPerformanceLoggingOn() {
        return false;
    }

    @Override
    public Map<String, Number> getNavigationTimingStats() {
        return null;
    }

    @Override
    public void addBrowserLogs(List<LogMessage> logs) {

    }

    @Override
    public void addLogcatLogs(List<LogMessage> logs) {

    }

    @Override
    public String getScreenshot() {
        return null;
    }

}
