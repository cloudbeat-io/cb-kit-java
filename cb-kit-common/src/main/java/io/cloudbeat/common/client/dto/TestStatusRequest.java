package io.cloudbeat.common.client.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.common.reporter.model.RunStatus;
import io.cloudbeat.common.reporter.serializer.EpochTimeSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class TestStatusRequest {
    @Nonnull
    RunStatus status;
    @Nonnull
    String runId;
    @Nonnull
    String instanceId;
    @Nonnull
    float progress = 0;
    @Nonnull
    @JsonSerialize(using = EpochTimeSerializer.class)
    long timestamp;
    @Nonnull
    String agentId;
    @Nullable
    Optional<String> accountId;
    @Nullable
    Optional<String> userId;
    CaseStatusInfoDto caze;

    @Nonnull
    public RunStatus getStatus() {
        return status;
    }

    @Nonnull
    public String getRunId() {
        return runId;
    }

    @Nonnull
    public String getInstanceId() {
        return instanceId;
    }

    @Nonnull
    public float getProgress() {
        return progress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nonnull
    public String getAgentId() {
        return agentId;
    }

    @Nullable
    public Optional<String> getAccountId() {
        return accountId;
    }

    @Nullable
    public Optional<String> getUserId() {
        return userId;
    }

    public CaseStatusInfoDto getCase() {
        return caze;
    }

    public void setStatus(@Nonnull RunStatus status) {
        this.status = status;
    }

    public void setRunId(@Nonnull String runId) {
        this.runId = runId;
    }

    public void setInstanceId(@Nonnull String instanceId) {
        this.instanceId = instanceId;
    }

    public void setProgress(@Nonnull float progress) {
        this.progress = progress;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setAgentId(@Nonnull String agentId) {
        this.agentId = agentId;
    }

    public void setAccountId(@Nullable Optional<String> accountId) {
        this.accountId = accountId;
    }

    public void setUserId(@Nullable Optional<String> userId) {
        this.userId = userId;
    }

    public void setCase(CaseStatusInfoDto caze) {
        this.caze = caze;
    }
}
