package io.cloudbeat.common.client.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle status for the new runtime/case/suite status API, numerically aligned with the
 * platform-wide contract (matches CloudBeat.Infrastructure.Models RunStatus in cb-new-arch and
 * the RunStatusEnum used by the Node/.NET reporters).
 */
public enum RunStatusEnum {
    PENDING(0),
    INITIALIZING(1),
    RUNNING(2),
    FINISHED(3),
    CANCELED(5);

    private final int value;

    RunStatusEnum(final int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }
}
