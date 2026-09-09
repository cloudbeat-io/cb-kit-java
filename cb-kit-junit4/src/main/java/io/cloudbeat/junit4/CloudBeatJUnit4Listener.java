/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.cloudbeat.junit4;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.TestStatus;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * CloudBeat reporter plugin for JUnit 4.
 *
 * JUnit 4.12's RunListener has no explicit "class/suite started" event (that was only added in
 * 4.13's testSuiteStarted/testSuiteFinished), so a suite (= test class) boundary is inferred by
 * tracking the class name of consecutive testStarted/testIgnored calls on this thread.
 */
@RunListener.ThreadSafe
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity",
        "ClassDataAbstractionCoupling",
        "unused"
})
public class CloudBeatJUnit4Listener extends RunListener {
    private final CbTestContext ctx = CbTestContext.getInstance();
    private final ThreadLocal<String> currentSuiteFqn = new ThreadLocal<>();

    public CloudBeatJUnit4Listener() {
    }

    @Override
    public void testRunStarted(final Description description) {
        if (!ctx.isActive())
            return;
        final CbTestReporter reporter = ctx.getReporter();
        reporter.setFramework("JUnit", "4");
        if (!reporter.getInstance().isPresent())
            reporter.startInstance();
    }

    @Override
    public void testRunFinished(final Result result) {
        if (!ctx.isActive())
            return;
        final CbTestReporter reporter = ctx.getReporter();
        endCurrentSuite(reporter);
        if (reporter.getInstance().isPresent())
            reporter.endInstance();
    }

    @Override
    public void testStarted(final Description description) {
        if (!ctx.isActive())
            return;
        final CbTestReporter reporter = ctx.getReporter();
        ensureSuiteStarted(reporter, description);
        reporter.startCase(description.getMethodName(), getMethodFqn(description));
    }

    @Override
    public void testFinished(final Description description) {
        if (!ctx.isActive())
            return;
        // guarded by CbTestReporter.endCase (no-op if already ended by testFailure/testAssumptionFailure)
        ctx.getReporter().endCase(getMethodFqn(description), null, null);
    }

    @Override
    public void testFailure(final Failure failure) {
        if (!ctx.isActive())
            return;
        ctx.getReporter().endCase(getMethodFqn(failure.getDescription()), TestStatus.FAILED, failure.getException());
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
        if (!ctx.isActive())
            return;
        // a failed assumption means the test was skipped, not failed
        ctx.getReporter().endCase(getMethodFqn(failure.getDescription()), TestStatus.SKIPPED, null);
    }

    @Override
    public void testIgnored(final Description description) {
        if (!ctx.isActive())
            return;
        // @Ignore'd tests never get testStarted/testFinished, only this standalone event
        final CbTestReporter reporter = ctx.getReporter();
        ensureSuiteStarted(reporter, description);
        final String methodFqn = getMethodFqn(description);
        reporter.startCase(description.getMethodName(), methodFqn);
        reporter.endCase(methodFqn, TestStatus.SKIPPED, null);
    }

    private void ensureSuiteStarted(final CbTestReporter reporter, final Description description) {
        final String suiteFqn = description.getClassName();
        if (suiteFqn.equals(currentSuiteFqn.get()))
            return;
        endCurrentSuite(reporter);
        final Class<?> testClass = description.getTestClass();
        reporter.startSuite(testClass != null ? testClass.getSimpleName() : suiteFqn, suiteFqn);
        currentSuiteFqn.set(suiteFqn);
    }

    private void endCurrentSuite(final CbTestReporter reporter) {
        final String suiteFqn = currentSuiteFqn.get();
        if (suiteFqn == null)
            return;
        reporter.endSuite(suiteFqn);
        currentSuiteFqn.remove();
    }

    private static String getMethodFqn(final Description description) {
        return String.format("%s#%s", description.getClassName(), description.getMethodName());
    }
}
