package io.cloudbeat.common.reporter.model.extra.http;

import java.util.Map;

public class HttpResponseResult {
    private int statusCode;
    private String statusText;
    private Map<String, String> headers;
    private Map<String, String> cookies;
    private String body;
    private String contentType;

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
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

    public String getContentType() {
        return contentType;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
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

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}