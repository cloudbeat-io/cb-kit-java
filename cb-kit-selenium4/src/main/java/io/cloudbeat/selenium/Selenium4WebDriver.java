package io.cloudbeat.selenium;

import io.cloudbeat.common.reporter.model.LogMessage;
import io.cloudbeat.common.webdriver.AbstractWebDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v108.performance.Performance;
import org.openqa.selenium.devtools.v108.performance.model.Metric;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;

import java.io.Console;
import java.util.*;

public class Selenium4WebDriver implements AbstractWebDriver {
    private final WebDriver driver;
    private final DevTools devTools;
    public Selenium4WebDriver(WebDriver driver) {
        this.driver = driver;
        if (driver instanceof HasDevTools) {
            HasDevTools devtoolsDriver = (HasDevTools) driver;
            devTools = devtoolsDriver.getDevTools();
        }
        else
            devTools = null;
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
    public void openDevToolsSession() {
        if (driver instanceof HasDevTools) {
            HasDevTools devtoolsDriver = (HasDevTools) driver;
            try (DevTools devTools = devtoolsDriver.getDevTools()) {
                devTools.createSessionIfThereIsNotOne();
            }
            catch (Throwable ignore) { }
        }
    }
    public void closeDevToolsSession() {

    }
    public void enableDevToolsPerformance() {
        if (devTools != null) {
            try {
                devTools.createSessionIfThereIsNotOne();
                devTools.send(Performance.enable(Optional.empty()));
            }
            catch (Throwable e) {
                System.err.println("enableDevToolsPerformance: " + e.toString());
            }
        }
    }
    public void disableDevToolsPerformance() {
        if (devTools != null) {
            try {
                devTools.createSessionIfThereIsNotOne();
                devTools.send(Performance.disable());
            } catch (Throwable ignore) { }
        }
    }
    public void addDevToolsPerformanceStats(Map<String, Number> stats) {
        if (devTools != null) {
            try {
                devTools.createSessionIfThereIsNotOne();
                List<Metric> metrics = devTools.send(Performance.getMetrics());
                // process Core Web Vitals metrics
                //metrics.stream().forEach(m -> System.out.println(m.getName()));
            }
            catch (Throwable e) {
                System.err.println("addDevToolsStats: " + e.toString());
            }
        }
    }
    @Override
    public void addNavigationTimingStats(Map<String, Number> stats) {
        // retrieve navigation timings from the browser
        ArrayList<Object> navigationEntriesResult = (ArrayList<Object>) ((JavascriptExecutor)driver).executeScript(
                "return window.performance.getEntriesByType('navigation');");
        if (navigationEntriesResult == null || navigationEntriesResult.isEmpty())
            return;
        // retrieve resources list from the browser and calculate total resources size and count
        ArrayList<Object> resourceEntriesResult = (ArrayList<Object>) ((JavascriptExecutor)driver).executeScript(
                "var entries = window.performance.getEntriesByType(\"resource\"); var totalSize = window.performance.getEntriesByType(\"resource\").reduce((accumulator, r) => accumulator + r.transferSize, 0); return [totalSize, entries.length];");

        Optional<Long> resourceCount = resourceEntriesResult.size() == 2 ? Optional.of((Long)resourceEntriesResult.get(1)) : null;
        Optional<Long> transferSize = resourceEntriesResult.size() == 2 ? Optional.of((Long)resourceEntriesResult.get(0)) : null;

        Map<String, Object> entries = (Map<String, Object>)navigationEntriesResult.get(0);
        stats.put("domContentLoadedEvent", ((Double)entries.get("domContentLoadedEventEnd")).longValue());
        stats.put("domInteractive", ((Double)entries.get("domInteractive")).longValue());
        stats.put("loadEvent", ((Double)entries.get("loadEventEnd")).longValue());
        stats.put("domComplete", ((Double)entries.get("domComplete")).longValue());
        if (resourceCount.isPresent())
            stats.put("requests", resourceCount.get());
        if (transferSize.isPresent())
            stats.put("transferSize", transferSize.get());
    }

    @Override
    public void addBrowserLogs(List<LogMessage> logs) {
        if (logs == null) return;
        LogEntries browserLogs = driver.manage().logs().get(LogType.BROWSER);
        if (browserLogs == null)
            return;
        browserLogs.forEach(logEntry -> {
            LogMessage cbLogEntry = new LogMessage();
            cbLogEntry.setLevel(logEntry.getLevel().toString());
            cbLogEntry.setMessage(logEntry.getMessage());
            cbLogEntry.setTimestamp(logEntry.getTimestamp());
            logs.add(cbLogEntry);
        });
    }

    @Override
    public void addLogcatLogs(List<LogMessage> logs) {
        if (logs == null) return;
        LogEntries logcatLogs = driver.manage().logs().get("logcat");
        if (logcatLogs == null)
            return;
        logcatLogs.forEach(logEntry -> {
            LogMessage cbLogEntry = new LogMessage();
            cbLogEntry.setLevel(logEntry.getLevel().toString());
            cbLogEntry.setMessage(logEntry.getMessage());
            cbLogEntry.setTimestamp(logEntry.getTimestamp());
            logs.add(cbLogEntry);
        });
    }

    @Override
    public String getScreenshot() {
        try {
            String screenshotBase64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            return screenshotBase64;
        }
        catch (Exception e) {
            System.err.println("Selenium4WebDriver - failed to take a screenshot: " + e.toString());
        }
        return  null;
    }

}
