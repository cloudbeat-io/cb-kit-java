package io.cloudbeat.common.client.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.common.reporter.serializer.EpochTimeSerializer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoadTestMetricsUpdateRequest {
    @Nonnull
    String runId;
    @Nonnull
    String instanceId;
    @Nonnull
    @JsonSerialize(using = EpochTimeSerializer.class)
    long timestamp;
    @Nonnull
    final ArrayList<CaseMetricsUpdateDto> caseList = new ArrayList<>();

    @Nonnull
    final Map<String, Number> metrics = new HashMap<>();

    public ArrayList<CaseMetricsUpdateDto> getCaseList() {
        return caseList;
    }

    public Map<String, Number> getMetrics() {
        return metrics;
    }

    public String getRunId() {
        return runId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setRunId(@Nonnull String runId) {
        this.runId = runId;
    }

    public void setInstanceId(@Nonnull String instanceId) {
        this.instanceId = instanceId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void addCaseMetrics(CaseMetricsUpdateDto caseMetrics) {
        caseList.add(caseMetrics);
    }
}
