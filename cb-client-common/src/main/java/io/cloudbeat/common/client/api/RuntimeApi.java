package io.cloudbeat.common.client.api;

import io.cloudbeat.common.client.CbClientException;
import io.cloudbeat.common.client.RetrofitApiBase;
import io.cloudbeat.common.model.runtime.NewInstanceOptions;
import io.cloudbeat.common.model.runtime.NewRunOptions;
import io.cloudbeat.common.reporter.model.RunStatus;
import io.cloudbeat.common.reporter.model.TestResult;

public class RuntimeApi extends RetrofitApiBase {
    final RuntimeApiRetro retroApi;
    public RuntimeApi(RuntimeApiRetro retroApi) {
        this.retroApi = retroApi;
    }
    public String newRun(NewRunOptions options) throws CbClientException {
        return executeWithApiResponse(retroApi.newRun(options));
    }

    public String newInstance(String runId, NewInstanceOptions options) throws CbClientException {
        return executeWithApiResponse(retroApi.newInstance(runId, options));
    }

    public void updateRunStatus(String runId, RunStatus status) throws CbClientException {
        execute(retroApi.updateRunStatus(runId, status));
    }
    public void updateInstanceStatus(String runId, String instanceId, RunStatus status) throws CbClientException {
        execute(retroApi.updateInstanceStatus(runId, instanceId, status));
    }

    public void endInstance(String runId, String instanceId, TestResult result) throws CbClientException {
        if (result == null)
            execute(retroApi.endInstance(runId, instanceId));
        else
            execute(retroApi.publishInstanceResult(runId, instanceId, result));
    }
}
