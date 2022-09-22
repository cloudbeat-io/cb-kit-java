package io.cloudbeat.common.client;

public class CbApiResponse<T> {
    T data;
    int statusCode;
    String message;

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int code) { this.statusCode = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
