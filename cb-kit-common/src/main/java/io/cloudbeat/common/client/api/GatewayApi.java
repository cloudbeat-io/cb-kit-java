package io.cloudbeat.common.client.api;

import io.cloudbeat.common.client.CbClientException;
import io.cloudbeat.common.client.RetrofitApiBase;
import io.cloudbeat.common.client.dto.CaseStatusInfoDto;
import io.cloudbeat.common.client.dto.TestStatusRequest;
import io.cloudbeat.common.model.runtime.NewInstanceOptions;
import io.cloudbeat.common.model.runtime.NewRunOptions;
import io.cloudbeat.common.reporter.model.FailureResult;
import io.cloudbeat.common.reporter.model.RunStatus;
import io.cloudbeat.common.reporter.model.TestResult;
import io.cloudbeat.common.reporter.model.TestStatus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Optional;

public class GatewayApi extends RetrofitApiBase {
    final GatewayApiRetro retroApi;
    public GatewayApi(GatewayApiRetro retroApi) {
        this.retroApi = retroApi;
    }

    public void updateTestCaseStatus(
            String runId,
            String instanceId,
            String caseResultId,
            String caseFqn,
            String caseName,
            Optional<TestStatus> status,
            FailureResult failure
    ) throws CbClientException {
        TestStatusRequest req = new TestStatusRequest();
        req.setRunId(runId);
        req.setInstanceId(instanceId);
        req.setTimestamp(Calendar.getInstance().getTimeInMillis());
        req.setProgress(1);
        req.setCase(new CaseStatusInfoDto());
        req.getCase().setFqn(caseFqn);
        req.getCase().setName(caseName);
        req.getCase().setCaseResultId(caseResultId);
        req.setStatus(RunStatus.RUNNING);
        if (status.isPresent()) {
            req.getCase().setProgress(1);
            if (status.get() == TestStatus.PASSED)
                req.getCase().setIterationsPassed(1);
            else if (status.get() == TestStatus.FAILED)
                req.getCase().setIterationsFailed(1);
            else if (status.get() == TestStatus.SKIPPED)
                req.getCase().setIterationsSkipped(1);
        }
        else
            req.getCase().setProgress(0.1f);

        if (failure != null) {
            req.getCase().setFailures(new ArrayList<>());
            CaseStatusInfoDto.FailureInfoDto failureInfo = new CaseStatusInfoDto.FailureInfoDto();
            failureInfo.setMessage(failure.getMessage());
            failureInfo.setDetails(failure.getData());
            failureInfo.setType(failure.getType());
            failureInfo.setSubtype(failure.getSubtype());
            failureInfo.setIsFatal(true);
            req.getCase().getFailures().add(failureInfo);
        }
        executeAsync(retroApi.updateTestCaseStatus(req));
    }
}
