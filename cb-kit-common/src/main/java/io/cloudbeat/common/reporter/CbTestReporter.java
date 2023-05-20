package io.cloudbeat.common.reporter;

import io.cloudbeat.common.client.CbClientException;
import io.cloudbeat.common.client.CbHttpClient;
import io.cloudbeat.common.client.api.RuntimeApi;
import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.model.runtime.NewInstanceOptions;
import io.cloudbeat.common.model.runtime.NewRunOptions;
import io.cloudbeat.common.reporter.model.*;
import io.cloudbeat.common.reporter.model.extra.hook.HookType;
import io.cloudbeat.common.writer.ResultWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CbTestReporter {
    //private static final Logger LOGGER = LoggerFactory.getLogger(CbTestReporter.class);
    private final static String TEST_RESULTS_FILENAME = ".CB_TEST_RESULTS";
    private final String language = "java";

    private CbHttpClient cbClient;
    private Optional<RuntimeApi> runtimeApi;
    private CbConfig config;
    private Instance instance;
    private String frameworkName;
    private String frameworkVersion;
    private String runnerName;
    private TestResult result;
    private Optional<SuiteResult> startedSuite = Optional.empty();
    private Optional<CaseResult> startedCase = Optional.empty();
    private boolean alreadyTriedToStartInstance = false;
    private final Deque<StepResult> startedSteps = new ConcurrentLinkedDeque<>();

    public CbTestReporter(CbConfig config) {
        this.config = config;
        if (config != null && StringUtils.isNotEmpty(config.getApiToken())) {
            try {
                this.cbClient = new CbHttpClient(config.getApiToken(), config.getApiEndpointUrl());
            } catch (CbClientException e) {
                // ignore client initialization error - isStarted will be false in that case
                return;
            }
            runtimeApi = Optional.ofNullable(cbClient.getRuntimeApi());
        }
        else
            runtimeApi = Optional.empty();
    }

    public Optional<Instance> getInstance() {
        if (instance == null)
            return Optional.empty();
        return Optional.of(instance);
    }

    public boolean isStarted() {
        return config != null && /*this.cbClient != null &&*/ instance != null && result != null;
    }

    public void setFramework(final String frameworkName) {
        this.setFramework(frameworkName, null);
    }

    public void setFramework(final String frameworkName, final String frameworkVersion) {
        this.frameworkName = frameworkName;
        this.frameworkVersion = frameworkVersion;
        if (result != null && !result.getMetaData().containsKey("framework.name")) {
            result.addAttribute("framework.name", frameworkName);
            result.addAttribute("framework.version", frameworkVersion);
        }
    }

    public void setRunnerName(final String name) {
        this.runnerName = name;
    }

    public String getRunnerName() {
        return runnerName;
    }
    public SuiteResult getSuiteResult(final String fqn) {
        return result.lastSuite(fqn).orElse(null);
    }

    public TestResult getResult() {
        return  result;
    }

    public synchronized void startInstance() {
        // ignore this call if instance has been already initialized
        if (instance != null || alreadyTriedToStartInstance)
            return;
        // "alreadyTriedToStartInstance" helps us to ensure that we run the below code only once
        alreadyTriedToStartInstance = true;
        final String runId;
        final String instanceId;
        try {
            // create new run if run id was not specified, or update the existing run status
            if (config.getRunId() == null && runtimeApi.isPresent()) {
                runId = runtimeApi.get().newRun(
                        new NewRunOptions(config.getProjectId(), config.getRunGroup(), RunStatus.RUNNING));
            }
            else if (config.getRunId() != null) {
                runId = config.getRunId();
                if (runtimeApi.isPresent())
                    runtimeApi.get().updateRunStatus(runId, RunStatus.RUNNING);
            }
            else
                runId = null;
            // create new instance if instance id was not specified, or update the existing instance status
            if (config.getInstanceId() == null && runtimeApi.isPresent() && runId != null) {
                instanceId = runtimeApi.get().newInstance(runId, new NewInstanceOptions(RunStatus.RUNNING)); //, config.getMetadata());
            }
            else if (config.getInstanceId() != null) {
                instanceId = config.getInstanceId();
                if (runtimeApi.isPresent())
                    runtimeApi.get().updateInstanceStatus(runId, instanceId, RunStatus.RUNNING);
            }
            else
                instanceId = null;
        } catch (CbClientException e) {
            return;
        }
        // create instance object
        instance = new Instance(runId, instanceId, config.getCapabilities());
        instance.setStatus(RunStatus.RUNNING);
        result = new TestResult(instance.getRunId(), instance.getId(), config.getCapabilities(), config.getOptions(), config.getMetadata(), config.getEnvironmentVariables());
        // add system attributes (e.g. agent information)
        addSystemAttributes();
        // add framework details, if provided by framework implementation
        if (frameworkName != null)
            result.addAttribute("framework.name", frameworkName);
        if (frameworkVersion != null)
            result.addAttribute("framework.version", frameworkVersion);
    }

    public synchronized void endInstance() {
        if (instance == null)
            return;
        result.end();
        instance.setStatus(RunStatus.FINISHED);
        try {
            if (config.isRunningInCb()) {
                if (runtimeApi.isPresent())
                    runtimeApi.get().updateInstanceStatus(instance.getRunId(), instance.getId(), RunStatus.FINISHED);
                ResultWriter.writeResult(result, TEST_RESULTS_FILENAME);
            }
            else {
                ResultWriter.writeResult(result, TEST_RESULTS_FILENAME);
                if (runtimeApi.isPresent())
                    runtimeApi.get().endInstance(instance.getRunId(), instance.getId(), result);
            }
        } catch (CbClientException e) {}
        instance = null;
    }

    public void startSuite(final String name, final String fqn) {
        // do not add the same suite twice
        if (Objects.nonNull(fqn) && result.lastSuite(fqn).isPresent())
            return;
        final SuiteResult suite = result.addNewSuite(name);
        suite.setFqn(fqn);
        startedSuite = Optional.of(suite);
    }

    public void endSuite(String fqn) {
        result.lastSuite(fqn).ifPresent(suite -> {
           suite.end();
           if (startedSuite.isPresent() && startedSuite.get().getFqn() != null && startedSuite.get().getFqn().equals(fqn))
               startedSuite = Optional.empty();
        });
    }

    public void endLastSuite() {
        startedSuite.ifPresent(suite -> {
            suite.end();
        });
        startedSuite = Optional.empty();
    }

    public CaseResult startCase(final String name, final String fqn, final String suiteFqn) throws Exception {
        SuiteResult suiteResult = result.lastSuite(suiteFqn).orElseThrow(
                () -> new Exception("No started suite was found. You must call startSuite first.")
        );
        // do not add the same case twice
        //if (Objects.nonNull(fqn) && suiteResult.lastCase(fqn).isPresent())
        //    return;
        CaseResult caseResult = suiteResult.addNewCaseResult(name);
        caseResult.setFqn(fqn);
        startedCase = Optional.of(caseResult);
        return caseResult;
    }

    public void endCase(final String caseFqn) throws Exception {
        endCase(caseFqn, null, null);
    }
    public void endCase(final String caseFqn, final Throwable throwable) throws Exception {
        endCase(caseFqn, null, throwable);
    }
    public void endCase(final String caseFqn, final TestStatus status, final Throwable throwable) throws Exception {
        if (!startedCase.isPresent() || startedCase.get().getFqn() == null)
            return;
        if (!startedCase.get().getFqn().equals(caseFqn))
            throw new Exception("Cannot find started case: " + caseFqn);

        endStartedSteps(status, throwable);
        startedCase.get().end(status, throwable);
        startedCase = Optional.empty();
    }

    private void endStartedSteps(TestStatus status, Throwable throwable) {
        // if the case is ended due to error, there might be open steps that need to be closed
        while (!startedSteps.isEmpty()) {
            StepResult stepToEnd = startedSteps.pop();
            // end current and all the parent steps
            do {
                if (stepToEnd.getEndTime() == null)
                    stepToEnd.end(status, throwable);
                stepToEnd = stepToEnd.getParentStep();
                if (startedSteps.contains(stepToEnd))
                    startedSteps.remove(stepToEnd);
            } while (stepToEnd != null);
        }
    }

    public void passCase(final String caseFqn) throws Exception {
        endCase(caseFqn, TestStatus.PASSED, null);
    }

    public void failCase(final String caseFqn, Throwable exception) throws Exception {
        endCase(caseFqn, TestStatus.FAILED, exception);
    }

    public void skipCase(final String caseFqn) throws Exception {
        endCase(caseFqn, TestStatus.SKIPPED, null);
    }

    public StepResult startStep(final String name) {
        return startStep(name, StepType.GENERAL, null, null);
    }

    public StepResult startStep(final String name, final StepType type) {
        return startStep(name, type, null, null);
    }

    public StepResult startStep(
            @Nonnull final String name,
            @Nonnull final StepType type,
            @Nullable final String fqn,
            @Nullable final List<String> args
    ) {
        StepResult newStep;
        if (!startedSteps.isEmpty())
            startedSteps.push(newStep = startedSteps.peek().addNewSubStep(name, type));
        else if (startedCase.isPresent()) {
            startedSteps.push(newStep = startedCase.get().addNewStep(name, type));
        }
        else    // we are not supposed to call startStep if no case or parent step was started before
            return null;
        if (fqn != null)
            newStep.setFqn(fqn);
        if (args != null)
            newStep.setArgs(args);
        return newStep;
    }

    public void endLastStep() {
        if (!startedSteps.isEmpty()) {
            endStep(startedSteps.peek().getId(), null, null);
        }
    }

    public void passLastStep() {
        if (!startedSteps.isEmpty()) {
            endStep(startedSteps.peek().getId(), TestStatus.PASSED, null);
        }
    }

    public void passStep(final String stepId) {
        passStep(stepId, null);
    }

    public void passStep(final String stepId, Map<String, Number> stats) {
        passStep(stepId, stats, null);
    }

    public void passStep(final String stepId, Map<String, Number> stats, List<LogMessage> logs) {
        final StepResult step = endStep(stepId, TestStatus.PASSED, null);
        if (stats != null)
            step.addStats(stats);
        if (logs != null)
            step.addLogs(logs);
    }

    public void failStep(final String name, Throwable throwable) {
        failStep(name, throwable, null);
    }

    public void failStep(final String stepId, Throwable throwable, String screenshot) {
        endStep(stepId, TestStatus.FAILED, throwable, screenshot);
    }

    public void failStep(final StepResult stepResult, Throwable throwable, String screenshot) {
        endStep(stepResult, TestStatus.FAILED, throwable, screenshot);
    }

    public StepResult endStep(final String stepId) {
        return endStep(stepId, TestStatus.PASSED, null);
    }
    public StepResult endStep(final String stepId, TestStatus status, Throwable throwable) {
        return endStep(stepId, status, throwable, null);
    }

    public StepResult endStep(final String stepId, TestStatus status, Throwable throwable, String screenshot) {
        // see if step with specified id is found in started steps list
        Optional<StepResult> matchedStep = startedSteps.stream()
                .filter(
                    stepResult -> stepResult.getId().equals(stepId)
                    || (stepResult.getFqn() != null && stepResult.getFqn().equals(stepId)))
                .findFirst();
        if (!matchedStep.isPresent())
            return null;
        return endStep(matchedStep.get(), status, throwable, screenshot);
    }

    public StepResult endStep(final StepResult stepResult, TestStatus status, Throwable throwable, String screenshot) {
        if (startedSteps.isEmpty() || !startedSteps.contains(stepResult))
            return null;
        // if the step to be ended is not the last one that has been started
        // then end all the steps in the stack that were started after the specified step (e.g. stepResult)
        while (startedSteps.peek() != stepResult) {
            StepResult stepToBeEnded = startedSteps.pop();
            if (stepToBeEnded.getEndTime() != null)
                stepToBeEnded.end();
        }
        // pop the step out from the stack
        startedSteps.pop();
        /*LinkedList<StepResult> stepStack = new LinkedList<>();
        StepResult currentStep = lastStep;
        boolean stepFound = false;
        while (currentStep != null) {
            stepStack.push(currentStep);
            if ((currentStep.getId() != null && currentStep.getId().equals(stepId))
                || (currentStep.getFqn() != null && currentStep.getFqn().equals(stepId))
            ) {
                stepFound = true;
                break;
            }
            currentStep = currentStep.getParentStep();
        }
        if (!stepFound)
            return null;
        final StepResult endedStep = stepStack.pop();*/
        stepResult.end(status, throwable, null, screenshot);
        //lastStep = endedStep.getParentStep();
        // make sure to end all children/parent steps, if they remain open
        /*stepStack.stream().forEach((step) -> {
            if (step.getEndTime() == 0)
                step.end(status, throwable);
        });*/
        return stepResult;
    }

    public void step(final String name, Runnable stepFunc) {
        step(name, stepFunc, false);
    }

    public void step(final String name, Runnable stepFunc, boolean continueOnError) {
        final StepResult step = startStep(name);
        final String stepId = step.getId();
        try {
            stepFunc.run();
            endStep(step, null, null, null);
        }
        catch (Throwable e) {
            failStep(stepId, e);
            if (!continueOnError)
                throw e;
        }
    }
    public StepResult startCaseHook(final String name, final boolean isBefore) {
        if (!startedCase.isPresent())
            return null;
        StepResult newHookStep;
        startedSteps.push(newHookStep = startedCase.get().addNewHook(name, isBefore ? HookType.BEFORE : HookType.AFTER));
        return newHookStep;
    }
    public void endCaseHook(final StepResult hookResult, final Throwable throwable) {
        endStep(hookResult, null, throwable, null);
    }
    public StepResult startSuiteHook(final String name, final String fqn, final boolean isBefore) {
        if (!startedSuite.isPresent())
            return null;
        StepResult newHookStep;
        startedSteps.push(newHookStep = startedSuite.get().addNewHook(name, isBefore ? HookType.BEFORE : HookType.AFTER));
        newHookStep.setFqn(fqn);
        return newHookStep;
    }
    public StepResult endSuiteHook(final String fqn) {
        return endStep(fqn);
    }
    public StepResult beforeCaseHook(final String name, final TestStatus status) {
        if (startedCase.isPresent()) {
            final StepResult hookResult = startedCase.get().addNewHook(name, HookType.BEFORE);
            hookResult.end(status);
            return hookResult;
        }
        return null;
    }
    public StepResult afterCaseHook(final String name, final TestStatus status) {
        if (startedCase.isPresent()) {
            final StepResult hookResult = startedCase.get().addNewHook(name, HookType.AFTER);
            hookResult.end(status);
            return hookResult;
        }
        return null;
    }
    public void beforeAllHook(final String name, final Runnable func, final boolean continueOnError) {
        if (!isStarted() || getResult() == null) {
            func.run();
            return;
        }
        final StepResult hookResult = getResult().addNewHook(name, HookType.BEFORE);

        try {
            func.run();
            hookResult.end();
        }
        catch (Throwable e) {
            hookResult.end(e);
            if (!continueOnError)
                throw e;
        }
    }

    public void afterAllHook(final String name, final Runnable func, final boolean continueOnError) {
        if (!isStarted() || getResult() == null) {
            func.run();
            return;
        }
        final StepResult hookResult = getResult().addNewHook(name, HookType.AFTER);

        try {
            func.run();
            hookResult.end();
        }
        catch (Throwable e) {
            hookResult.end(e);
            if (!continueOnError)
                throw e;
        }
    }

    public void logMessage(LogMessage logMessage) {
        if (logMessage == null)
            return;
        // define were to add the log: current step, current case or current suite
        if (!startedSteps.isEmpty())
            startedSteps.peek().addLogMessage(logMessage);
        else if (startedCase.isPresent())
            startedCase.get().addLogMessage(logMessage);
        else if (startedSuite.isPresent())
            startedSuite.get().addLogMessage(logMessage);
    }

    public void logInfo(final String message) {

    }

    public void logWarning(final String message) {

    }

    public void logError(final String message) {
        logError(message, null);
    }
    public void logError(Throwable error) {
        logError(null, error);
    }

    public void logError(final String message, Throwable error) {
        final LogMessage logMessage = new LogMessage();
        logMessage.setMessage(message);
        if (error != null)
            logMessage.setFailure(new FailureResult(error));
        logMessage.setLevel(LogLevel.ERROR.value());
        logMessage(logMessage);
    }

    public void addGlobalError(final Throwable error) {
        if (!isStarted())
            return;
        result.addFailure(new FailureResult(error));
    }

    public boolean hasSuites() {
        return result != null && result.getSuites().size() > 0;
    }

    /*
    private void loadConfig() {
        String payloadpath = System.getProperty("payloadpath");;
        String testmonitorUrl = System.getProperty("testmonitorurl");
        testMonitorToken = System.getProperty("testmonitortoken");

        try {
            if (payloadpath != null && testmonitorUrl != null && testMonitorToken != null) {
                testMonitorStatusUrl = testmonitorUrl + "/status";
                payload = CbConfig.load(payloadpath);
            }
            else {
                logInfo("Plugin will be disabled. One of payloadpath, testmonitorurl, or testmonitortoken parameters is missing.");
            }
        }
        catch (IOException e) {
            logError("Unable to load CloudBeat configuration settings.", e);
            //LOGGER.error("Unable to load CloudBeat configuration settings.", e);
        }
        // TODO: make sure we throw an exception or handle in some other way the situation
        // where no configuration parameters where provided (e.g. when user runs test outside CB environment)
    }*/

    private void addSystemAttributes() {
        result.addAttribute("agent.hostname", getHostName());
        result.addAttribute("agent.java.name", SystemUtils.JAVA_RUNTIME_NAME);
        result.addAttribute("agent.java.version", SystemUtils.JAVA_VERSION);
        result.addAttribute("agent.os.name", SystemUtils.OS_NAME);
        result.addAttribute("agent.os.version", SystemUtils.OS_VERSION);
        result.addAttribute("agent.user.name", SystemUtils.USER_NAME);
        result.addAttribute("agent.user.home", SystemUtils.USER_HOME);
        result.addAttribute("agent.user.timezone", SystemUtils.USER_TIMEZONE);
    }
    private static String getHostName() {
        try {
            return Optional.ofNullable(SystemUtils.getHostName()).orElse(InetAddress.getLocalHost().getHostName());
        }
        catch (UnknownHostException e) {
            return null;
        }
    }

    public StepResult getLastStep() {
        return startedSteps.peek();
    }

    public CaseResult getStartedCase() {
        return startedCase.orElse(null);
    }

}
