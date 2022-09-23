package io.cloudbeat.cucumber.runner;

import cucumber.api.junit.Cucumber;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.HookDefinition;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.FeatureRunner;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Tag;
import io.cloudbeat.common.CbTestContext;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * CloudBeat runner for Cucumber v1 and JUnit 4.
 */

public class CbCucumberRunner extends Cucumber {
    private static final String RUNNER_NAME = "CbCucumberRunner";
    public CbCucumberRunner(Class clazz) throws InitializationError, IOException {
        super(clazz);
        startCbInstance();
    }

    private void startCbInstance() {
        final CbTestContext ctx = CbTestContext.getInstance();
        if (ctx.isActive() && !ctx.getReporter().isStarted()) {
            ctx.getReporter().setFramework("Cucumber", "1");
            ctx.getReporter().setRunnerName(RUNNER_NAME);
            ctx.getReporter().startInstance();
        }
    }

    public void run(RunNotifier notifier) {
        notifier.addListener(new CbJUnitRunListener());
        super.run(notifier);
    }

    @Override
    protected Runtime createRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        return new CbRuntime(resourceLoader, classFinder, classLoader, runtimeOptions);
        //return super.createRuntime(resourceLoader, classLoader, runtimeOptions);
    }

    @Override
    public List<FeatureRunner> getChildren() {
        return super.getChildren();
    }

    @Override
    protected boolean isIgnored(FeatureRunner child) {
        return super.isIgnored(child);
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        return super.withAfterClasses(statement);
    }


}