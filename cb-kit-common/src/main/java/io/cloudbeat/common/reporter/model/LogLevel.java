package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LogLevel {
    INFO("info"),
    ERROR("error"),
    WARNING("warn"),
    DEBUG("debug");

    private final String value;

    LogLevel(final String v) {
        value = v;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
