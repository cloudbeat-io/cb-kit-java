package io.cloudbeat.junit;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.CbTestReporter.PendingCaseInfo;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Platform-level listener, auto-registered via META-INF/services ServiceLoader discovery. Unlike
 * {@link CbJunitExtension}, which is scoped per test class and only ever sees its own class,
 * this sees the full discovered TestPlan across every class before any of them run - so it can
 * announce every suite/case that WILL run as "Pending" upfront, instead of the live progress
 * screen only revealing tests one at a time as each class's extension happens to start them.
 */
public class CbJunitExecutionListener implements TestExecutionListener {
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        CbTestContext ctx = CbTestContext.getInstance();
        if (!ctx.isActive())
            return;
        CbTestReporter reporter = ctx.getReporter();
        // testPlanExecutionStarted always fires before any class's beforeAll, so this listener
        // is always the one that bootstraps the instance/reporter first - startInstance/setFramework
        // are both idempotent, so CbJunitExtension.setup() calling them again later is harmless
        if (!reporter.isStarted()) {
            reporter.setFramework("JUnit", "5");
            JunitReporterUtils.startInstance(reporter, true);
        }
        // Suites (classes) are reported individually - there's no bulk endpoint for them, but a
        // suite typically only has a handful of classes, unlike the potentially large number of
        // test methods below, so the per-request overhead there is negligible.
        List<PendingCaseInfo> pendingCases = new ArrayList<>();
        for (TestIdentifier root : testPlan.getRoots()) {
            reportPending(testPlan, root, reporter, pendingCases);
        }
        // Cases are collected and sent as a single bulk request instead of one HTTP call per
        // case - with dozens of test methods, reporting them one at a time (even as fire-and-forget
        // calls) gets throttled by OkHttp's default 5-concurrent-per-host cap, so the full Pending
        // list can take a noticeable while to finish appearing on the live progress screen.
        reporter.reportPendingCases(pendingCases);
    }

    private void reportPending(TestPlan testPlan, TestIdentifier identifier, CbTestReporter reporter, List<PendingCaseInfo> pendingCases) {
        Optional<TestSource> source = identifier.getSource();
        if (source.isPresent() && source.get() instanceof ClassSource) {
            final String classFqn = ((ClassSource) source.get()).getClassName();
            reporter.reportPendingSuite(identifier.getDisplayName(), classFqn);
        }
        else if (source.isPresent() && source.get() instanceof MethodSource) {
            final MethodSource methodSource = (MethodSource) source.get();
            final String classFqn = methodSource.getClassName();
            final String methodFqn = String.format(JunitReporterUtils.JAVA_METHOD_FQN_FORMAT, classFqn, methodSource.getMethodName());
            pendingCases.add(new PendingCaseInfo(identifier.getDisplayName(), methodFqn, classFqn, classFqn));
        }
        for (TestIdentifier child : testPlan.getChildren(identifier)) {
            reportPending(testPlan, child, reporter, pendingCases);
        }
    }
}
