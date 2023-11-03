package io.cloudbeat.common.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HttpNetworkEntry {
    public HttpNetworkEntry() {
        this.request = new HttpRequestEntry();
        this.response = new HttpResponseEntry();
    }
    protected HttpRequestEntry request;
    protected HttpResponseEntry response;
    protected Date startTime;
    protected Date endTime;

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public HttpRequestEntry getRequest() {
        return request;
    }

    public HttpResponseEntry getResponse() {
        return response;
    }

    public void setRequest(HttpRequestEntry request) {
        this.request = request;
    }

    public void setResponse(HttpResponseEntry response) {
        this.response = response;
    }
    public class HttpRequestEntry {
        private long timestamp;
        private String url;
        private String method;
        private String postData;
        private Map<String, Object> headers;

        public HttpRequestEntry() {
            headers = new HashMap<>();
        }

        public String getUrl() {
            return url;
        }

        public String getMethod() {
            return method;
        }

        public String getPostData() {
            return postData;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public void setPostData(String postData) {
            this.postData = postData;
        }

        public void setHeaders(Map<String, Object> headers) {
            this.headers = headers;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
    public class HttpResponseEntry {
        private long timestamp;
        private Map<String, Object> headers;
        private String url;
        private Integer status;
        private String statusText;

        public HttpResponseEntry() {
            headers = new HashMap<>();
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }

        public String getUrl() {
            return url;
        }

        public Integer getStatus() {
            return status;
        }

        public String getStatusText() {
            return statusText;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public void setStatusText(String statusText) {
            this.statusText = statusText;
        }

        public void setHeaders(Map<String, Object> headers) {
            this.headers = headers;
        }
    }
}
