package io.cloudbeat.common.client.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.common.reporter.model.TestStatus;
import io.cloudbeat.common.reporter.serializer.TestStatusSerializer;

/**
 * Request body for the new Redis-backed runtime status API:
 * POST runtime/run/{runId}/instance/{instanceId}/suite/status
 * Field shape mirrors SuiteStatusUpdateRequest in cb-new-arch (and its Node/.NET equivalents).
 */
public class SuiteStatusUpdateRequest {
    private Long timestamp;
    private String runId;
    private String instanceId;
    private String id;
    private String fqn;
    private String parentFqn;
    private String parentId;
    private String parentName;
    private String name;
    private Long startTime;
    private Long endTime;
    private RunStatusEnum runStatus;
    @JsonSerialize(using = TestStatusSerializer.class)
    private TestStatus testStatus;
    private String framework;
    private String language;

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFqn() {
        return fqn;
    }

    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    public String getParentFqn() {
        return parentFqn;
    }

    public void setParentFqn(String parentFqn) {
        this.parentFqn = parentFqn;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public RunStatusEnum getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(RunStatusEnum runStatus) {
        this.runStatus = runStatus;
    }

    public TestStatus getTestStatus() {
        return testStatus;
    }

    public void setTestStatus(TestStatus testStatus) {
        this.testStatus = testStatus;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
