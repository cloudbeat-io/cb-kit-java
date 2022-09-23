package io.cloudbeat.cucumber.runner;

import cucumber.api.Scenario;
import cucumber.runtime.HookDefinition;
import cucumber.runtime.RuntimeGlue;
import cucumber.runtime.UndefinedStepsTracker;
import cucumber.runtime.xstream.LocalizedXStreams;
import gherkin.formatter.model.Tag;
import io.cloudbeat.common.reporter.model.StepResult;

import java.util.Collection;
import java.util.List;

class CbRuntimeGlue extends RuntimeGlue {
    public CbRuntimeGlue(UndefinedStepsTracker tracker, LocalizedXStreams localizedXStreams) {
        super(tracker, localizedXStreams);
    }

    @Override
    public void addBeforeHook(final HookDefinition hookDefinition) {
        super.addBeforeHook(wrapHook(hookDefinition, true));
    }

    @Override
    public void addAfterHook(HookDefinition hookDefinition) {
        super.addAfterHook(wrapHook(hookDefinition, false));
    }

    private HookDefinition wrapHook(final HookDefinition hookDefinition, final boolean before) {
        return new HookDefinitionWrapper(hookDefinition, before);
    }

    class HookDefinitionWrapper implements HookDefinition {
        final HookDefinition orgHook;
        final boolean isBefore;
        public HookDefinitionWrapper(HookDefinition orgHook, boolean isBefore) {
            this.orgHook = orgHook;
            this.isBefore = isBefore;
        }
        @Override
        public String getLocation(boolean detail) {
            return orgHook.getLocation(detail);
        }

        @Override
        public void execute(Scenario scenario) throws Throwable {
            StepResult hookResult = ReporterUtils.startHook(scenario, this, isBefore);
            if (hookResult == null) {
                orgHook.execute(scenario);
                return;
            }
            try {
                orgHook.execute(scenario);
                ReporterUtils.endHook(hookResult, null);
            } catch (Throwable e) {
                ReporterUtils.endHook(hookResult, e);
                throw e;
            }
        }

        @Override
        public boolean matches(Collection<Tag> tags) {
            return orgHook.matches(tags);
        }

        @Override
        public int getOrder() {
            return orgHook.getOrder();
        }

        @Override
        public boolean isScenarioScoped() {
            return orgHook.isScenarioScoped();
        }
    }
}
