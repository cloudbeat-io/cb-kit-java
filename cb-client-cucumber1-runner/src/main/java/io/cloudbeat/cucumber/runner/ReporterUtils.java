package io.cloudbeat.cucumber.runner;

import cucumber.api.Scenario;
import cucumber.runtime.HookDefinition;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.StepResult;

final class ReporterUtils {
    public static StepResult startHook(final Scenario scenario, final HookDefinition hook, boolean isBefore) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (!CbTestContext.getInstance().isActive() || reporter == null && !reporter.isStarted())
            return null;
        return reporter.startCaseHook(hook.getLocation(false), isBefore);
    }
    public static void endHook(final StepResult hookResult, Throwable throwable) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (!CbTestContext.getInstance().isActive() || reporter == null && !reporter.isStarted() || hookResult == null)
            return;
        reporter.endCaseHook(hookResult, throwable);
    }
}
