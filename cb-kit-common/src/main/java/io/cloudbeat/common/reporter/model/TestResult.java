package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.common.reporter.model.extra.hook.HookStepExtra;
import io.cloudbeat.common.reporter.model.extra.hook.HookType;
import io.cloudbeat.common.reporter.serializer.EpochTimeSerializer;
import io.cloudbeat.common.reporter.serializer.TestStatusSerializer;

import java.util.*;

public class TestResult {
    String instanceId;
    String runId;
    @JsonSerialize(using = EpochTimeSerializer.class)
    long startTime;
    @JsonSerialize(using = EpochTimeSerializer.class)
    long endTime;
    long duration;
    Map<String, String> options;
    Map<String, Object> capabilities;
    //Map<String, String> attributes = new HashMap<>();
    Map<String, String> metaData;
    Map<String, String> environmentVariables;

    List<FailureResult> failures;
    ArrayList<StepResult> hooks = new ArrayList<>();
    ArrayList<SuiteResult> suites = new ArrayList<>();
    @JsonSerialize(using = TestStatusSerializer.class)
    TestStatus status;

    public TestResult(final String runId, final String instanceId) {
        this(runId, instanceId, null, null, null, null);
    }

    public TestResult(
            final String runId,
            final String instanceId,
            final Map<String, Object> capabilities,
            final Map<String, String> options,
            final Map<String, String> metaData,
            final Map<String, String> environmentVariables
    ) {
        this.runId = runId;
        this.instanceId = instanceId;
        this.capabilities = capabilities != null ? capabilities : new HashMap<>();
        this.options = options;
        if (metaData == null)
            this.metaData = new HashMap<>();
        else
            this.metaData = metaData;
        this.environmentVariables = environmentVariables;
        startTime = Calendar.getInstance().getTime().getTime();
    }

    public void end(TestStatus status) {
        endTime = Calendar.getInstance().getTime().getTime();
        this.duration = endTime - startTime;
        this.status = status;
    }

    public void end() {
        this.end(this.calculateFinalTestResultStatus());
    }

    public SuiteResult addNewSuite(final String name) {
        SuiteResult newSuite = new SuiteResult(name);
        suites.add(newSuite);
        return newSuite;
    }

    public StepResult addNewHook(final String name, final HookType type) {
        StepResult hookResult = new StepResult(name, StepType.HOOK);
        hookResult.addExtra("hook", new HookStepExtra(type));
        hooks.add(hookResult);
        return hookResult;
    }

    public void addAttribute(final String name, final String value) {
        this.metaData.put(name, value);
    }

    public void addCapability(final String name, final String value) {
        this.capabilities.put(name, value);
    }

    public Optional<SuiteResult> lastSuite() {
        if (suites.size() > 0)
            return Optional.of(suites.get(suites.size() - 1));
        return Optional.empty();
    }

    public Optional<SuiteResult> lastSuite(String fqn) {
        for (int i = suites.size(); i-- > 0; ) {
            final SuiteResult suite = suites.get(i);
            final String suiteFqn = suite.getFqn();
            if (suiteFqn != null && suiteFqn.equals(fqn))
                return Optional.of(suite);
        }
        return Optional.empty();
    }

    /* Getters */

    public String getRunId() { return runId; }

    public String getInstanceId() { return instanceId; }

    public long getStartTime() { return startTime; }

    public long getEndTime() { return endTime; }

    public long getDuration() { return duration; }

    public List<SuiteResult> getSuites() { return suites; }

    public Map<String, String> getOptions() {
        return options;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public List<FailureResult> getFailures() {
        return failures;
    }

    public void addFailure(FailureResult failure) {
        if (failures == null)
            failures = new ArrayList<>();
        failures.add(failure);
    }

    public TestStatus getStatus() {
        return status;
    }

    private TestStatus calculateFinalTestResultStatus() {
        boolean hasFailedSuites = suites.stream().anyMatch(s -> s.status == TestStatus.FAILED);
        boolean allSkipped = suites.stream().allMatch(s -> s.status == TestStatus.SKIPPED);
        if (hasFailedSuites)
            return TestStatus.FAILED;
        if (allSkipped)
            return TestStatus.SKIPPED;
        return TestStatus.PASSED;
    }
}
