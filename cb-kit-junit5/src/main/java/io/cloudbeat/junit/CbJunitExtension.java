package io.cloudbeat.junit;

import java.io.Console;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.Helper;
import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.restassured.CbRestAssuredFilter;
import io.cloudbeat.common.restassured.RestAssuredFailureListener;

import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Method;
import java.util.function.Function;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

//@AutoService(TestExecutionListener.class)
public class CbJunitExtension implements
        BeforeAllCallback,
        BeforeEachCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        AfterEachCallback,
        AfterAllCallback,
        ExtensionContext.Store.CloseableResource,
        TestWatcher
{
    static boolean started = false;
    static CbTestContext ctx = CbTestContext.getInstance();

    public CbJunitExtension() {

    }

    public static String getEnv(String name) {
        if (ctx == null || ctx.getReporter() == null)
            return System.getenv(name);
        Map<String, String> cbEnvVars = ctx.getConfig().getEnvironmentVariables();
        if (cbEnvVars != null && cbEnvVars.containsKey(name))
            return cbEnvVars.get(name);
        return System.getenv(name);
    }

    public static <D> D wrapWebDriver(D driver) {
        return ctx.wrapWebDriver(driver);
    }

    public static <D, L> L getWebDriverListener(D driver) {
        return ctx.getWebDriverListener(driver);
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
    public synchronized void beforeAll(ExtensionContext context) {
        if (!started) {
            started = true;
            // The following line registers a callback hook when the root test context is shut down
            context.getRoot().getStore(GLOBAL).put("CB-JUNIT-EXT", this);
        }
        if (ctx.isActive() && !ctx.getReporter().isStarted())
            setup(context);

        if (ctx.isActive())
            JunitReporterUtils.startSuite(ctx.getReporter(), context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (ctx.isActive())
            JunitReporterUtils.endSuite(ctx.getReporter(), context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (ctx.isActive())
            JunitReporterUtils.startBeforeEachHook(ctx.getReporter(), context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (ctx.isActive())
            JunitReporterUtils.endBeforeEachHook(ctx.getReporter(), context);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        if (!ctx.isActive())
            return;
        try {
            ctx.setCurrentTestClass(context.getTestClass().orElse(null));
            JunitReporterUtils.startCase(ctx.getReporter(), context);
        }
        catch (Exception e) {
            System.err.println("Error in beforeTestExecution: " + e.toString());
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (!ctx.isActive())
            return;
        try {
            JunitReporterUtils.endCase(ctx.getReporter(), context);
            ctx.setCurrentTestClass(null);
        }
        catch (Exception e) {
            System.err.println("Error in afterTestExecution: " + e.toString());
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        if (!ctx.isActive())
            return;
        try {
            JunitReporterUtils.disabledCase(ctx.getReporter(), context, reason);
        }
        catch (Exception e) {
            System.err.println("Error in testDisabled: " + e.toString());
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable throwable) {
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable throwable) {
        if (!ctx.isActive())
            return;
        try {
            JunitReporterUtils.failedCase(ctx.getReporter(), context, throwable);
            ctx.setCurrentTestClass(null);
        }
        catch (Throwable e) {
            System.err.println("Error in testFailed: " + e.toString());
        }
    }

    private void setup(final ExtensionContext context) {
        if (!ctx.isActive())
            return;
        ctx.getReporter().setFramework("JUnit", "5");
        JunitReporterUtils.startInstance(ctx.getReporter());
    }

    @Override
    public void close() throws Throwable {
        if (!ctx.isActive())
            return;
        System.out.println("close - thread: " + Thread.currentThread().getName());
        JunitReporterUtils.endInstance(ctx.getReporter());
        started = false;
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

    public static void step(final String name, Runnable stepFunc) {
        CbTestReporter reporter = getReporter();
        if (reporter == null)
            return;
        reporter.step(name, stepFunc);
    }

    private static CbTestReporter getReporter() {
        if (CbJunitExtension.ctx == null)
            return null;
        return CbJunitExtension.ctx.getReporter();
    }
}
