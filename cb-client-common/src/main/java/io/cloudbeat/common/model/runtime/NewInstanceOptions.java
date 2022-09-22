package io.cloudbeat.common.model.runtime;

import io.cloudbeat.common.reporter.model.RunStatus;

import javax.annotation.Nullable;

public class NewInstanceOptions {
    @Nullable
    RunStatus initialStatus;
    public NewInstanceOptions() {}
    public NewInstanceOptions(@Nullable final RunStatus initialStatus) {
        this.initialStatus = initialStatus;
    }

    public RunStatus getInitialStatus() { return initialStatus; }
}
