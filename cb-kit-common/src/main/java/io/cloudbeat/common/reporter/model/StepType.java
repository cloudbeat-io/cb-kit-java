package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StepType {
    GENERAL("general"),
    HTTP("http"),
    TRANSACTION("transaction"),
    ASSERTION("assert"),
    HOOK("hook");

    private final String value;

    StepType(final String v) {
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
