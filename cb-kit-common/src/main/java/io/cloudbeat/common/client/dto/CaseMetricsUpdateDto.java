package io.cloudbeat.common.client.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.common.reporter.model.StepType;
import io.cloudbeat.common.reporter.model.extra.IStepExtra;
import io.cloudbeat.common.reporter.serializer.EpochTimeSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CaseMetricsUpdateDto {
    @JsonSerialize(using = EpochTimeSerializer.class)
    long timestamp;
    String name;
    String fqn;
    final Map<String, Number> metrics = new HashMap<>();
    final ArrayList<StepMetricsUpdateDto> stepList = new ArrayList<>();

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFqn() {
        return fqn;
    }

    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    public Map<String, Number> getMetrics() {
        return metrics;
    }

    public ArrayList<StepMetricsUpdateDto> getStepList() {
        return stepList;
    }

    public static class StepMetricsUpdateDto {
        String name;
        String fqn;
        StepType type;
        final Map<String, Number> metrics = new HashMap<>();
        final Map<String, IStepExtra> extra = new HashMap<>();
        final ArrayList<StepMetricsUpdateDto> stepList = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFqn() {
            return fqn;
        }

        public void setFqn(String fqn) {
            this.fqn = fqn;
        }

        public void setType(StepType type) {
            this.type = type;
        }

        public StepType getType() {
            return type;
        }

        public Map<String, Number> getMetrics() {
            return metrics;
        }

        public Map<String, IStepExtra> getExtra() {
            return extra;
        }

        public ArrayList<StepMetricsUpdateDto> getStepList() {
            return stepList;
        }
    }
}
