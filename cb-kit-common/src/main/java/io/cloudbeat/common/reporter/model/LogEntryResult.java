package io.cloudbeat.common.reporter.model;

public class LogEntryResult {
    long time;
    String level;
    String msg;
    String src;

    public LogEntryResult(long time, String level, String message, String src) {
        this.time = time;
        this.level = level;
        this.msg = message;
        this.src = src;
    }
}
