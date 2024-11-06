package io.cloudbeat.common.client.dto;

import java.util.ArrayList;
import java.util.Optional;

public class CaseStatusInfoDto {
    String id;
    String caseResultId;
    String name;
    String fqn;
    Optional<Integer> order;
    int iterationsPassed;
    int iterationsFailed;
    int iterationsWarning;
    int iterationsSkipped;
    float progress;
    ArrayList<FailureInfoDto> failures;

    public String getId() {
        return id;
    }

    public String getCaseResultId() {
        return caseResultId;
    }

    public String getName() {
        return name;
    }

    public String getFqn() {
        return fqn;
    }

    public Optional<Integer> getOrder() {
        return order;
    }

    public int getIterationsPassed() {
        return iterationsPassed;
    }

    public int getIterationsFailed() {
        return iterationsFailed;
    }

    public int getIterationsWarning() {
        return iterationsWarning;
    }

    public int getIterationsSkipped() {
        return iterationsSkipped;
    }

    public float getProgress() {
        return progress;
    }

    public ArrayList<FailureInfoDto> getFailures() {
        return failures;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCaseResultId(String id) {
        this.caseResultId = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    public void setOrder(Optional<Integer> order) {
        this.order = order;
    }

    public void setIterationsPassed(int iterationsPassed) {
        this.iterationsPassed = iterationsPassed;
    }

    public void setIterationsFailed(int iterationsFailed) {
        this.iterationsFailed = iterationsFailed;
    }

    public void setIterationsWarning(int iterationsWarning) {
        this.iterationsWarning = iterationsWarning;
    }

    public void setIterationsSkipped(int iterationsSkipped) {
        this.iterationsSkipped = iterationsSkipped;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setFailures(ArrayList<FailureInfoDto> failures) {
        this.failures = failures;
    }

    public static class FailureInfoDto {
        String message;
        String type;
        String subtype;
        Optional<Integer> line;
        String details;
        boolean isFatal;

        public String getMessage() {
            return message;
        }

        public String getType() {
            return type;
        }

        public String getSubtype() {
            return subtype;
        }

        public Optional<Integer> getLine() {
            return line;
        }

        public String getDetails() {
            return details;
        }

        public boolean getIsFatal() {
            return isFatal;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setSubtype(String subtype) {
            this.subtype = subtype;
        }

        public void setLine(Optional<Integer> line) {
            this.line = line;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public void setIsFatal(boolean fatal) {
            isFatal = fatal;
        }
    }
}
