package io.cloudbeat.testng;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.wrapper.webdriver.WrapperOptions;
import org.testng.*;
import org.testng.internal.IResultListener2;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CbTestNGListener implements
        IExecutionListener,
        ISuiteListener,
        IResultListener2,
        IInvokedMethodListener {
    static boolean started = false;
    static CbTestContext ctx = CbTestContext.getInstance();
    final ThreadLocal<Map<String, BeforeTestMethodHookInvocationDetails>> beforeMethodConfigMap
            = ThreadLocal.withInitial(HashMap::new);
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

    public static <D> D wrapWebDriver(D driver, WrapperOptions options) {
        return ctx.wrapWebDriver(driver, options);
    }

    public static <D, L> L getWebDriverListener(D driver) {
        return ctx.getWebDriverListener(driver);
    }
    public static <D, L> L getWebDriverListener(D driver, WrapperOptions options) {
        return ctx.getWebDriverListener(driver, options);
    }

    public static void wrapRestAssured() {
        ctx.wrapRestAssured();
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

    public static void attachScreencastFile(final String videoFilePath) {
        CbTestReporter reporter = getReporter();
        if (reporter != null)
            reporter.addScreencastAttachment(videoFilePath, false);
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
            CbTestNGReporter.startTestMethod(ctx.getReporter(), testResult);
            // check if we have pending "before method" hooks, if yes - append them to the test case
            if (!beforeMethodConfigMap.get().isEmpty()) {
                beforeMethodConfigMap.get().entrySet().forEach(entry -> {
                    BeforeTestMethodHookInvocationDetails hookInvocationDetails = entry.getValue();
                    CbTestNGReporter.startMethodHook(
                            ctx.getReporter(),
                            hookInvocationDetails.getTestMethod(),
                            true,
                            hookInvocationDetails.getStartTime());
                    CbTestNGReporter.endMethodHook(
                            ctx.getReporter(),
                            hookInvocationDetails.getTestMethod(),
                            hookInvocationDetails.getTestResult(),
                            hookInvocationDetails.getEndTime());
                });
                beforeMethodConfigMap.remove();
            }
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
            CbTestNGReporter.endTestMethod(ctx.getReporter(), testResult);
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
            CbTestNGReporter.failTestMethod(ctx.getReporter(), testResult);
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
            CbTestNGReporter.skipTestMethod(ctx.getReporter(), testResult);
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
            CbTestNGReporter.startSuite(ctx.getReporter(), suite);
    }

    @Override
    public void onFinish(ISuite suite) {
        if (ctx.isActive())
            CbTestNGReporter.endSuite(ctx.getReporter(), suite);
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
        String wrapConsolePropVal = System.getProperty("CB_WRAP_CONSOLE", null);
        boolean wrapConsole;
        if (wrapConsolePropVal == null)
            wrapConsole = true;
        else
            wrapConsole = !wrapConsolePropVal.equalsIgnoreCase("false");

        ctx.getReporter().setFramework("TestNG", null);
        CbTestNGReporter.startInstance(ctx.getReporter(), wrapConsole);
    }

    private void shutdown() throws Throwable {
        if (!ctx.isActive() || !started)
            return;
        started = false;
        CbTestNGReporter.endInstance(ctx.getReporter());
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!ctx.isActive() || !started)
            return;
        if (!method.isConfigurationMethod())
            return;
        ITestNGMethod testMethod = method.getTestMethod();
        if (testMethod.isBeforeClassConfiguration())
            CbTestNGReporter.startClassHook(ctx.getReporter(), testMethod, true);
        else if (testMethod.isAfterClassConfiguration())
            CbTestNGReporter.startClassHook(ctx.getReporter(), testMethod, false);
        else if (testMethod.isBeforeMethodConfiguration()) {
            // save method details for later use when onTestStart will be called
            BeforeTestMethodHookInvocationDetails hookInvocationDetails = new BeforeTestMethodHookInvocationDetails();
            hookInvocationDetails.start(testMethod);
            beforeMethodConfigMap.get().put(testMethod.getQualifiedName(), hookInvocationDetails);
        }
        else if (testMethod.isAfterMethodConfiguration())
            CbTestNGReporter.startMethodHook(ctx.getReporter(), testMethod, false);
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!ctx.isActive() || !started)
            return;
        if (!method.isConfigurationMethod())
            return;
        ITestNGMethod testMethod = method.getTestMethod();
        if (testMethod.isBeforeClassConfiguration() || testMethod.isAfterClassConfiguration())
            CbTestNGReporter.endClassHook(ctx.getReporter(), testMethod, testResult);
        else if (testMethod.isBeforeMethodConfiguration()) {
            if (beforeMethodConfigMap.get().containsKey(testMethod.getQualifiedName()))
                beforeMethodConfigMap.get().get(testMethod.getQualifiedName()).end(testResult);
        }
        else if (testMethod.isAfterMethodConfiguration())
            CbTestNGReporter.endMethodHook(ctx.getReporter(), testMethod, testResult);
    }

    class BeforeTestMethodHookInvocationDetails {
        @Nullable
        ITestNGMethod testMethod;
        @Nullable
        ITestResult testResult;
        @Nullable
        Long startTime;
        @Nullable
        Long endTime;

        public void start(@Nullable ITestNGMethod testMethod) {
            this.testMethod = testMethod;
            this.startTime = Calendar.getInstance().getTimeInMillis();
        }

        public void end(@Nullable ITestResult testResult) {
            this.testResult = testResult;
            this.endTime = Calendar.getInstance().getTimeInMillis();
        }
        @Nullable
        public ITestNGMethod getTestMethod() {
            return testMethod;
        }

        @Nullable
        public ITestResult getTestResult() {
            return testResult;
        }

        @Nullable
        public Long getStartTime() {
            return startTime;
        }

        @Nullable
        public Long getEndTime() {
            return endTime;
        }
    }
}
