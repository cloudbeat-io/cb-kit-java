package io.cloudbeat.common.reporter.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AttachmentSubType {
    IMAGE_SCREENSHOT("screenshot"),
    VIDEO_SCREENCAST("screencast"),
    NETWORK_HAR("har");

    private final String value;

    AttachmentSubType(final String v) {
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
