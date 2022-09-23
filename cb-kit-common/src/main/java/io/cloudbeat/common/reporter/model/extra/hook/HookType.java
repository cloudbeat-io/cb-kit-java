package io.cloudbeat.common.reporter.model.extra.hook;

public enum HookType {
    BEFORE("before"),
    AFTER("after");

    private final String value;

    HookType(final String v) {
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
