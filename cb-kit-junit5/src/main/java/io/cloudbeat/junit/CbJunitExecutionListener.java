package io.cloudbeat.junit;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

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
        for (TestIdentifier root : testPlan.getRoots()) {
            reportPending(testPlan, root, reporter);
        }
    }

    private void reportPending(TestPlan testPlan, TestIdentifier identifier, CbTestReporter reporter) {
        Optional<TestSource> source = identifier.getSource();
        if (source.isPresent() && source.get() instanceof ClassSource) {
            final String classFqn = ((ClassSource) source.get()).getClassName();
            reporter.reportPendingSuite(identifier.getDisplayName(), classFqn);
        }
        else if (source.isPresent() && source.get() instanceof MethodSource) {
            final MethodSource methodSource = (MethodSource) source.get();
            final String classFqn = methodSource.getClassName();
            final String methodFqn = String.format(JunitReporterUtils.JAVA_METHOD_FQN_FORMAT, classFqn, methodSource.getMethodName());
            reporter.reportPendingCase(identifier.getDisplayName(), methodFqn, classFqn, classFqn);
        }
        for (TestIdentifier child : testPlan.getChildren(identifier)) {
            reportPending(testPlan, child, reporter);
        }
    }
}
