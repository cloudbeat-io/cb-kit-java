package io.cloudbeat.cucumber;

import cucumber.runtime.HookDefinition;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.Argument;
import gherkin.formatter.model.*;
import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.CaseResult;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.reporter.model.StepType;
import io.cloudbeat.common.reporter.model.TestStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class CucumberUtils {
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_PASSED = "passed";
    private static final String STATUS_SKIPPED = "skipped";
    private static final String STATUS_PENDING = "pending";
    private static final String RESOURCES_PREFIX_MAIN = "src/main/resources/";
    private static final String RESOURCES_PREFIX_TEST = "src/test/resources/";
    public static void startSuite(final String featureUri, final Feature feature) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            final String fqn = getSuiteFqnFromUri(featureUri);
            final String name = getSuiteName(feature);
            reporter.startSuite(name, fqn);
        }
    }

    public static void endSuite(final String featureUri) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            reporter.endSuite(getSuiteFqnFromUri(featureUri));
        }
    }

    public static void startCase(final String featureUri, final TagStatement scenario) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            final String name = getCaseName(scenario);
            final String fqn = getCaseFqn(scenario);
            try {
                CaseResult caseResult = reporter.startCase(name, fqn, getSuiteFqnFromUri(featureUri));
                caseResult.setDescription(scenario.getDescription());
            } catch (Throwable e) {}
        }
    }

    public static void endCase(final String fqn) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            try {
                reporter.endCase(fqn);
            }
            catch (Throwable e) {}
        }
    }

    public static void endCase(final ScenarioOutline so) {
        endCase(getCaseFqn(so));
    }

    // Start Examples step
    public static void startStep(Examples examples) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            reporter.startStep(
                getStepName(examples),
                StepType.GENERAL
            );
        }
    }
    // Start Examples Table Row step
    public static void startStep(Examples examples, ExamplesTableRow exampleRow) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            reporter.startStep(
                    getStepName(examples, exampleRow),
                    StepType.GENERAL
            );
        }
    }

    public static void startStep(
            final StepDefinitionMatch stepDefinition
    ) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            StepResult newStepResult = reporter.startStep(
                    getStepName(stepDefinition),
                    StepType.GENERAL,
                    stepDefinition.getLocation(),
                    stepDefinition.getArguments().stream().map(Argument::getVal).collect(Collectors.toList())
            );
            if (newStepResult != null)
                newStepResult.setLocation(getStepLocation(stepDefinition));
        }
    }
    public static void endStep(StepDefinitionMatch step, Result result) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            Throwable exception = null;
            TestStatus status = TestStatus.PASSED;
            switch (result.getStatus()) {
                case STATUS_FAILED:
                    exception = result.getError();
                    status = TestStatus.FAILED;
                    break;
                case STATUS_PENDING:
                case STATUS_SKIPPED:
                    status = TestStatus.SKIPPED;
                    break;
                default:
                    break;
            }
            reporter.endStep(step.getLocation(), status, exception);
        }
    }
    public static void endLastStep() {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            reporter.endLastStep();
        }
    }

    public static void beforeScenarioHook(final HookDefinition hook, final Scenario scenario) {

    }
    public static void beforeScenarioHook(final Match match, final Result result) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            // do nothing if CloudBeat Cucumber runner is used, as it will handle hooks
            if (reporter.getRunnerName() != null && reporter.getRunnerName().equals("CbCucumberRunner"))
                return;
            final StepResult hookResult = reporter.beforeCaseHook(match.getLocation(), getTestStatus(result));
            hookResult.end(getTestStatus(result), result.getError(), result.getDuration());
        }
    }

    public static void afterScenarioHook(final Match match, final Result result) {
        final CbTestReporter reporter = CbTestContext.getInstance().getReporter();
        if (CbTestContext.getInstance().isActive() && reporter != null && reporter.isStarted()) {
            // do nothing if CloudBeat Cucumber runner is used, as it will handle hooks
            if (reporter.getRunnerName() != null && reporter.getRunnerName().equals("CbCucumberRunner"))
                return;
            final StepResult hookResult = reporter.afterCaseHook(match.getLocation(), getTestStatus(result));
            hookResult.end(getTestStatus(result), result.getError(), result.getDuration());
        }
    }

    private static TestStatus getTestStatus(Result result) {
        switch (result.getStatus()) {
            case STATUS_FAILED:
                return TestStatus.FAILED;
            case STATUS_PENDING:
            case STATUS_SKIPPED:
                return TestStatus.SKIPPED;
            case STATUS_PASSED:
            default:
                return TestStatus.PASSED;
        }
    }
    private static String getSuiteFqnFromUri(final String featureUri) {
        if (featureUri.startsWith(RESOURCES_PREFIX_MAIN))
            return featureUri.replaceFirst(RESOURCES_PREFIX_MAIN, "");
        else if (featureUri.startsWith(RESOURCES_PREFIX_TEST))
            return featureUri.replaceFirst(RESOURCES_PREFIX_TEST, "");
        return featureUri;
    }

    private static String getSuiteName(final Feature feature) {
        return String.format("%s: %s", feature.getKeyword(), feature.getName());
    }

    private static String getCaseName(final TagStatement scenario) {
        return String.format("%s: %s", scenario.getKeyword(), scenario.getName());
    }

    private static String getStepName(final StepDefinitionMatch stepDefinitionMatch) {
        return stepDefinitionMatch.getStepLocation().getMethodName();
        //return String.format("%s: %s", stepDefinitionMatch.getStepName().getKeyword(), statement.getName());
    }
    private static String getStepName(final Examples statement) {
        String headers;
        if (!statement.getRows().isEmpty())
            headers = String.format(" (%s)", String.join(" | ", statement.getRows().get(0).getCells()));
        else
            headers = "";
        return String.format("%s: %s%s", statement.getKeyword(), statement.getName(), headers);
    }

    private static String getStepName(final Examples examples, final ExamplesTableRow examplesRow) {
        // get header row
        if (examples.getRows().isEmpty())   // no suppose to happen
            return String.join(" | ", examplesRow.getCells());
        List<String> headerNames = examples.getRows().get(0).getCells();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i< headerNames.size(); i++) {
            sb
                    .append(headerNames.get(i))
                    .append(": ")
                    .append(examplesRow.getCells().get(i));
            if (i < headerNames.size() - 1)
                sb.append(" | ");
        }
        return sb.toString();
    }

    public static String getCaseFqn(final TagStatement scenario) {
        return scenario.getId();
    }

    private static String getStepLocation(final StepDefinitionMatch stepDefinition) {
        return String.format("%s:%s",
                stepDefinition.getStepLocation().getFileName(),
                stepDefinition.getStepLocation().getLineNumber());
    }

}
