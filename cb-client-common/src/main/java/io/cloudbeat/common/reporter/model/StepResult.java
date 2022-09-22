package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import io.cloudbeat.common.reporter.model.extra.IStepExtra;
import io.cloudbeat.common.reporter.serializer.EpochTimeSerializer;
import io.cloudbeat.common.reporter.serializer.TestStatusSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class StepResult {
    @Nonnull
    String id;
    @Nonnull
    String name;
    @Nonnull
    StepType type;
    @Nullable
    String fqn;
    @Nullable
    String location;
    @JsonSerialize(using = EpochTimeSerializer.class)
    long startTime;
    @JsonSerialize(using = EpochTimeSerializer.class, nullsUsing = NullSerializer.class)
    Long endTime = null;
    Long duration = null;
    @JsonSerialize(using = TestStatusSerializer.class)
    TestStatus status;
    List<String> args;
    FailureResult failure;
    String screenShot;
    final Map<String, IStepExtra> extra = new HashMap<>();
    @JsonIgnore
    StepResult parentStep = null;
    final ArrayList<StepResult> steps = new ArrayList<>();
    Map<String, Number> stats = new HashMap<>();
    final ArrayList<LogMessage> logs = new ArrayList<>();

    public StepResult(String name) {
        this(name, StepType.GENERAL);
    }

    public StepResult(String name, StepType type) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.startTime = Calendar.getInstance().getTimeInMillis();
    }

    public void end() {
        end(null, null);
    }

    public void end(TestStatus status) {
        end(status, null);
    }

    public void end(Throwable throwable) {
        end(null, throwable);
    }

    public void end(TestStatus status, Throwable throwable) { end(status, throwable, null, null); }

    public void end(TestStatus status, Throwable throwable, Long duration) {
        end(status, throwable, duration, null);
    }

    public void end(TestStatus status, Throwable throwable, Long duration, String screenshot) {
        this.endTime = duration == null ? Calendar.getInstance().getTimeInMillis() : this.startTime + duration;
        this.duration = duration == null ? endTime - startTime : duration;
        this.screenShot = screenshot;
        if (throwable != null)
            this.failure = new FailureResult(throwable);
        // calculate status automatically or force the provided status
        this.status = status != null ? status : calculateStepStatus();
    }

    public StepResult addNewSubStep(final String name) {
        return addNewSubStep(name, StepType.GENERAL);
    }

    public StepResult addNewSubStep(final String name, final StepType type) {
        StepResult newSubStep = new StepResult(name, type);
        newSubStep.parentStep = this;
        steps.add(newSubStep);
        return newSubStep;
    }

    public void addLogMessage(final LogMessage logMessage) {
        this.logs.add(logMessage);
    }

    public void addLogs(final List<LogMessage> logs) {
        this.logs.addAll(logs);
    }

    public void addStats(final Map<String, Number> newStats) {
        newStats.keySet().forEach(statName -> {
            if (this.stats.containsKey(statName))
                return;     // do not override existing stats
            this.stats.put(statName, newStats.get(statName));
        });
    }

    private TestStatus calculateStepStatus() {
        if (failure != null)
            return TestStatus.FAILED;
        boolean hasFailedSubStep = steps.stream().anyMatch(x -> x.status == TestStatus.FAILED);
        if (hasFailedSubStep)
            return TestStatus.FAILED;
        // if all step's sub-steps were skipped, then mark the step as skipped
        boolean allSkippedSubSteps = steps.stream().allMatch(x -> x.status == TestStatus.SKIPPED);
        return allSkippedSubSteps ? TestStatus.SKIPPED : TestStatus.PASSED;
    }

    /* Setters */
    public void setFqn(String fqn) { this.fqn = fqn; }
    public void setArgs(List<String > args) { this.args = args; }
    public void setLocation(String location) { this.location = location; }
    public String getLocation() { return location; }
    public void setStats(Map<String, Number> stats) { this.stats = stats; }

    /* Getters */
    public String getId() { return id; }

    public String getName() { return name; }

    public List<String> getArgs() { return args; }

    public String getFqn() { return fqn; }

    public long getStartTime() { return startTime; }

    public Long getEndTime() { return endTime; }

    public Long getDuration() { return duration; }

    public List<StepResult> getSteps() { return steps; }

    public StepResult getParentStep() { return parentStep; }

    public Map<String, Number> getStats() { return stats; }

    public FailureResult getFailure() { return failure; }

    public List<LogMessage> getLogs() { return logs; }

    public String getScreenShot() { return screenShot; }
    public Map<String, IStepExtra> getExtra() { return this.extra; }
    public IStepExtra getExtra(String name) { return extra.getOrDefault(name, null); }
    public  void addExtra(String name, IStepExtra extra) { this.extra.put(name, extra); }
    public  IStepExtra removeExtra(String name) { return this.extra.remove(name); }
}
