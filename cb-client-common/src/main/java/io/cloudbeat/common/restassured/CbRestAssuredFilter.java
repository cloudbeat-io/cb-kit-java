package io.cloudbeat.common.restassured;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.reporter.model.StepType;
import io.cloudbeat.common.reporter.model.extra.http.HttpRequestResult;
import io.cloudbeat.common.reporter.model.extra.http.HttpResponseResult;
import io.cloudbeat.common.reporter.model.extra.http.HttpStepExtra;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.internal.NameAndValue;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CbRestAssuredFilter implements OrderedFilter {
    final CbTestContext ctx;
    final Prettifier prettifier = new Prettifier();
    public CbRestAssuredFilter(CbTestContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext filterContext) {
    //public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        // start step
        final String stepName = getStepName(requestSpec);
        final StepResult stepResult = ctx.getReporter().startStep(stepName, StepType.HTTP);
        // get a response
        final Response response = filterContext.next(requestSpec, responseSpec);
        // add request and response properties to "extra"
        stepResult.addExtra(StepType.HTTP.value(), generateHttpExtra(requestSpec, response));
        // end step
        ctx.getReporter().endStep(stepResult.getId());
        return response;
    }

    private String getStepName(FilterableRequestSpecification requestSpec) {
        final String url = requestSpec.getURI();
        final String method = requestSpec.getMethod();
        return String.format("%s %s", method, url);
    }

    private HttpStepExtra generateHttpExtra(FilterableRequestSpecification requestSpec, Response response) {
        HttpRequestResult requestResult = generateHttpRequestResult(requestSpec);
        HttpResponseResult responseResult = generateHttpResponseResult(response);
        return new HttpStepExtra(requestResult, responseResult);
    }

    private HttpRequestResult generateHttpRequestResult(FilterableRequestSpecification requestSpec) {
        final Map<String, String> headers = toMap(requestSpec.getHeaders());
        final Map<String, String> cookies = toMap(requestSpec.getCookies());
        final String body = Objects.nonNull(requestSpec.getBody())
                ? prettifier.getPrettifiedBodyIfPossible(requestSpec)
                : null;
        HttpRequestResult requestResult = new HttpRequestResult();
        requestResult.setUrl(requestSpec.getURI());
        requestResult.setMethod(requestSpec.getMethod());
        requestResult.setContentType(requestSpec.getContentType());
        requestResult.setHeaders(headers);
        requestResult.setCookies(cookies);
        requestResult.setQueryParams(requestSpec.getQueryParams());
        requestResult.setBody(body);

        return requestResult;
    }

    private HttpResponseResult generateHttpResponseResult(Response response) {
        final String body = prettifier.getPrettifiedBodyIfPossible(response, response.getBody());
        final Map<String, String> headers = toMap(response.getHeaders());

        HttpResponseResult responseResult = new HttpResponseResult();
        responseResult.setStatusCode(response.getStatusCode());
        responseResult.setStatusText(response.getStatusLine());
        responseResult.setBody(body);
        responseResult.setCookies(response.getCookies());
        responseResult.setHeaders(headers);
        responseResult.setContentType(response.getContentType());

        return responseResult;
    }



    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private static Map<String, String> toMap(final Iterable<? extends NameAndValue> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach(h -> result.put(h.getName(), h.getValue()));
        return result;
    }

}
