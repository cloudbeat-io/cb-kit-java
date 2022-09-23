package io.cloudbeat.common.reporter.model;

import java.util.Calendar;
import java.util.UUID;

public class LogMessage {
    String id;
    String message;
    String level;
    long timestamp;
    FailureResult failure;

    public LogMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Calendar.getInstance().getTimeInMillis();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setFailure(FailureResult failure) {
        this.failure = failure;
    }

    public String getId() { return id; }

    public String getMessage() {
        return message;
    }

    public String getLevel() {
        return level;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
