package io.cloudbeat.junit;

import java.util.*;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.StepResult;

import org.junit.jupiter.api.extension.*;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

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
        TestWatcher,
        InvocationInterceptor
{
    static boolean started = false;
    static CbTestContext ctx = CbTestContext.getInstance();
    final ThreadLocal<Map<String, BeforeTestMethodHookInvocationDetails>> beforeMethodHookInvocationMap
            = ThreadLocal.withInitial(HashMap::new);

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

    public static Map<String, Object> getCapabilities(Map<String, Object> defaultCapabilities) {
        if (ctx == null || ctx.getConfig() == null)
            return defaultCapabilities;
        return ctx.getConfig().getCapabilities();
    }

    public static Map<String, Object> getCapabilities() {
        return getCapabilities(null);
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

    public static void logInfo(final String message) {
        if (ctx == null || ctx.getReporter() == null) {
            System.out.println(message);
            return;
        }
        ctx.getReporter().logInfo(message);
    }

    public static void logWarning(final String message) {
        if (ctx == null || ctx.getReporter() == null) {
            System.out.println(message);
            return;
        }
        ctx.getReporter().logWarning(message);
    }

    public static void logError(final String message) {
        logError(message, null);
    }
    public static void logError(final String message, Throwable throwable) {
        if (ctx == null || ctx.getReporter() == null || message == null) {
            System.err.println(message);
            if (throwable != null)
                System.err.println(throwable);
            return;
        }
        ctx.getReporter().logError(message, throwable);
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
    public void interceptBeforeEachMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {
        if (ctx.isActive()) {
            final String testMethodFqn = JunitReporterUtils.getTestMethodFqn(extensionContext);
            final String hookClassFqn = invocationContext.getExecutable().getDeclaringClass().getName();
            final String hookName = invocationContext.getExecutable().getName();
            final String hookFqn = String.format(JunitReporterUtils.JAVA_METHOD_FQN_FORMAT, hookClassFqn, hookName);
            BeforeTestMethodHookInvocationDetails hookInvocationDetails = new BeforeTestMethodHookInvocationDetails();
            hookInvocationDetails.start(hookName, hookFqn, extensionContext);
            beforeMethodHookInvocationMap.get().put(testMethodFqn, hookInvocationDetails);
            try {
                invocation.proceed();
            }
            catch (Throwable e) {
                hookInvocationDetails.end(e);
                throw e;
            }
            hookInvocationDetails.end();
        }
        else
            invocation.proceed();
    }

    @Override
    public void interceptAfterEachMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {
        if (ctx.isActive()) {
            final String hookClassFqn = invocationContext.getExecutable().getDeclaringClass().getName();
            final String hookName = invocationContext.getExecutable().getName();
            final String hookFqn = String.format(JunitReporterUtils.JAVA_METHOD_FQN_FORMAT, hookClassFqn, hookName);
            JunitReporterUtils.startCaseHook(
                ctx.getReporter(),
                hookName,
                hookFqn,
                false,
                null);
            try {
                invocation.proceed();
            }
            catch (Throwable e) {
                JunitReporterUtils.endCaseHook(
                    ctx.getReporter(),
                    hookFqn,
                    e,
                    null);
                throw e;
            }
            JunitReporterUtils.endCaseHook(
                ctx.getReporter(),
                hookFqn,
                null,
                null);
        }
        else
            invocation.proceed();
    }
    @Override
    public void afterAll(ExtensionContext context) {
        if (ctx.isActive())
            JunitReporterUtils.endSuite(ctx.getReporter(), context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
    }

    @Override
    public void afterEach(ExtensionContext context) {
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        if (!ctx.isActive())
            return;
        try {
            ctx.setCurrentTestClass(context.getTestClass().orElse(null));
            JunitReporterUtils.startCase(ctx.getReporter(), context);
            // if there are pending "beforeEach" invocations, append them to the test case
            addPendingBeforeHooks();
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
            addPendingBeforeHooks();
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
        if (reporter == null) {
            stepFunc.run();
            return;
        }
        reporter.step(name, stepFunc);
    }

    private static CbTestReporter getReporter() {
        if (CbJunitExtension.ctx == null)
            return null;
        return CbJunitExtension.ctx.getReporter();
    }

    private void addPendingBeforeHooks() {
        beforeMethodHookInvocationMap.get().entrySet().forEach(entry -> {
            BeforeTestMethodHookInvocationDetails hookInvocationDetails = entry.getValue();
            JunitReporterUtils.startCaseHook(
                    ctx.getReporter(),
                    hookInvocationDetails.getHookName(),
                    hookInvocationDetails.getHookFqn(),
                    true,
                    hookInvocationDetails.getStartTime());
            JunitReporterUtils.endCaseHook(
                    ctx.getReporter(),
                    hookInvocationDetails.getHookFqn(),
                    hookInvocationDetails.getThrowable(),
                    hookInvocationDetails.getEndTime());
        });
        beforeMethodHookInvocationMap.remove();
    }

    class BeforeTestMethodHookInvocationDetails {
        @Nullable
        Throwable throwable;
        @Nullable
        ExtensionContext context;
        @Nullable
        String hookName;
        @Nullable
        String hookFqn;
        @Nullable
        Long startTime;
        @Nullable
        Long endTime;

        public void start(String hookName, String hookFqn, ExtensionContext context) {
            this.context = context;
            this.hookName = hookName;
            this.hookFqn = hookFqn;
            this.startTime = Calendar.getInstance().getTimeInMillis();
        }
        public void end(Throwable throwable) {
            this.throwable = throwable;
            this.endTime = Calendar.getInstance().getTimeInMillis();
        }
        public void end() {
            end(null);
        }
        @Nullable
        public ExtensionContext getContext() {
            return context;
        }

        @Nullable
        public Long getStartTime() {
            return startTime;
        }

        @Nullable
        public Long getEndTime() {
            return endTime;
        }

        @Nullable
        public String getHookName() {
            return hookName;
        }

        @Nullable
        public String getHookFqn() {
            return hookFqn;
        }

        @Nullable
        public Throwable getThrowable() {
            return throwable;
        }
    }
}
