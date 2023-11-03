package io.cloudbeat.testng;

import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.CaseResult;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.reporter.model.SuiteResult;
import io.cloudbeat.common.reporter.model.TestStatus;
import io.cloudbeat.common.wrapper.console.SystemConsoleWrapper;
import org.testng.ISuite;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.util.Objects;

public final class CbTestNGReporter {
    final static SystemConsoleWrapper consoleWrapper = new SystemConsoleWrapper();
    public static void startInstance(CbTestReporter reporter) {
        startInstance(reporter, false);
    }
    public static void startInstance(CbTestReporter reporter, boolean wrapSystemConsole) {
        if (!reporter.getInstance().isPresent())
            reporter.startInstance();
        if (wrapSystemConsole)
            consoleWrapper.start(reporter);
    }

    public static  void endInstance(CbTestReporter reporter) {
        if (reporter.getInstance().isPresent())
            reporter.endInstance();
        consoleWrapper.stop();
    }

    public static void startSuite(CbTestReporter reporter, ISuite suite) {
        if (!reporter.getInstance().isPresent())
            return;
        final String displayName = suite.getName();
        final String fqn = generateFqnForSuite(suite.getXmlSuite());
        reporter.startSuite(displayName, fqn);
    }

    private static String generateFqnForSuite(XmlSuite suite) {
        StringBuilder sb = new StringBuilder(suite.getName());
        while (!Objects.isNull(suite.getParentSuite())) {
            sb.insert(0, String.format("%s.", suite.getName()));
        }
        return sb.toString();
    }

    public static void endSuite(CbTestReporter reporter, ISuite suite) {
        if (!reporter.getInstance().isPresent())
            return;
        reporter.endStartedSuite();
    }

    public static void startClassHook(CbTestReporter reporter, ITestNGMethod testMethod, boolean isBeforeHook) {
        if (!reporter.getInstance().isPresent())
            return;
        final String methodDisplayName = testMethod.getMethodName();
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        reporter.startSuiteHook(methodDisplayName, methodFqn, isBeforeHook);
    }
    public static void endClassHook(CbTestReporter reporter, ITestNGMethod testMethod, ITestResult testResult) {
        if (!reporter.getInstance().isPresent())
            return;
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        reporter.endSuiteHook(methodFqn, getCbTestStatusFromTestNgStatus(testResult.getStatus()), testResult.getThrowable());
    }
    public static void startMethodHook(CbTestReporter reporter, ITestNGMethod testMethod, boolean isBeforeHook, Long startTime) {
        if (!reporter.getInstance().isPresent())
            return;
        final String methodDisplayName = testMethod.getMethodName();
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        StepResult hookResult = reporter.startCaseHook(methodDisplayName, methodFqn, isBeforeHook);
        if (startTime != null)
            hookResult.setStartTime(startTime);
    }
    public static void startMethodHook(CbTestReporter reporter, ITestNGMethod testMethod, boolean isBeforeHook) {
        startMethodHook(reporter, testMethod, isBeforeHook, null);
    }
    public static void endMethodHook(CbTestReporter reporter, ITestNGMethod testMethod, ITestResult testResult, Long endTime) {
        if (!reporter.getInstance().isPresent())
            return;
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        StepResult hookResult = reporter.endCaseHook(methodFqn, testResult.getThrowable());
        if (endTime != null) {
            hookResult.setEndTime(endTime);
            hookResult.setDuration(hookResult.getEndTime() - hookResult.getStartTime());
        }
    }
    public static void endMethodHook(CbTestReporter reporter, ITestNGMethod testMethod, ITestResult testResult) {
        endMethodHook(reporter, testMethod, testResult, null);
    }
    public static void startTestMethod(CbTestReporter reporter, ITestResult testResult) throws Exception {
        if (!reporter.getInstance().isPresent())
            return;
        final ITestNGMethod testMethod = testResult.getMethod();
        final String methodDisplayName = testMethod.getMethodName();
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        // startTestMethod might be called twice for the same test method, if the test method
        // has "before method" hooks - in that case, ignore the second call
        // if (reporter.getStartedCase() != null && reporter.getStartedCase().getFqn().equals(methodFqn))
            // return;
        reporter.startCase(methodDisplayName, methodFqn);
    }

    public static void endTestMethod(CbTestReporter reporter, ITestResult testResult) {
        if (!reporter.getInstance().isPresent())
            return;
        final ITestNGMethod testMethod = testResult.getMethod();
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        if (Objects.nonNull(testResult.getThrowable()))
            reporter.endCase(methodFqn, TestStatus.FAILED, testResult.getThrowable());
        else
            reporter.endCase(methodFqn);
    }

    public static void skipTestMethod(CbTestReporter reporter, ITestResult testResult) throws Exception {
        final ITestNGMethod testMethod = testResult.getMethod();
        final String methodDisplayName = testMethod.getMethodName();
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        final String suiteFqn = generateFqnForSuite(testResult.getTestContext().getSuite().getXmlSuite());
        // reporter.startCase(methodDisplayName, methodFqn);
        reporter.skipCase(methodFqn);
    }

    public static void failTestMethod(CbTestReporter reporter, ITestResult testResult) throws Exception {
        if (!reporter.getInstance().isPresent())
            return;
        final ITestNGMethod testMethod = testResult.getMethod();
        final String methodFqn = fixFqnWithHash(testMethod.getQualifiedName());
        reporter.endCase(methodFqn, TestStatus.FAILED, testResult.getThrowable());
    }

    // Helpers
    private static String fixFqnWithHash(final String fqn) {
        int lastDotIndex = fqn.lastIndexOf('.');
        String classFqn = fqn.substring(0, lastDotIndex);
        String methodName = fqn.substring(lastDotIndex + 1, fqn.length());
        return String.format("%s#%s", classFqn, methodName);
    }

    private static TestStatus getCbTestStatusFromTestNgStatus(int tngStatus) {
        switch (tngStatus) {
            case ITestResult.FAILURE:
                return TestStatus.FAILED;
            case ITestResult.SKIP:
                return TestStatus.SKIPPED;
            default:
                return TestStatus.PASSED;
        }
    }
}
