package io.cloudbeat.common.client.api;

import io.cloudbeat.common.client.dto.LoadTestMetricsUpdateRequest;
import io.cloudbeat.common.client.dto.TestStatusRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GatewayApiRetro {
    @POST("testresult/Status")
    Call<Void> updateTestCaseStatus(@Body TestStatusRequest statusRequest);
    @POST("testresult/load/run/{runId}/instance/{instanceId}/metrics")
    Call<Void> updateLoadTestMetrics(@Path("runId") String runId, @Path("instanceId") String instanceId, @Body LoadTestMetricsUpdateRequest request);
}
