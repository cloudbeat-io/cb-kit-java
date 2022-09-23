package io.cloudbeat.cucumber.runner;

import cucumber.runtime.*;
import cucumber.runtime.Runtime;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.xstream.LocalizedXStreams;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Tag;

import java.util.*;

public class CbRuntime extends Runtime {
    private final RuntimeOptions runtimeOptions;
    private final StopWatch stopWatch;
    private ScenarioImpl scenarioResult;
    private boolean skipNextStep;
    public CbRuntime(ResourceLoader resourceLoader, ClassFinder classFinder, ClassLoader classLoader, RuntimeOptions runtimeOptions) {
        this(resourceLoader, classLoader, loadBackends(resourceLoader, classFinder), runtimeOptions);
    }

    public CbRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions) {
        //this(resourceLoader, classLoader, backends, runtimeOptions, StopWatch.SYSTEM, (RuntimeGlue)null);
        this(resourceLoader, classLoader, backends, runtimeOptions, StopWatch.SYSTEM,
            new CbRuntimeGlue(new UndefinedStepsTracker(), new LocalizedXStreams(classLoader)));
    }

    public CbRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions, RuntimeGlue optionalGlue) {
        this(resourceLoader, classLoader, backends, runtimeOptions, StopWatch.SYSTEM, optionalGlue);
    }

    public CbRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions, StopWatch stopWatch, RuntimeGlue optionalGlue) {
        super(resourceLoader, classLoader, backends, runtimeOptions, stopWatch, optionalGlue);
        this.stopWatch = stopWatch;
        this.runtimeOptions = runtimeOptions;
    }

    @Override
    public void runBeforeHooks(Reporter reporter, Set<Tag> tags) {
        //this.glue.getBeforeHooks()
        //reporter.
        super.runBeforeHooks(reporter, tags);
    }

    private static Collection<? extends Backend> loadBackends(ResourceLoader resourceLoader, ClassFinder classFinder) {
        Reflections reflections = new Reflections(classFinder);
        return reflections.instantiateSubclasses(Backend.class, "cucumber.runtime", new Class[]{ResourceLoader.class}, new Object[]{resourceLoader});
    }
}
