package io.cloudbeat.common.wrapper.webdriver;

import io.cloudbeat.common.har.model.HarEntry;
import io.cloudbeat.common.har.model.HarLog;
import io.cloudbeat.common.model.HttpNetworkEntry;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.LogMessage;
import io.cloudbeat.common.reporter.model.StepResult;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
//import java.util.stream.Stream;

public class WebDriverEventHandler {
    public final static String CAP_BROWSER_NAME = "browserName";
    public final static String CAP_PLATFORM_NAME = "platformName";
    public final static String CAP_CHROME_LOGGING_PREFS = "goog:loggingPrefs";
    private final CbTestReporter reporter;
    private final AbstractWebDriver wrapper;
    private String lastStepId = null;
    private Map<String, Object> caps;
    private final boolean isWeb;
    private final boolean isMobile;
    private final boolean isAndroid;
    private final boolean isChrome;
    private final boolean isPerformanceLoggingOn;

    public WebDriverEventHandler(CbTestReporter reporter, AbstractWebDriver wrapper)
    {
        this.reporter = reporter;
        this.wrapper = wrapper;
        // identify if the provided webdriver is of mobile or web type
        this.caps = wrapper.getCapabilities();
        final String browserName = getBrowserName();
        final String platformName = getPlatformName();
        isWeb = StringUtils.isNotEmpty(browserName);
        isMobile = platformName != null && (
                platformName.equalsIgnoreCase("android")
                || platformName.equalsIgnoreCase("ios")
        );
        isAndroid = platformName != null && platformName.equalsIgnoreCase("android");
        isChrome = isChrome(browserName);
        isPerformanceLoggingOn = isPerformanceLoggingOn();
        if (wrapper.hasDevTools())
            wrapper.enableDevToolsConsoleLogs();
    }

    private String getBrowserName() {
        if (caps != null && caps.containsKey(CAP_BROWSER_NAME))
            return Objects.toString(caps.get(CAP_BROWSER_NAME), null);
        return null;
    }

    private String getPlatformName() {
        if (caps != null && caps.containsKey(CAP_PLATFORM_NAME))
            return Objects.toString(caps.get(CAP_PLATFORM_NAME), null);
        return null;
    }

    private boolean isChrome(final String browserName) {
        if (wrapper.isChromeDriverInstance())
            return true;

        return StringUtils.isNotEmpty(browserName) && browserName.equalsIgnoreCase("chrome");
    }

    private boolean isPerformanceLoggingOn() {
        return caps != null && caps.containsKey(CAP_CHROME_LOGGING_PREFS)
                && caps.get(CAP_CHROME_LOGGING_PREFS) != null;
    }
    
    public void beforeAlertAccept() {
        final StepResult step = reporter.startStep("Accept alert");
        lastStepId = step != null ? step.getId() : null;
    }
    
    public void afterAlertAccept() {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeAlertDismiss() {
        final StepResult step = reporter.startStep("Dismiss alert");
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterAlertDismiss() {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeNavigateTo(final String url) {
        try {
            wrapper.enableDevToolsPerformance();
        }
        catch (Throwable ignore) {}
        /*try {
            wrapper.enableDevToolsNetworkCapturing();
        }
        catch (Throwable ignore) {}*/
        final StepResult step = reporter.startStep("Navigate to " + url);
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterNavigateTo(final String url) {
        if (lastStepId == null)      // not suppose to happen
            return;

        String stepName = "Navigate to " + url;

        // get navigation timing metrics
        Map<String, Number> stats = new HashMap<>();
        try {
            wrapper.addNavigationTimingStats(stats);
            wrapper.addDevToolsPerformanceStats(stats);
            wrapper.disableDevToolsPerformance();
        }
        catch (Throwable ignore) {}

        /*try {
            wrapper.disableDevToolsNetworkCapturing();
        }
        catch (Throwable ignore) {}*/

        // get browser or device logs
        final List<LogMessage> logs = collectLogs();

        final HarLog harLog = collectNetworkLogs();

        //collectPerformanceData(webDriver);

        StepResult stepResult = reporter.passStep(lastStepId, stats, logs.size() == 0 ? null : logs);
        stepResult.addHarAttachment(harLog);

        lastStepId = null;
    }

    private HarLog collectNetworkLogs() {
        try {
            if (isWeb)
                return wrapper.getHarLog();
        }
        catch (Exception ignore) {}
        return null;
    }

    private void collectPerformanceData() {
        //if (isChrome && isPerformanceLoggingOn)

        /*ArrayList<Object> results = (ArrayList<Object>) ((JavascriptExecutor)webDriver).executeScript(
                //"return window.performance.getEntries();");
                "return await chrome.devtools.network.getHAR();");
        results.forEach((url)->System.out.println(url.toString()));*/

        /*boolean hasPerformanceLogs = webDriver.manage().logs().getAvailableLogTypes()
                .stream().anyMatch(t -> t.equals("performance"));
        if (!hasPerformanceLogs)
            return;
        LogEntries perfLogs = webDriver.manage().logs().get("performance");*/
    }

    
    public void beforeNavigateBack() {
        final StepResult step = reporter.startStep("Navigate back");
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterNavigateBack() {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeNavigateForward() {
        final StepResult step = reporter.startStep("Navigate forward");
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterNavigateForward() {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeNavigateRefresh() {
        final StepResult step = reporter.startStep("Navigate refresh");
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterNavigateRefresh() {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeFindElement(final AbstractLocator locator) {
        final String locatorDisplayName = this.getLocatorDisplayName(locator);
        final StepResult step = reporter.startStep(String.format("Find element %s",  locatorDisplayName));
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterFindElement(final AbstractLocator locator, final AbstractWebElement elm) {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    public void beforeFindElements(final AbstractLocator locator) {
        final String locatorDisplayName = this.getLocatorDisplayName(locator);
        final StepResult step = reporter.startStep(String.format("Find elements %s",  locatorDisplayName));
        lastStepId = step != null ? step.getId() : null;
    }

    public void afterFindElements(final AbstractLocator locator, final List<AbstractWebElement> elements) {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeClickOn(final AbstractWebElement elm) {
        final String elmName = this.getElementDisplayName(elm);
        final StepResult step = reporter.startStep(String.format("Click on %s", elmName));
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterClickOn(final AbstractWebElement elm) {
        // get navigation timing metrics
        final Map<String, Number> stats = new HashMap<>();

        try {
            wrapper.addNavigationTimingStats(stats);
            wrapper.addDevToolsPerformanceStats(stats);
            wrapper.disableDevToolsPerformance();
        }
        catch (Throwable ignore) {}
        // get browser or device logs
        final List<LogMessage> logs = collectLogs();

        reporter.passStep(lastStepId, stats, logs);
        lastStepId = null;
    }

    
    public void beforeChangeValueOf(final AbstractWebElement elm,
            final CharSequence[] keysToSend) {
        final StringBuilder sb = new StringBuilder();
        for (final CharSequence charSequence : keysToSend) {
            sb.append(charSequence.toString());
        }
        final StepResult step = reporter.startStep(String.format("Set value \"%s\"", sb.toString()));
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterChangeValueOf(final AbstractWebElement elm,
            final CharSequence[] keysToSend) {
        final StringBuilder sb = new StringBuilder();
        for (final CharSequence charSequence : keysToSend) {
            sb.append(charSequence.toString());
        }
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeScript(final String script) {
        final StepResult step = reporter.startStep("Executing script: " + script);
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterScript(final String script) {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void beforeSwitchToWindow(final String windowName) {
        final StepResult step = reporter.startStep("Switch to window " + windowName);
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterSwitchToWindow(final String windowName) {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    
    public void onException(final Throwable throwable) {
        try {
            // try to take a screenshot
            String screenshot = wrapper.getScreenshot();
            if (lastStepId != null)
                reporter.failStep(lastStepId, throwable, screenshot);
            else
                reporter.setScreenshotOnException(screenshot);
        }
        catch (Throwable e) {}
    }
    
    public void beforeGetText(final AbstractWebElement elm) {
        final StepResult step = reporter.startStep("Getting text of  " + elm.getText());
        lastStepId = step != null ? step.getId() : null;
    }

    
    public void afterGetText(final AbstractWebElement elm, final String text) {
        reporter.passStep(lastStepId);
        lastStepId = null;
    }

    private String getLocatorDisplayName(final AbstractLocator by) {
        if (by == null)
            return "";
        final String byLocatorStr = by.toString();
        return String.format("%s", byLocatorStr.replace("By.", "by "));
    }

    private String getElementDisplayName(final AbstractWebElement webElement) {
        if (webElement == null)
            return "element";
        final String text = webElement.getText();
        final String tagName = webElement.getTagName();
        final String elmType = webElement.getAttribute("type");
        String elmTypeLabel = "";
        // determine element type (link, button or other)
        if (tagName.equals("a")) {
            elmTypeLabel = "link ";
        }            
        else if (tagName.equals("button")) {
            elmTypeLabel = "button ";
        }
        else if (tagName.equals("option")) {
            elmTypeLabel = "option ";
        }
        else if (tagName.equals("label")) {
            elmTypeLabel = "label ";
        }
        else if (tagName.equals("input") && elmType != null && (elmType.equals("button") || elmType.equals("submit"))) {
            elmTypeLabel = "button ";
        }
        else if (tagName.equals("input") && elmType != null && elmType.equals("url")) {
            elmTypeLabel = "link ";
        }

        if (text != null && !text.isEmpty()) {
            return String.format("%s\"%s\"", elmTypeLabel, text);
        }
        else {
            return String.format("<%s>", tagName);
        }
    }

    private List<LogMessage> collectLogs() {
        ArrayList<LogMessage> logs = new ArrayList<>();

        try {
            if (isWeb)
                wrapper.addBrowserLogs(logs);
            if (isAndroid)
                wrapper.addLogcatLogs(logs);
        }
        catch (Exception ignore) {}

        return logs;
    }
}
