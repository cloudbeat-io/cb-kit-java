package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.common.reporter.model.extra.hook.HookStepExtra;
import io.cloudbeat.common.reporter.model.extra.hook.HookType;
import io.cloudbeat.common.reporter.serializer.EpochTimeSerializer;
import io.cloudbeat.common.reporter.serializer.TestStatusSerializer;

import javax.annotation.Nullable;
import java.util.*;

public class SuiteResult implements IResultWithAttachment {
    String id;
    String name;
    @JsonSerialize(using = EpochTimeSerializer.class)
    long startTime;
    @Nullable
    @JsonSerialize(using = EpochTimeSerializer.class)
    Long endTime;
    @Nullable
    Long duration;
    String fqn;
    @JsonSerialize(using = TestStatusSerializer.class)
    TestStatus status;
    ArrayList<String> args;
    ArrayList<StepResult> hooks = new ArrayList<>();
    ArrayList<CaseResult> cases = new ArrayList<>();
    ArrayList<LogMessage> logs = new ArrayList<>();

    ArrayList<Attachment> attachments = new ArrayList<>();

    public SuiteResult(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.startTime = Calendar.getInstance().getTimeInMillis();
    }

    public void end() {
        this.endTime = Calendar.getInstance().getTimeInMillis();
        this.duration = endTime - startTime;
        // determine if at least one of the cases has failed
        boolean hasFailedCases = cases.stream().anyMatch(x -> x.status == TestStatus.FAILED);
        // set suite's status to Failed if at least one case has failed
        if (hasFailedCases)
            this.status = TestStatus.FAILED;
        else {
            boolean hasFailedHooks = hooks.stream().anyMatch(x -> x.status == TestStatus.FAILED);
            if (hasFailedHooks)
                this.status = TestStatus.FAILED;
            else {
                boolean hasAllSkippedCases = cases.stream().allMatch(x -> x.status == TestStatus.SKIPPED);
                this.status = hasAllSkippedCases ? TestStatus.SKIPPED : TestStatus.PASSED;
            }
        }
    }

    public CaseResult addNewCaseResult(String name) {
        CaseResult newCase = new CaseResult(name);
        cases.add(newCase);
        return newCase;
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

    public Optional<CaseResult> lastCase(String fqn) {
        for (int i = cases.size(); i-- > 0; ) {
            final CaseResult caseResult = cases.get(i);
            final String caseFqn = caseResult.getFqn();
            if (caseFqn != null && caseFqn.equals(fqn))
                return Optional.of(caseResult);
        }
        return Optional.empty();

    }
    /* Setters */
    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    /* Getters */
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public Long getDuration() { return duration; }

    public String getFqn() {
        return fqn;
    }

    public TestStatus getStatus() {
        return status;
    }

    public List<String> getArgs() {
        return args;
    }

    public List<CaseResult> getCases() {
        return cases;
    }

    public List<LogMessage> getLogs() { return logs; }

    public List<StepResult> getHooks() { return hooks; }

    @Override
    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
    }

    @Override
    public List<Attachment> getAttachments() {
        return attachments;
    }
}
