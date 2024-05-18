package io.cloudbeat.common.client.api;

import io.cloudbeat.common.client.CbApiResponse;
import io.cloudbeat.common.client.dto.TestStatusRequest;
import io.cloudbeat.common.model.runtime.NewInstanceOptions;
import io.cloudbeat.common.model.runtime.NewRunOptions;
import io.cloudbeat.common.reporter.model.RunStatus;
import io.cloudbeat.common.reporter.model.TestResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RuntimeApiRetro {
    @PUT("runs/api/run")
    Call<CbApiResponse<String>> newRun(@Body NewRunOptions options);
    @PUT("runs/api/run/{runId}/instance")
    Call<CbApiResponse<String>> newInstance(@Path("runId") String runId, @Body NewInstanceOptions options);
    @POST("api/run/{runId}/stop")
    Call<Void> stopRun(@Path("runId") String runId);
    @POST("api/run/{runId}/status")
    Call<Void> updateRunStatus(@Path("runId") String runId, @Body RunStatus status);
    @POST("api/run/{runId}/instance/{instanceId}/status")
    Call<Void> updateInstanceStatus(@Path("runId") String runId, @Path("instanceId") String instanceId, @Body RunStatus status);
    @POST("api/run/{runId}/instance/{instanceId}/case")
    Call<Void> updateTestCaseStatus(@Path("runId") String runId, @Path("instanceId") String instanceId, @Body TestStatusRequest status);
    @POST("api/run/{runId}/instance/{instanceId}/end")
    Call<Void> endInstance(@Path("runId") String runId, @Path("instanceId") String instanceId);
    @PUT("runs/api/run/{runId}/instance/{instanceId}/result")
    Call<Void> publishInstanceResult(
            @Path("runId") String runId,
            @Path("instanceId") String instanceId,
            @Body TestResult result
    );
}
