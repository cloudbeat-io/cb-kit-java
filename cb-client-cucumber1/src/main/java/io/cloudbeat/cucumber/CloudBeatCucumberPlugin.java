/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.cloudbeat.cucumber;

import cucumber.runtime.StepDefinitionMatch;
import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import io.cloudbeat.common.CbTestContext;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CloudBeat reporter plugin for Cucumber v1.
 */
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity",
        "ClassDataAbstractionCoupling",
        "unused"
})
public class CloudBeatCucumberPlugin implements Reporter, Formatter {
    private String currentFeatureUri;
    private Feature currentFeature;
    private Scenario currentScenario;
    private ScenarioOutline currentOutline;
    private Examples currentExamples;
    private Integer examplesTableRowIndex;
    private Match currentMatch = null;
    private final boolean isActive;

    @SuppressWarnings("unused")
    public CloudBeatCucumberPlugin() {
        isActive = CbTestContext.getInstance().isActive()
                && CbTestContext.getInstance().getReporter() != null
                && CbTestContext.getInstance().getReporter().isStarted();
    }

    @Override
    public void uri(final String uri) {
        currentFeatureUri = uri;
    }

    // On feature start
    @Override
    public void feature(final Feature feature) {
        if (!isActive)
            return;
        currentFeature = feature;
        CucumberUtils.startSuite(currentFeatureUri, feature);
    }
    // On feature end
    @Override
    public void eof() {
        if (!isActive)
            return;
        if (currentExamples != null) {
            CucumberUtils.endLastStep();
            currentExamples = null;
        }
        if (currentOutline != null) {
            CucumberUtils.endCase(currentOutline);
            currentOutline = null;
        }
        if (currentFeature != null && currentFeatureUri != null) {
            CucumberUtils.endSuite(currentFeatureUri);
            currentFeature = null;
            currentFeatureUri = null;
            currentOutline = null;
            currentScenario = null;
            currentMatch = null;
            currentExamples = null;
        }
    }
    // On hook start
    @Override
    public void before(final Match match, final Result result) {
        if (!isActive)
            return;
        if (currentScenario != null && currentMatch == null)
            CucumberUtils.beforeScenarioHook(match, result);
        //new StepUtils(currentFeature, currentScenario).fireFixtureStep(match, result, true);
    }
    // On hook end
    @Override
    public void after(final Match match, final Result result) {
        if (!isActive)
            return;
        if (currentScenario != null && currentMatch == null)
            CucumberUtils.afterScenarioHook(match, result);
        //new StepUtils(currentFeature, currentScenario).fireFixtureStep(match, result, false);
    }
    // On Scenario Outline start
    @Override
    public void scenarioOutline(final ScenarioOutline so) {
        if (!isActive)
            return;
        if (currentOutline != null)
            CucumberUtils.endCase(currentOutline);
        currentOutline = so;
        CucumberUtils.startCase(currentFeatureUri, so);
    }

    @Override
    public void examples(final Examples examples) {
        if (!isActive)
            return;
        // end previously started Examples step
        if (currentExamples != null)
            CucumberUtils.endLastStep();
        currentExamples = examples;
        examplesTableRowIndex = null;
        CucumberUtils.startStep(examples);
    }

    @Override
    public void startOfScenarioLifeCycle(final Scenario scenario) {
        if (!isActive)
            return;
        this.currentScenario = scenario;
        final boolean isOutline = currentOutline != null && scenario.getId().startsWith(currentOutline.getId());
        if (!isOutline) {
            // end previously started Scenario Outline case
            if (currentOutline != null) {
                CucumberUtils.endCase(currentOutline);
                currentOutline = null;
            }
            CucumberUtils.startCase(currentFeatureUri, scenario);
        }
        else if (currentExamples != null) {
            // increase or set current row index to identify the last row in endOfScenarioLifeCycle
            if (examplesTableRowIndex == null)
                examplesTableRowIndex = 1;  // data row index starts from 1, as 0 is headers
            else
                examplesTableRowIndex++;
            // add example row as a container step
            if (scenario.getId().startsWith(currentExamples.getId())) {
                // find current row
                Optional<ExamplesTableRow> row = currentExamples
                    .getRows().stream()
                    .filter(r -> r.getId().equals(scenario.getId()))
                    .findFirst();
                if (row.isPresent())
                    CucumberUtils.startStep(currentExamples, row.get());
            }
        }
        //final Deque<Tag> tags = new LinkedList<>(scenario.getTags());
        //tags.addAll(currentFeature.getTags());
    }

    @Override
    public void endOfScenarioLifeCycle(final Scenario scenario) {
        if (!isActive)
            return;
        final boolean isOutline = currentOutline != null && scenario.getId().startsWith(currentOutline.getId());
        if (!isOutline) {
            CucumberUtils.endCase(scenario.getId());
        }
        else if (currentExamples != null) {
            // end example row step
            if (scenario.getId().startsWith(currentExamples.getId())) {
                CucumberUtils.endLastStep();
                // end Examples step if all rows in the example table have been reported
                if (examplesTableRowIndex >= currentExamples.getRows().size() - 1) {
                    CucumberUtils.endLastStep();
                    examplesTableRowIndex = null;
                }
            }
        }
    }

    @Override
    public void background(final Background b) {
    }

    @Override
    public void scenario(final Scenario scenario) {
    }

    @Override
    public void step(final Step step) {
    }

    @Override
    public void match(final Match match) {
        if (!isActive)
            return;
        currentMatch = match;
        if (match instanceof StepDefinitionMatch) {
            CucumberUtils.startStep((StepDefinitionMatch) match);
        }
    }

    @SuppressWarnings("PMD.NcssCount")
    @Override
    public void result(final Result result) {
        if (!isActive)
            return;
        if (currentMatch != null && currentMatch instanceof StepDefinitionMatch) {
            CucumberUtils.endStep((StepDefinitionMatch) currentMatch, result);
            currentMatch = null;
        }
    }

    @Override
    public void embedding(final String string, final byte[] bytes) {
    }

    @Override
    public void write(final String string) {
    }

    @Override
    public void syntaxError(final String state, final String event,
                            final List<String> legalEvents, final String uri, final Integer line) {
    }

    @Override
    public void done() {
    }

    @Override
    public void close() {
        if (!isActive)
            return;
        CbTestContext.getInstance().getReporter().endInstance();
    }

}