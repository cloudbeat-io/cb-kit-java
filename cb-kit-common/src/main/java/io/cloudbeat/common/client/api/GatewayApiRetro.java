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

public interface GatewayApiRetro {
    @POST("testresult/Status")
    Call<Void> updateTestCaseStatus(@Body TestStatusRequest statusRequest);
}
