package io.cloudbeat.common.reporter.model;

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

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value();
    }
}
