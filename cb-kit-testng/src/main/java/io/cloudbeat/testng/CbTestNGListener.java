package io.cloudbeat.testng;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.StepResult;
import org.testng.*;
import org.testng.internal.IResultListener2;
import java.util.Map;

public class CbTestNGListener implements
        IExecutionListener,
        ISuiteListener,
        IResultListener2,
        IInvokedMethodListener {
    static boolean started = false;
    static CbTestContext ctx = CbTestContext.getInstance();

    public CbTestNGListener() {

    }

    private static CbTestReporter getReporter() {
        if (CbTestNGListener.ctx == null)
            return null;
        return CbTestNGListener.ctx.getReporter();
    }

    public static void step(final String name, Runnable stepFunc) {
        CbTestReporter reporter = getReporter();
        if (reporter == null)
            return;
        reporter.step(name, stepFunc);
    }

    public static String startStep(final String name) {
        CbTestReporter reporter = getReporter();
        if (reporter == null)
            return null;
        final StepResult step = reporter.startStep(name);
        return step != null ? step.getId() : null;
    }

    public static void endLastStep() {
        CbTestReporter reporter = getReporter();
        if (reporter == null)
            return;
        reporter.endLastStep();
    }

    public static <D> D wrapWebDriver(D driver) {
        return ctx.wrapWebDriver(driver);
    }

    public static String getEnv(String name) {
        if (ctx == null || ctx.getReporter() == null)
            return System.getenv(name);
        Map<String, String> cbEnvVars = ctx.getConfig().getEnvironmentVariables();
        if (cbEnvVars != null && cbEnvVars.containsKey(name))
            return cbEnvVars.get(name);
        return System.getenv(name);
    }

    public static String getWebDriverUrl() {
        CbConfig config = CbTestContext.getInstance().getConfig();

        if (config != null) {
            String remoteUrl =  config.getSeleniumOrAppiumUrl();
            return remoteUrl != null ? remoteUrl : CbConfig.DEFAULT_WEBDRIVER_URL;
        }

        return CbConfig.DEFAULT_WEBDRIVER_URL;
    }

    @Override
    public void onExecutionStart() {
        setup();
    }

    @Override
    public void onExecutionFinish() {
        try {
            shutdown();
        }
        catch (Throwable e) {
            System.err.println("Failed to shutdown CloudBeat listener: " + e.toString());
        }
    }

    @Override
    public void beforeConfiguration(ITestResult iTestResult) {

    }

    @Override
    public void onConfigurationSuccess(ITestResult iTestResult) {

    }

    @Override
    public void onConfigurationFailure(ITestResult iTestResult) {

    }

    @Override
    public void onConfigurationSkip(ITestResult iTestResult) {

    }

    @Override
    public void onTestStart(ITestResult testResult) {
        if (!ctx.isActive())
            return;
        try {
            ctx.setCurrentTestClass(testResult.getTestClass().getRealClass());
            TestNGReporterHelper.startTestMethod(ctx.getReporter(), testResult);
        }
        catch (Exception e) {
            System.err.println("Error in onTestStart: " + e.toString());
        }
    }

    @Override
    public void onTestSuccess(ITestResult testResult) {
        if (!ctx.isActive())
            return;
        try {
            TestNGReporterHelper.endTestMethod(ctx.getReporter(), testResult);
            ctx.setCurrentTestClass(null);
        }
        catch (Exception e) {
            System.err.println("Error in onTestSuccess: " + e.toString());
        }
    }

    @Override
    public void onTestFailure(ITestResult testResult) {
        if (!ctx.isActive())
            return;
        try {
            TestNGReporterHelper.failTestMethod(ctx.getReporter(), testResult);
            ctx.setCurrentTestClass(null);
        }
        catch (Exception e) {
            System.err.println("Error in onTestFailure: " + e.toString());
        }
    }

    @Override
    public void onTestSkipped(ITestResult testResult) {
        if (!ctx.isActive())
            return;
        try {
            TestNGReporterHelper.skipTestMethod(ctx.getReporter(), testResult);
            ctx.setCurrentTestClass(null);
        }
        catch (Exception e) {
            System.err.println("Error in onTestSkipped: " + e.toString());
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    @Override
    public void onStart(ISuite suite) {
        if (ctx.isActive())
            TestNGReporterHelper.startSuite(ctx.getReporter(), suite);
    }

    @Override
    public void onFinish(ISuite suite) {
        if (ctx.isActive())
            TestNGReporterHelper.endSuite(ctx.getReporter(), suite);
    }

    @Override
    public void onStart(ITestContext testContext) {

    }

    @Override
    public void onFinish(ITestContext testContext) {

    }

    /* Private */
    private void setup() {
        if (!ctx.isActive() || started)
            return;
        started = true;
        ctx.getReporter().setFramework("TestNG", null);
        TestNGReporterHelper.startInstance(ctx.getReporter());
    }

    private void shutdown() throws Throwable {
        if (!ctx.isActive() || !started)
            return;
        started = false;
        TestNGReporterHelper.endInstance(ctx.getReporter());
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!ctx.isActive() || !started)
            return;
        if (!method.isConfigurationMethod())
            return;
        ITestNGMethod testMethod = method.getTestMethod();
        if (testMethod.isBeforeClassConfiguration())
            TestNGReporterHelper.startBeforeClassHook(ctx.getReporter(), testMethod);
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!ctx.isActive() || !started)
            return;
        if (!method.isConfigurationMethod())
            return;
        ITestNGMethod testMethod = method.getTestMethod();
        if (testMethod.isBeforeClassConfiguration())
            TestNGReporterHelper.endBeforeClassHook(ctx.getReporter(), testMethod);
    }
}
