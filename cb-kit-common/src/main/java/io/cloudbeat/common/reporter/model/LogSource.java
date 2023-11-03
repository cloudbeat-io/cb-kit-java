package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LogSource {
    BROWSER("browser"),
    USER("user"),
    SYSTEM("system"),
    DEVICE("device");

    private final String value;

    LogSource(final String v) {
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
