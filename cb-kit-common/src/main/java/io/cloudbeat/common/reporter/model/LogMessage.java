package io.cloudbeat.common.reporter.model;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class LogMessage {
    String id;
    String message;
    LogLevel level;
    long time;
    LogSource src;
    List<Object> args;
    FailureResult failure;

    public LogMessage() {
        this.id = UUID.randomUUID().toString();
        this.time = Calendar.getInstance().getTimeInMillis();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setFailure(FailureResult failure) {
        this.failure = failure;
    }

    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public void setSrc(LogSource source) {
        this.src = source;
    }

    public String getId() { return id; }

    public String getMessage() {
        return message;
    }

    public LogLevel getLevel() {
        return level;
    }

    public long getTime() {
        return time;
    }

    public LogSource getSrc() {
        return src;
    }

    public List<Object> getArgs() {
        return args;
    }
}
