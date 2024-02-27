package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AttachmentType {
    SCREENSHOT("screenshot"),
    VIDEO("video"),
    TEXT("text"),
    LOG("log"),
    HAR("har"),
    OTHER("other");

    private final String value;

    AttachmentType(final String v) {
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
