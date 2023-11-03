package io.cloudbeat.selenium;

import com.google.common.collect.ImmutableMap;
import io.cloudbeat.common.har.HarHelper;
import io.cloudbeat.common.har.model.HarEntry;
import io.cloudbeat.common.har.model.HarLog;
import io.cloudbeat.common.har.model.HarTiming;
import io.cloudbeat.common.har.model.HttpMethod;
import io.cloudbeat.common.reporter.model.LogMessage;
import io.cloudbeat.common.reporter.model.LogSource;
import io.cloudbeat.common.wrapper.webdriver.AbstractWebDriver;
import io.cloudbeat.selenium.har.PerformanceLogsToHar;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v117.network.Network;
import org.openqa.selenium.devtools.v117.network.model.RequestId;
import org.openqa.selenium.devtools.v117.network.model.ResourceTiming;
import org.openqa.selenium.devtools.v117.performance.Performance;
import org.openqa.selenium.devtools.v117.performance.model.Metric;
import org.openqa.selenium.devtools.v117.runtime.Runtime;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Selenium4WebDriver implements AbstractWebDriver {
    private final WebDriver driver;
    private final DevTools devTools;
    private final List<LogMessage> browserLogEntries = Collections.synchronizedList(new ArrayList<>());
    private final List<HarEntry> harEntries = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, HarEntry> harEntriesMap = Collections.synchronizedMap(new HashMap<>());
    private boolean isDevToolsConsoleLogsEnabled = false;
    private boolean isDevToolsNetworkEnabled = false;
    private final Lock consoleLogsLock = new ReentrantLock();
    private final Lock networkLogsLock = new ReentrantLock();
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
    public boolean hasDevTools() { return devTools != null; }
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
    @Override
    public void enableDevToolsConsoleLogs() {
        if (devTools != null) {
            try {
                devTools.createSessionIfThereIsNotOne();
                devTools.getDomains().events().addConsoleListener(consoleEvent -> {
                    if (consoleEvent.getMessages().size() == 0)
                        return;
                    List<Object> argList = DevToolsHelper.getArgumentListForConsoleEvent(consoleEvent);
                    consoleLogsLock.lock();
                    consoleEvent.getMessages().stream().forEach(msg -> {
                        LogMessage cbLogEntry = new LogMessage();
                        cbLogEntry.setLevel(DevToolsHelper.getLogLevelForConsoleEvent(consoleEvent));
                        cbLogEntry.setMessage(msg);
                        cbLogEntry.setTime(consoleEvent.getTimestamp().toEpochMilli());
                        cbLogEntry.setArgs(argList);
                        cbLogEntry.setSrc(LogSource.BROWSER);
                        browserLogEntries.add(cbLogEntry);
                    });
                    consoleLogsLock.unlock();
                });
                isDevToolsConsoleLogsEnabled = true;
            } catch (Throwable e) {
                isDevToolsConsoleLogsEnabled = false;
                System.err.println("enableDevToolsConsoleLogs: " + e.toString());
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
        // if we are collecting browser logs through DevTools
        if (isDevToolsConsoleLogsEnabled) {
            consoleLogsLock.lock();
            this.browserLogEntries.stream()
                    .forEach(logMessage -> logs.add(logMessage));
            this.browserLogEntries.clear();
            consoleLogsLock.unlock();
            return;
        }
        // or if DevTools are disabled or not supported, we will retrieve the logs from the webdriver
        LogEntries browserLogs = driver.manage().logs().get(LogType.BROWSER);
        if (browserLogs == null)
            return;
        browserLogs.forEach(logEntry -> {
            LogMessage cbLogEntry = new LogMessage();
            cbLogEntry.setLevel(DevToolsHelper.getLogLevelForJavaLevel(logEntry.getLevel()));
            cbLogEntry.setMessage(extractLogMessageText(logEntry.getMessage()));
            cbLogEntry.setSrc(LogSource.BROWSER);
            cbLogEntry.setTime(logEntry.getTimestamp());
            logs.add(cbLogEntry);
        });
    }

    private static String extractLogMessageText(String message) {
        if (message == null) return null;
        // If Chrome's console message includes "http..",
        // that means it includes reference to the JS file that generated the message.
        // We want to remove the JS reference and keep the text message only.
        if (!message.startsWith("http")) return message;
        Pattern pattern = Pattern.compile("\"(.*?)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) return message;
        return matcher.group(1);
    }

    @Override
    public void addLogcatLogs(List<LogMessage> logs) {
        if (logs == null) return;
        LogEntries logcatLogs = driver.manage().logs().get("logcat");
        if (logcatLogs == null)
            return;
        logcatLogs.forEach(logEntry -> {
            LogMessage cbLogEntry = new LogMessage();
            cbLogEntry.setLevel(DevToolsHelper.getLogLevelForJavaLevel(logEntry.getLevel()));
            cbLogEntry.setMessage(logEntry.getMessage());
            cbLogEntry.setSrc(LogSource.DEVICE);
            cbLogEntry.setTime(logEntry.getTimestamp());
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

    public void enableDevToolsNetworkCapturing() {
        if (devTools != null) {
            try {
                devTools.createSessionIfThereIsNotOne();
                devTools.send(Network.enable(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));
                devTools.addListener(Network.requestWillBeSent(),
                    cdpNetEntry -> {
                        try {
                            HarEntry harEntry = new HarEntry();
                            String reqId = cdpNetEntry.getRequestId().toString();
                            networkLogsLock.lock();
                            harEntriesMap.put(reqId, harEntry);
                            harEntries.add(harEntry);
                            // assign various request properties
                            long startTimeInMs = (long) (cdpNetEntry.getWallTime().toJson().doubleValue() * 1000);
                            harEntry.getRequest().setUrl(cdpNetEntry.getRequest().getUrl());
                            harEntry.getRequest().setMethod(HttpMethod.valueOf(cdpNetEntry.getRequest().getMethod().toUpperCase()));
                            harEntry.setStartedDateTime(new Date(startTimeInMs));
                            harEntry.setAdditionalField("startTimestamp", cdpNetEntry.getTimestamp().toJson());
                            if (cdpNetEntry.getRequest().getHasPostData().orElse(false) && cdpNetEntry.getRequest().getPostData().isPresent())
                                harEntry.getRequest().getPostData().setText(cdpNetEntry.getRequest().getPostData().orElse(null));
                            harEntry.getRequest().setQueryString(HarHelper.getHarQueryParamList(cdpNetEntry.getRequest().getUrl()));
                            harEntry.getRequest().setHeaders(HarHelper.getHarHeaderList(cdpNetEntry.getRequest().getHeaders().toJson()));
                            System.out.println("Request URI : " + cdpNetEntry.getRequest().getUrl() + "\n"
                                    + " With method : " + cdpNetEntry.getRequest().getMethod() + "\n");
                            networkLogsLock.unlock();
                        }
                        catch (Throwable e) {
                            System.err.println("Request URI : " + cdpNetEntry.getRequest().getUrl() + ", exception: " + e.getMessage());
                        }
                    });
                devTools.addListener(Network.loadingFinished(),
                    cdpEntry -> {
                        String reqId = cdpEntry.getRequestId().toString();
                        HarEntry harEntry = harEntriesMap.getOrDefault(reqId, null);
                        if (harEntry == null)
                            return;
                        harEntry.setAdditionalField("_loadingFinished", cdpEntry.getTimestamp().toJson());
                    });
                devTools.addListener(Network.responseReceivedExtraInfo(),
                    cdpEntry -> {
                    });
                devTools.addListener(Runtime.exceptionThrown(),
                    cdpEntry -> {
                        System.out.println("CDP exception: " + cdpEntry.getExceptionDetails().getText());
                    });
                devTools.addListener(Network.dataReceived(),
                    cdpEntry -> {
                        String reqId = cdpEntry.getRequestId().toString();
                        HarEntry harEntry = harEntriesMap.getOrDefault(reqId, null);
                        if (harEntry == null)
                            return;
                        double dataReceivedTs = cdpEntry.getTimestamp().toJson().doubleValue();
                        // calculate data "receive" time
                        if (harEntry.getAdditional().containsKey("_ts_responseReceived") &&
                                harEntry.getTimings().getReceive() != null) {
                            double responseReceivedTs = (double) harEntry.getAdditional().get("_ts_responseReceived");
                            double dataReceiveTime = (dataReceivedTs - responseReceivedTs) * 1000;
                            harEntry.getTimings().setReceive(harEntry.getTimings().getReceive() + dataReceiveTime);
                            harEntry.setTime(harEntry.getTime() + harEntry.getTimings().getReceive());
                        }
                        else
                            System.out.println("receive time is null");
                        harEntry.setAdditionalField("_ts_dataReceived", dataReceivedTs);
                        harEntry.getResponse().getContent().setSize(cdpEntry.getDataLength().longValue());
                        harEntry.getResponse().getContent().setCompression(cdpEntry.getEncodedDataLength().longValue());
                    });

                devTools.addListener(Network.responseReceived(),
                    cdpNetEntry -> {
                        try {
                            networkLogsLock.lock();
                            String reqId = cdpNetEntry.getRequestId().toString();
                            HarEntry harEntry = harEntriesMap.getOrDefault(reqId, null);
                            if (harEntry == null) {
                                networkLogsLock.unlock();
                                return;
                            }
                            // store responseReceived event timestamp for later "receive" time calculation
                            harEntry.setAdditionalField("_ts_responseReceived", cdpNetEntry.getTimestamp().toJson().doubleValue());
                            // set various HarResponse properties
                            harEntry.getResponse().setRedirectURL(cdpNetEntry.getResponse().getUrl());
                            harEntry.getResponse().setStatus(cdpNetEntry.getResponse().getStatus());
                            harEntry.getResponse().setStatusText(cdpNetEntry.getResponse().getStatusText());
                            harEntry.getResponse().setHeaders(HarHelper.getHarHeaderList(cdpNetEntry.getResponse().getHeaders().toJson()));
                            harEntry.getResponse().getContent().setMimeType(cdpNetEntry.getResponse().getMimeType());
                            harEntry.getResponse().setHttpVersion(cdpNetEntry.getResponse().getProtocol().orElse(null));
                            setHarTimingFromCdp(
                                    harEntry,
                                    cdpNetEntry.getResponse().getTiming());
                            harEntry.setServerIPAddress(
                                    cdpNetEntry.getResponse().getRemoteIPAddress().orElse(null));
                            networkLogsLock.unlock();
                            // Network.getResponseBody()
                        }
                        catch (Throwable e) {
                            System.err.println("Request URI : " + cdpNetEntry.getResponse().getUrl() + ", exception: " + e.getMessage());
                        }
                    });
            } catch (Throwable e) {
                System.err.println("enableDevToolsNetworkCapturing: " + e.toString());
            }
        }
    }

    private static HarTiming setHarTimingFromCdp(
            HarEntry harEntry,
            Optional<ResourceTiming> cdpTiming) {
        if (!cdpTiming.isPresent()) return null;
        try {
            double queueing = cdpTiming.get().getDnsStart().doubleValue();
            double blocked = cdpTiming.get().getDnsEnd().doubleValue()
                    - cdpTiming.get().getConnectStart().doubleValue();
            double connect = cdpTiming.get().getConnectEnd().doubleValue()
                    - cdpTiming.get().getConnectStart().doubleValue();
            double dns = cdpTiming.get().getDnsEnd().doubleValue()
                    - cdpTiming.get().getDnsStart().doubleValue();
            double send = cdpTiming.get().getSendEnd().doubleValue()
                    - cdpTiming.get().getSendStart().doubleValue();
            double ssl = cdpTiming.get().getSslEnd().doubleValue()
                    - cdpTiming.get().getSslStart().doubleValue();
            double wait = cdpTiming.get().getReceiveHeadersStart().doubleValue()
                    - cdpTiming.get().getSendEnd().doubleValue();
            // The "receive" time below includes only time of receiving response header
            // and does not include the time it takes to receive the entire response data
            double receive = cdpTiming.get().getReceiveHeadersEnd().doubleValue()
                    - cdpTiming.get().getReceiveHeadersStart().doubleValue();
            HarTiming harTiming = new HarTiming();
            harTiming.setBlocked(queueing);
            harTiming.setConnect(connect);
            harTiming.setDns(dns);
            harTiming.setSend(send);
            harTiming.setSsl(ssl);
            harTiming.setWait(wait);
            harTiming.setReceive(receive);
            harTiming.setAdditionalField("_blocked_queueing", blocked);
            harEntry.setTimings(harTiming);
            // set "time" which is a sum of all "timing" metrics
            harEntry.setTime(
                    queueing
                            + blocked
                            + connect
                            + dns
                            + send
                            + ssl
                            + wait
                            + receive
            );
            return harTiming;
        }
        catch (Throwable e) {

        }
        return null;

    }

    public void disableDevToolsNetworkCapturing() {
        if (devTools != null) {
            try {
                devTools.createSessionIfThereIsNotOne();
                devTools.send(Network.disable());
            } catch (Throwable e) {
                System.err.println("disableDevToolsNetworkCapturing: " + e.toString());
            }
        }
    }
    public HarLog getHarLog() {
        try {
            if (!driver.manage().logs().getAvailableLogTypes().contains("performance"))
                return  null;
            LogEntries perfLogs = driver.manage().logs().get("performance");
            if (perfLogs == null)
                return null;
            return PerformanceLogsToHar.parse(perfLogs);
        }
        catch (Throwable e) {
            System.out.println(e);
            return  null;
        }
        /*List<HarEntry> harEntriesCopy = new ArrayList<>();
        networkLogsLock.lock();
        harEntriesCopy.addAll(harEntries);
        harEntries.clear();
        harEntriesMap.clear();
        networkLogsLock.unlock();
        return harEntriesCopy;*/
    }
}
