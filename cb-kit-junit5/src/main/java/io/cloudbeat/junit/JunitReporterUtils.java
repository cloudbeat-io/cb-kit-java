package io.cloudbeat.junit;

import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.CaseResult;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.reporter.model.TestStatus;
import io.cloudbeat.common.wrapper.console.SystemConsoleWrapper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Optional;

public class JunitReporterUtils {
    public static final String JAVA_METHOD_FQN_FORMAT = "%s#%s";
    private static final ThreadLocal<HashMap<String, CaseResult>> caseResultByUniqueId = ThreadLocal.withInitial(HashMap::new);
    final static SystemConsoleWrapper consoleWrapper = new SystemConsoleWrapper();

    public static  void startInstance(CbTestReporter reporter, boolean wrapSystemConsole) {
        // a second JUnit execution in the same JVM (Surefire / Failsafe re-running failed tests) continues the run
        if (!reporter.getInstance().isPresent() && !reporter.reopenInstance())
            reporter.startInstance();
        if (wrapSystemConsole)
            consoleWrapper.start(reporter);
    }

    public static  void endInstance(CbTestReporter reporter) {
        if (reporter.getInstance().isPresent())
            reporter.endInstance();
    }

    public static void startSuite(CbTestReporter reporter, ExtensionContext context) {
        final String classFqn = context.getTestClass().get().getName();
        final String classDisplayName = context.getTestClass().get().getSimpleName();
        // a class can start again in the same run, e.g. Surefire / Failsafe re-running failed tests
        // (rerunFailingTestsCount): continue its suite instead of adding a second one
        reporter.startOrResumeSuite(classDisplayName, classFqn);
        System.out.println("startSuite: " + context.getTestClass().get().getName());
    }

    public static void endSuite(CbTestReporter reporter, ExtensionContext context) {
        final String classFqn = context.getTestClass().get().getName();
        reporter.endSuite(classFqn);
        // Clear cached case results to free up some memory, as caching related to the suite
        caseResultByUniqueId.get().clear();
    }

    /**
     * Failed tests by JUnit unique id, for the whole run (not cleared per suite): when the same test (the same unique
     * id, so not just another invocation of a parameterized test) runs again, it is a re-run of that failure.
     */
    private static final java.util.Map<String, CaseResult> failedCaseByUniqueId = new java.util.concurrent.ConcurrentHashMap<>();

    public static CaseResult startOrGetCase(CbTestReporter reporter, ExtensionContext context) throws Exception {
        String uniqueId = context.getUniqueId();
        if (caseResultByUniqueId.get().containsKey(uniqueId))
            return caseResultByUniqueId.get().get(uniqueId);
        final CaseResult failedBefore = failedCaseByUniqueId.remove(uniqueId);
        if (failedBefore != null)
            reporter.markCaseRetried(failedBefore);
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        CaseResult caseResult = reporter.startCase(methodName, methodFqn, context.getDisplayName());
        caseResultByUniqueId.get().put(uniqueId, caseResult);
        return caseResult;
    }

    public static void endCase(CbTestReporter reporter, ExtensionContext context) throws Exception {
        String uniqueId = context.getUniqueId();
        if (!caseResultByUniqueId.get().containsKey(uniqueId))
            return;
        CaseResult startedCase = caseResultByUniqueId.get().get(uniqueId);
        /*
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        */
        //CbTestReporter reporter = CbTestContext.getReporter();
        if (context.getExecutionException().isPresent()) {
            final Throwable failure = context.getExecutionException().get();
            if (isPioneerRetry(context, failure)) {
                // a failed attempt junit-pioneer will retry: the next invocation continues this result
                reporter.endCase(startedCase, TestStatus.FAILED, failure);
                reporter.markCaseRetried(startedCase);
                return;
            }
            reporter.endCase(startedCase, TestStatus.FAILED, failure);
            failedCaseByUniqueId.put(uniqueId, startedCase);
        }
            // reporter.failCase(methodFqn, context.getExecutionException().get());
        else
            // reporter.endCase(methodFqn);
            reporter.endCase(startedCase, null, null);
    }

    /**
     * junit-pioneer's @RetryingTest aborts (TestAbortedException) every failed attempt it will retry; the last
     * attempt fails normally. Read by name: the kit does not depend on junit-pioneer.
     */
    static boolean isPioneerRetry(ExtensionContext context, Throwable failure) {
        if (failure == null || !"org.opentest4j.TestAbortedException".equals(failure.getClass().getName()))
            return false;
        return context.getTestMethod()
                .map(m -> java.util.Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> "org.junitpioneer.jupiter.RetryingTest".equals(a.annotationType().getName())))
                .orElse(false);
    }

    public static void disabledCase(CbTestReporter reporter, ExtensionContext context, Optional<String> reason) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        //CbTestReporter reporter = CbTestContext.getReporter();
        reporter.startCase(methodName, methodFqn, context.getDisplayName());
        reporter.skipCase(methodFqn);
    }
    public static void failedCase(
            CbTestReporter reporter,
            ExtensionContext context,
            Throwable throwable
    ) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);

        if (reporter.getStartedCase() == null) {
            CaseResult startedCase = reporter.startCase(methodName, methodFqn, context.getDisplayName());
            // startedCase.setStartTime( <set the same start time as the parent suite> );
        }

        reporter.endCase(methodFqn, throwable);
    }
    public static void startSuiteHook(
            CbTestReporter reporter,
            ReflectiveInvocationContext<Method> context,
            boolean isBefore) {
        final String classFqn = context.getTargetClass().getName();
        final String methodName = context.getExecutable().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        reporter.startSuiteHook(methodName, methodFqn, isBefore);
    }
    public static void endSuiteHook(
            CbTestReporter reporter,
            ReflectiveInvocationContext<Method> context,
            Throwable exception) {
        final String classFqn = context.getTargetClass().getName();
        final String methodName = context.getExecutable().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        reporter.endSuiteHook(methodFqn, null, exception);
    }
    public static void startCaseHook(CbTestReporter reporter, ExtensionContext context, boolean isBefore) {
        startCaseHook(reporter, context, isBefore,null);
    }
    public static StepResult startCaseHook(CbTestReporter reporter, String hookName, String hookFqn, boolean isBefore, Long startTime) {
        StepResult hookResult = reporter.startCaseHook(hookName, hookFqn, isBefore);
        if (startTime != null)
            hookResult.setStartTime(startTime);
        return hookResult;
    }
    public static void startCaseHook(CbTestReporter reporter, ExtensionContext context, boolean isBefore, Long startTime) {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        StepResult hookResult = reporter.startCaseHook(methodName, methodFqn, isBefore);
        if (startTime != null)
            hookResult.setStartTime(startTime);
    }

    public static void endCaseHook(CbTestReporter reporter, ExtensionContext context) {
        endCaseHook(reporter, context, null);
    }
    public static void endCaseHook(CbTestReporter reporter, String hookFqn, Throwable throwable, Long endTime) {
        StepResult hookResult = reporter.endCaseHook(hookFqn, throwable);
        if (endTime != null) {
            hookResult.setEndTime(endTime);
            hookResult.setDuration(hookResult.getEndTime() - hookResult.getStartTime());
        }
    }
    public static void endCaseHook(CbTestReporter reporter, ExtensionContext context, Long endTime) {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        StepResult hookResult = reporter.endCaseHook(methodFqn, context.getExecutionException().orElse(null));
        if (endTime != null) {
            hookResult.setEndTime(endTime);
            hookResult.setDuration(hookResult.getEndTime() - hookResult.getStartTime());
        }
    }

    public static String getTestMethodFqn(ExtensionContext context) {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        return String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
    }
}
