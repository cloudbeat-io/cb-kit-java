package io.cloudbeat.common.reporter;

import io.cloudbeat.common.client.CbClientException;
import io.cloudbeat.common.client.CbApiHttpClient;
import io.cloudbeat.common.client.CbGatewayHttpClient;
import io.cloudbeat.common.client.api.GatewayApi;
import io.cloudbeat.common.client.api.RuntimeApi;
import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.helper.AttachmentHelper;
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
import java.util.function.Consumer;

public class CbTestReporter {
    //private static final Logger LOGGER = LoggerFactory.getLogger(CbTestReporter.class);
    private final static String TEST_RESULTS_FILENAME = ".CB_TEST_RESULTS.json";
    private final String language = "java";

    private CbApiHttpClient cbApiClient;
    private CbGatewayHttpClient cbGatewayClient;
    private Optional<RuntimeApi> runtimeApi = Optional.empty();
    private Optional<GatewayApi> gatewayApi = Optional.empty();
    private CbConfig config;
    private Instance instance;
    private String frameworkName;
    private String frameworkVersion;
    private String runnerName;
    private TestResult result;
    private boolean alreadyTriedToStartInstance = false;
    // local thread safe
    private final ThreadLocal<CaseResult> lastCaseResult = new ThreadLocal();
    private final ThreadLocal<SuiteResult> lastSuiteResult = new ThreadLocal();
    private final ThreadLocal<Deque<StepResult>> startedStepsQueue = ThreadLocal.withInitial(ConcurrentLinkedDeque::new);
    private final ThreadLocal<String> lastScreenshotOnException = new ThreadLocal();

    public CbTestReporter(CbConfig config) {
        this.config = config;
        // if the test is running on user's server, then use the standard CB API
        if (config != null && StringUtils.isNotEmpty(config.getApiToken())) {
            try {
                this.cbApiClient = new CbApiHttpClient(config.getApiToken(), config.getApiEndpointUrl());
            } catch (CbClientException e) {
                // ignore client initialization error - isStarted will be false in that case
                return;
            }
            runtimeApi = Optional.ofNullable(cbApiClient.getRuntimeApi());
        }
        // if the test is running in CB runner, then use Gateway API
        if (config != null && StringUtils.isNotEmpty(config.getGatewayToken())) {
            try {
                this.cbGatewayClient = new CbGatewayHttpClient(config.getGatewayUrl(), config.getGatewayToken());
            }
            catch (Throwable e) {   // CbClientException
                // ignore client initialization error - isStarted will be false in that case
                System.out.println(e.toString());
                return;
            }
            gatewayApi = Optional.ofNullable(cbGatewayClient.getGatewayApi());
        }
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

    public SuiteResult startSuite(final String name, final String fqn) {
        final SuiteResult newSuite = result.addNewSuite(name);
        newSuite.setFqn(fqn);
        // make sure we clean up previous case and started steps
        lastCaseResult.remove();
        startedStepsQueue.remove();
        lastScreenshotOnException.remove();
        lastSuiteResult.set(newSuite);
        return newSuite;
    }

    public SuiteResult startSuite(final String name, final Consumer<SuiteResult> updateFunc) {
        final SuiteResult newSuite = result.addNewSuite(name);
        // make sure we clean up previous case and started steps
        lastCaseResult.remove();
        startedStepsQueue.remove();
        lastSuiteResult.set(newSuite);
        updateFunc.accept(newSuite);
        return newSuite;
    }

    public SuiteResult endSuite(String fqn) {
        final SuiteResult startedSuite;
        if (lastSuiteResult.get() != null)
            startedSuite = lastSuiteResult.get();
        else
            startedSuite = result.getSuites().stream()
                .filter(
                    suiteResult -> suiteResult.getFqn().equals(fqn)
                ).findFirst().orElse(null);

        if (startedSuite == null || !startedSuite.getFqn().equals(fqn))
            return null;
        startedSuite.end();
        return startedSuite;
    }

    public SuiteResult endStartedSuite() {
        if (lastSuiteResult.get() == null)
            return null;
        SuiteResult startedSuite = lastSuiteResult.get();
        startedSuite.end();
        return startedSuite;
    }

    public CaseResult startCase(final String name, final String fqn) {
        SuiteResult startedSuite = lastSuiteResult.get();
        if (startedSuite == null) {
            if (result.getSuites().size() > 0)
                startedSuite = result.getSuites().get(0);
            else
                return  null;
        }
        CaseResult newCase = startedSuite.addNewCaseResult(name);
        newCase.setFqn(fqn);
        startedStepsQueue.remove();
        lastCaseResult.set(newCase);
        reportCaseStatus(newCase, Optional.empty(), null);

        return newCase;
    }

    public void endCase(final String caseFqn) {
        endCase(caseFqn, null, null);
    }
    public void endCase(final String caseFqn, final Throwable throwable) {
        endCase(caseFqn, null, throwable);
    }
    public CaseResult endCase(final String caseFqn, final TestStatus status, final Throwable throwable) {
        final CaseResult startedCase;
        if (lastCaseResult.get() != null)
            startedCase = lastCaseResult.get();
        else if (lastSuiteResult.get() != null)
            startedCase = lastSuiteResult.get().getCases().stream()
                    .filter(
                            caseResult -> caseResult.getFqn().equals(caseFqn)
                    ).findFirst().orElse(null);
        else
            return null;

        if (startedCase == null || !startedCase.getFqn().equals(caseFqn))
            return null;

        // endCase might be called twice for the same test case result,
        // so make sure we do not update the status and report it back to CB twice
        if (startedCase.getStatus() == null) {
            endStartedSteps(status, throwable);
            startedCase.end(status, throwable);
            reportCaseStatus(startedCase, Optional.of(startedCase.getStatus()), throwable);
        }

        return startedCase;
    }

    private void reportCaseStatus(CaseResult startedCase, Optional<TestStatus> status, Throwable throwable) {
        if (!config.isRunningInCb())
            return;
        try {
            if (this.gatewayApi.isPresent())
                this.gatewayApi.get().updateTestCaseStatus(
                        result.getRunId(),
                        result.getInstanceId(),
                        startedCase.getId(),
                        startedCase.getFqn(),
                        startedCase.getName(),
                        status,
                        startedCase.getFailure());
            else if (this.runtimeApi.isPresent())
                this.runtimeApi.get().updateTestCaseStatus(
                        result.getRunId(),
                        result.getInstanceId(),
                        startedCase.getId(),
                        startedCase.getFqn(),
                        startedCase.getName(),
                        status,
                        startedCase.getFailure());
        } catch (CbClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void endStartedSteps(TestStatus status, Throwable throwable) {
        if (startedStepsQueue.get() == null || startedStepsQueue.get().isEmpty())
            return;
        // if the case is ended due to error, there might be open steps that need to be closed
        while (!startedStepsQueue.get().isEmpty()) {
            StepResult stepToEnd = startedStepsQueue.get().pop();
            // end current and all the parent steps
            do {
                if (stepToEnd.getEndTime() == null)
                    stepToEnd.end(status, throwable);
                stepToEnd = stepToEnd.getParentStep();
                if (startedStepsQueue.get().contains(stepToEnd))
                    startedStepsQueue.get().remove(stepToEnd);
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
        if (!startedStepsQueue.get().isEmpty())
            startedStepsQueue.get().push(newStep = startedStepsQueue.get().peek().addNewSubStep(name, type));
        else if (lastCaseResult.get() != null) {
            startedStepsQueue.get().push(newStep = lastCaseResult.get().addNewStep(name, type));
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
        if (!startedStepsQueue.get().isEmpty()) {
            endStep(startedStepsQueue.get().peek(), null, null, null);
        }
    }

    public void passLastStep() {
        if (!startedStepsQueue.get().isEmpty()) {
            endStep(startedStepsQueue.get().peek().getId(), TestStatus.PASSED, null);
        }
    }

    public void failLastStep(Throwable throwable) {
        failLastStep(throwable, null);
    }

    public void failLastStep(final Throwable throwable, final String screenshot) {
        if (!startedStepsQueue.get().isEmpty()) {
            endStep(startedStepsQueue.get().peek().getId(), TestStatus.FAILED, throwable, screenshot);
        }
    }

    public StepResult passStep(final String stepId) {
        return passStep(stepId, null);
    }

    public StepResult passStep(final String stepId, Map<String, Number> stats) {
        return passStep(stepId, stats, null);
    }

    public StepResult passStep(final String stepId, Map<String, Number> stats, List<LogMessage> logs) {
        final StepResult step = endStep(stepId, TestStatus.PASSED, null);
        if (stats != null)
            step.addStats(stats);
        if (logs != null)
            step.addLogs(logs);
        return step;
    }

    public StepResult failStep(final String name, Throwable throwable) {
        return failStep(name, throwable, null);
    }

    public StepResult failStep(final String stepId, Throwable throwable, String screenshot) {
        return endStep(stepId, TestStatus.FAILED, throwable, screenshot);
    }

    public StepResult failStep(final StepResult stepResult, Throwable throwable, String screenshot) {
        return endStep(stepResult, TestStatus.FAILED, throwable, screenshot);
    }

    public StepResult endStep(final String stepId) {
        return endStep(stepId, TestStatus.PASSED, null);
    }
    public StepResult endStep(final String stepId, TestStatus status, Throwable throwable) {
        return endStep(stepId, status, throwable, null);
    }

    public StepResult endStep(final String stepId, TestStatus status, Throwable throwable, String screenshot) {
        // see if step with specified id is found in started steps list
        Optional<StepResult> matchedStep = startedStepsQueue.get().stream()
                .filter(
                    stepResult -> stepResult.getId().equals(stepId)
                    || (stepResult.getFqn() != null && stepResult.getFqn().equals(stepId)))
                .findFirst();
        if (!matchedStep.isPresent())
            return null;
        return endStep(matchedStep.get(), status, throwable, screenshot);
    }

    public StepResult endStep(final StepResult stepResult,
                              final TestStatus status,
                              final Throwable throwable,
                              final String screenshot) {
        if (startedStepsQueue.get().isEmpty() || !startedStepsQueue.get().contains(stepResult))
            return null;
        // if the step to be ended is not the last one that has been started
        // then end all the steps in the stack that were started after the specified step (e.g. stepResult)
        while (startedStepsQueue.get().peek() != stepResult) {
            StepResult stepToBeEnded = startedStepsQueue.get().pop();
            if (stepToBeEnded.getEndTime() != null)
                stepToBeEnded.end();
        }
        // pop the step out from the stack
        startedStepsQueue.get().pop();
        if (screenshot == null
                && lastScreenshotOnException.get() != null
                && throwable != null) {
            stepResult.end(status, throwable, null, lastScreenshotOnException.get());
            lastScreenshotOnException.remove();
        }
        else
            stepResult.end(status, throwable, null, screenshot);

        return stepResult;
    }

    public void step(final String name, Runnable stepFunc) {
        step(name, stepFunc, false);
    }

    public void step(final String name, Runnable stepFunc, boolean continueOnError) {
        final StepResult step = startStep(name);
        // we will get step == null if test class is not extended with CbJUnitExtension
        if (step == null) {
            stepFunc.run();
            return;
        }
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
    public StepResult startCaseHook(final String name, final String fqn, final boolean isBefore) {
        if (lastCaseResult.get() == null)
            return null;
        StepResult newHookStep;
        startedStepsQueue.get().push(newHookStep = lastCaseResult.get().addNewHook(name, isBefore ? HookType.BEFORE : HookType.AFTER));
        newHookStep.setFqn(fqn);
        return newHookStep;
    }
    public StepResult endCaseHook(final StepResult hookResult, final Throwable throwable) {
        return endStep(hookResult, null, throwable, null);
    }
    public StepResult endCaseHook(final String hookFqn, final Throwable throwable) {
        return endStep(hookFqn, null, throwable, null);
    }
    public StepResult startSuiteHook(final String name, final String fqn, final boolean isBefore) {
        if (lastSuiteResult.get() == null)
            return null;
        StepResult newHookStep;
        startedStepsQueue.get().push(newHookStep = lastSuiteResult.get().addNewHook(name, isBefore ? HookType.BEFORE : HookType.AFTER));
        newHookStep.setFqn(fqn);
        return newHookStep;
    }
    public StepResult endSuiteHook(final String fqn) {
        return endSuiteHook(fqn, TestStatus.PASSED, null);
    }

    public StepResult endSuiteHook(final String fqn, TestStatus status, Throwable throwable) {
        return endStep(fqn, status, throwable);
    }

    public StepResult beforeCaseHook(final String name, final TestStatus status) {
        if (lastCaseResult.get() != null) {
            final StepResult hookResult = lastCaseResult.get().addNewHook(name, HookType.BEFORE);
            hookResult.end(status);
            return hookResult;
        }
        return null;
    }
    public StepResult afterCaseHook(final String name, final TestStatus status) {
        if (lastCaseResult.get() != null) {
            final StepResult hookResult = lastCaseResult.get().addNewHook(name, HookType.AFTER);
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
        if (!startedStepsQueue.get().isEmpty())
            startedStepsQueue.get().peek().addLogMessage(logMessage);
        else if (lastCaseResult.get() != null)
            lastCaseResult.get().addLogMessage(logMessage);
        else if (lastSuiteResult.get() != null)
            lastSuiteResult.get().addLogMessage(logMessage);
    }

    public void setScreenshotOnException(String base64Data) {
        this.lastScreenshotOnException.set(base64Data);
    }

    public void logInfo(final String message) {
        final LogMessage logMessage = new LogMessage();
        logMessage.setSrc(LogSource.USER);
        logMessage.setLevel(LogLevel.INFO);
        logMessage.setMessage(message);
        logMessage(logMessage);
    }

    public void logWarning(final String message) {
        final LogMessage logMessage = new LogMessage();
        logMessage.setSrc(LogSource.USER);
        logMessage.setLevel(LogLevel.WARNING);
        logMessage.setMessage(message);
        logMessage(logMessage);
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
        logMessage.setSrc(LogSource.USER);
        logMessage.setLevel(LogLevel.ERROR);
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

    public void addTestAttribute(final String name, final Object value) {
        if (lastCaseResult.get() != null) {
            lastCaseResult.get().addTestAttribute(name, value);
        }
    }

    public void addScreencastAttachment(final String videoFilePath, final boolean addToStep) {
        final IResultWithAttachment resultWithAttachment;
        if (addToStep && startedStepsQueue.get() != null && !startedStepsQueue.get().isEmpty()) {
            resultWithAttachment = startedStepsQueue.get().peek();
        }
        else if (!addToStep) {
            if (lastCaseResult.get() != null)
                resultWithAttachment = lastCaseResult.get();
            else if (lastSuiteResult.get() != null)
                resultWithAttachment = lastSuiteResult.get();
            else
                return;
        }
        else
            return;
        Attachment attachment = AttachmentHelper.prepareScreencastAttachment(videoFilePath);
        resultWithAttachment.addAttachment(attachment);
    }
    public void addScreencastAttachment(final byte[] videoData, final boolean addToStep) {
        final IResultWithAttachment resultWithAttachment;
        if (addToStep && startedStepsQueue.get() != null && !startedStepsQueue.get().isEmpty()) {
            resultWithAttachment = startedStepsQueue.get().peek();
        }
        else if (!addToStep) {
            if (lastCaseResult.get() != null)
                resultWithAttachment = lastCaseResult.get();
            else if (lastSuiteResult.get() != null)
                resultWithAttachment = lastSuiteResult.get();
            else
                return;
        }
        else
            return;
        Attachment attachment = AttachmentHelper.prepareScreencastAttachment(videoData);
        resultWithAttachment.addAttachment(attachment);
    }

    public void addScreenshotAttachment(final byte[] data, final boolean addToStep) {
        final IResultWithAttachment resultWithAttachment;
        if (addToStep && startedStepsQueue.get() != null && !startedStepsQueue.get().isEmpty()) {
            resultWithAttachment = startedStepsQueue.get().peek();
        }
        else if (!addToStep) {
            if (lastCaseResult.get() != null)
                resultWithAttachment = lastCaseResult.get();
            else if (lastSuiteResult.get() != null)
                resultWithAttachment = lastSuiteResult.get();
            else
                return;
        }
        else
            return;
        Attachment attachment = AttachmentHelper.prepareScreenshotAttachment(data);
        resultWithAttachment.addAttachment(attachment);
    }



    private static String getHostName() {
        try {
            return Optional.ofNullable(SystemUtils.getHostName()).orElse(InetAddress.getLocalHost().getHostName());
        }
        catch (UnknownHostException e) {
            return null;
        }
    }

    public StepResult getLastStartedStep() {
        if (startedStepsQueue.get() != null)
            return startedStepsQueue.get().peek();
        return null;
    }

    public SuiteResult getStartedSuite() {
        if (lastSuiteResult.get() != null)
            return lastSuiteResult.get();
        return null;
    }

    public CaseResult getStartedCase() {
        if (lastCaseResult.get() != null)
            return lastCaseResult.get();
        return null;
    }

}
