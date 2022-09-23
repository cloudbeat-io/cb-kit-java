package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.common.reporter.model.extra.hook.HookStepExtra;
import io.cloudbeat.common.reporter.model.extra.hook.HookType;
import io.cloudbeat.common.reporter.serializer.EpochTimeSerializer;
import io.cloudbeat.common.reporter.serializer.TestStatusSerializer;

import javax.annotation.Nullable;
import java.util.*;

public class CaseResult {
    String id;
    String name;
    String description;
    @JsonSerialize(using = EpochTimeSerializer.class)
    long startTime;
    @Nullable
    @JsonSerialize(using = EpochTimeSerializer.class)
    Long endTime;
    @Nullable
    Long duration;
    String fqn;
    ArrayList<String> args;
    @JsonSerialize(using = TestStatusSerializer.class)
    TestStatus status;
    FailureResult failure;
    ArrayList<StepResult> hooks = new ArrayList<>();
    ArrayList<StepResult> steps = new ArrayList<>();
    ArrayList<LogMessage> logs = new ArrayList<>();

    public CaseResult(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.startTime = Calendar.getInstance().getTimeInMillis();
    }

    public void end() {
        end(null, null);
    }

    public void end(Throwable throwable) {
        end(null, throwable);
    }

    public void end(TestStatus status) {
        end(status, null);
    }

    public void end(TestStatus status, Throwable throwable) {
        this.endTime = Calendar.getInstance().getTimeInMillis();
        this.duration = endTime - startTime;
        if (throwable != null)
            this.failure = new FailureResult(throwable);
        this.status = status != null ? status : calculateCaseStatus();
    }

    public StepResult addNewStep(String name) {
        return addNewStep(name, StepType.GENERAL);
    }
    public StepResult addNewStep(String name, StepType type) {
        StepResult newStep;
        steps.add(newStep = new StepResult(name, type));
        return newStep;
    }

    public StepResult addNewHook(final String name, final HookType type) {
        StepResult hookResult = new StepResult(name, StepType.HOOK);
        hookResult.addExtra("hook", new HookStepExtra(type));
        hooks.add(hookResult);
        return hookResult;
    }

    public void addLogMessage(final LogMessage logMessage) {
        this.logs.add(logMessage);
    }

    private TestStatus calculateCaseStatus() {
        // if there is a direct failure attached to the case,
        // mark it as failed, regardless its children status
        if (failure != null)
            return TestStatus.FAILED;
        // determine case status by its children's status
        boolean hasFailedStep = steps.stream().anyMatch(x -> x.status == TestStatus.FAILED);
        if (hasFailedStep)
            return TestStatus.FAILED;
        boolean hasAllSkippedSteps = !steps.isEmpty() && steps.stream().allMatch(x -> x.status == TestStatus.SKIPPED);
        return hasAllSkippedSteps ? TestStatus.SKIPPED : TestStatus.PASSED;
    }

    /* Setters */
    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    public void setFailure(Throwable exception) {
        failure = new FailureResult(exception);
    }

    /* Getters */
    public String getId() { return id; }

    public String getName() { return name; }
    public String getDescription() { return description; }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFqn() {
        return fqn;
    }

    public long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }
    public Long getDuration() {
        return duration;
    }

    public List<String> getArgs() {
        return args;
    }

    public TestStatus getStatus() {
        return status;
    }

    public FailureResult getFailure() {
        return failure;
    }

    public List<StepResult> getSteps() {
        return steps;
    }

    public List<LogMessage> getLogs() { return logs; }
    public List<StepResult> getHooks() { return hooks; }
}
