package io.cloudbeat.common.reporter.model;

public enum LogLevel {
    INFO("info"),
    ERROR("error"),
    WARNING("warn"),
    DEBUG("debug");

    private final String value;

    LogLevel(final String v) {
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
