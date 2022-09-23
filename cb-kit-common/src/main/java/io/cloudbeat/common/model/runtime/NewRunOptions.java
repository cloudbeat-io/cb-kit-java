package io.cloudbeat.common.model.runtime;

import io.cloudbeat.common.reporter.model.RunStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NewRunOptions {
    public NewRunOptions(final String projectId) {
        this.projectId = projectId;
    }
    public NewRunOptions(final String projectId, @Nullable final String runGroup, @Nullable final RunStatus initialStatus) {
        this(projectId);
        this.runGroup = runGroup;
        this.initialStatus = initialStatus;
    }
    @Nullable
    RunStatus initialStatus;
    @Nonnull
    String projectId;
    @Nullable
    String runGroup;

    public String getProjectId() {
        return projectId;
    }
    public String getRunGroup() {
        return runGroup;
    }
    public RunStatus getInitialStatus() {
        return initialStatus;
    }
}
