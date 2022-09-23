package io.cloudbeat.common.reporter.model;

public enum RunStatus {
    PENDING("pending"),
    INITIALIZING("initializing"),
    RUNNING("running"),
    FINISHED("finished"),
    CANCELED("canceled");

    private final String value;

    RunStatus(final String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value();
    }
}
