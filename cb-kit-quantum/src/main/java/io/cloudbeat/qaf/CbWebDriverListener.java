package io.cloudbeat.qaf;

import com.qmetry.qaf.automation.ui.webdriver.CommandTracker;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;
import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.reporter.model.TestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CbWebDriverListener extends QAFWebDriverCommandAdapter {
    private final boolean isActive;
    private final CbTestReporter reporter;
    private StepResult currentStep = null;

    public CbWebDriverListener() {
        isActive = CbTestContext.getInstance().isActive()
                && CbTestContext.getInstance().getReporter() != null
                && CbTestContext.getInstance().getReporter().isStarted();
        reporter = CbTestContext.getInstance().getReporter();
    }

    @Override
    public void beforeCommand(QAFExtendedWebDriver qafExtendedWebDriver, CommandTracker commandTracker) {
        if (!isActive || ignoreCommand(commandTracker))
            return;
        String stepName = getStepName(commandTracker);
        currentStep = reporter.startStep(stepName);
    }

    @Override
    public void afterCommand(QAFExtendedWebDriver qafExtendedWebDriver, CommandTracker commandTracker) {
        if (!isActive || currentStep == null || ignoreCommand(commandTracker))
            return;
        reporter.endStep(currentStep, TestStatus.PASSED, null, null);
    }

    @Override
    public void onFailure(QAFExtendedWebDriver qafExtendedWebDriver, CommandTracker commandTracker) {
        if (!isActive || currentStep == null)
            return;
        String screenshot = getScreenshot(qafExtendedWebDriver, commandTracker.getException());
        reporter.endStep(currentStep, TestStatus.FAILED, commandTracker.getException(), screenshot);
    }

    private static String getScreenshot(QAFExtendedWebDriver qafExtendedWebDriver, RuntimeException exception) {
        return qafExtendedWebDriver.takeScreenShot();
    }

    @Override
    public void beforeInitialize(org.openqa.selenium.Capabilities capabilities) {

    }

    @Override
    public void onInitialize(QAFExtendedWebDriver qafExtendedWebDriver) {

    }

    @Override
    public void onInitializationFailure(org.openqa.selenium.Capabilities capabilities, Throwable throwable) {

    }

    private static boolean ignoreCommand(CommandTracker commandTracker) {
        String cmdName = commandTracker.getCommand();
        if (cmdName.equals("executeScript"))
            return  true;
        return  false;
    }

    private static String getStepName(CommandTracker commandTracker) {
        String cmdName = commandTracker.getCommand();
        Map<String, Object> cmdParams = commandTracker.getParameters();
        List<String> formattedParams = cmdParams.keySet().stream().map(key -> formatNamedParam(key, cmdParams)).collect(Collectors.toList());
        String cmdArgsAsString = String.join(",", formattedParams);
        return String.format("%s(%s)", cmdName, cmdArgsAsString);
    }

    private static String formatNamedParam(String key, Map<String, Object> cmdParams) {
        Object valAsObj = cmdParams.getOrDefault(key, "null");
        return String.format("%s: \"%s\"", key, valAsObj.toString());
    }
}
