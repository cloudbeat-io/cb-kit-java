package io.cloudbeat.common.reporter.model.extra.hook;

import com.fasterxml.jackson.annotation.JsonValue;

public enum HookType {
    BEFORE("before"),
    AFTER("after");

    private final String value;

    HookType(final String v) {
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
