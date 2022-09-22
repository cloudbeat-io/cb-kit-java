package io.cloudbeat.common.reporter.model.extra.http;

import java.util.Map;

public class HttpRequestResult {
    private String url;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> cookies;
    private String body;
    private Map<String, String> queryParams;
    private String contentType;


    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getContentType() {
        return contentType;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}