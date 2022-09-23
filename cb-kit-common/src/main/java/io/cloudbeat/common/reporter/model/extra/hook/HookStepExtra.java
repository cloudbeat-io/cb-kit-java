package io.cloudbeat.common.reporter.model.extra.hook;

import io.cloudbeat.common.reporter.model.extra.IStepExtra;

public class HookStepExtra implements IStepExtra {
    public HookStepExtra() {
    }

    public HookStepExtra(final HookType type) {
        this.type = type;
    }
    private HookType type;
    private String subType;

    public HookType getType() {
        return type;
    }

    public void setType(HookType type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

}
