package io.cloudbeat.cucumber.runner;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;


/**
 * CloudBeat reporter plugin for JUnit 4.
 */
@RunListener.ThreadSafe
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity",
        "ClassDataAbstractionCoupling",
        "unused"
})
public class CbJUnitRunListener extends RunListener {
    @Override
    public void testRunStarted(final Description description) {
    }

    @Override
    public void testRunFinished(final Result result) {
        final CbTestContext ctx = CbTestContext.getInstance();
        if (ctx.isActive() && ctx.getReporter().isStarted()) {
            ctx.getReporter().endInstance();
        }
    }

    @Override
    public void testStarted(final Description description) {

    }

    @Override
    public void testFinished(final Description description) {

    }

    @Override
    public void testFailure(final Failure failure) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter.isStarted()) {
            // if no suites has been added yet, then this is a global error
            if (!reporter.hasSuites())
                reporter.addGlobalError(failure.getException());
        }
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {

    }

    @Override
    public void testIgnored(final Description description) {

    }
}