package io.cloudbeat.common.reporter.model.extra.http;

import io.cloudbeat.common.reporter.model.extra.IStepExtra;

import java.util.Map;

public class HttpStepExtra implements IStepExtra {
    private HttpRequestResult requestResult;
    private HttpResponseResult responseResult;

    public HttpStepExtra(HttpRequestResult requestResult, HttpResponseResult responseResult) {
        this.requestResult = requestResult;
        this.responseResult = responseResult;
    }

    public HttpRequestResult getRequest() {
        return requestResult;
    }

    public HttpResponseResult getResponse() {
        return responseResult;
    }
}
