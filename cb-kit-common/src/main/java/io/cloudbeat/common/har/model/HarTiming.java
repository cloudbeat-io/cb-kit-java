package io.cloudbeat.common.har.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarTiming {

    protected static final Double DEFAULT_TIME = -1.0;

    private Double blocked;
    private Double dns;
    private Double connect;
    private Double send;
    private Double wait;
    private Double receive;
    private Double ssl;
    private String comment;
    private final Map<String, Object> additional = new HashMap<>();

    /**
     * @return Time spent in a queue waiting for a network connection.
     * {@link #DEFAULT_TIME} if the timing does not apply to the current request.
     */
    public Double getBlocked() {
        if (blocked == null) {
            return DEFAULT_TIME;
        }
        return blocked;
    }

    public void setBlocked(Double blocked) {
        this.blocked = blocked;
    }

    /**
     * @return DNS resolution time. The time required to resolve a host name.
     * {@link #DEFAULT_TIME} if the timing does not apply to the current request.
     */
    public Double getDns() {
        if (dns == null) {
            return DEFAULT_TIME;
        }
        return dns;
    }

    public void setDns(Double dns) {
        this.dns = dns;
    }

    /**
     * @return Time required to create TCP connection.
     * {@link #DEFAULT_TIME} if the timing does not apply to the current request.
     */
    public Double getConnect() {
        if (connect == null) {
            return DEFAULT_TIME;
        }
        return connect;
    }

    public void setConnect(Double connect) {
        this.connect = connect;
    }

    /**
     * @return Time required to send HTTP request to the server, null if not present.
     */
    public Double getSend() {
        return send;
    }

    public void setSend(Double send) {
        this.send = send;
    }

    /**
     * @return Waiting for a response from the server, null if not present.
     */
    public Double getWait() {
        return wait;
    }

    public void setWait(Double wait) {
        this.wait = wait;
    }

    /**
     * @return Time required to read entire response from the server (or cache), null if not present.
     */
    public Double getReceive() {
        return receive;
    }

    public void setReceive(Double receive) {
        this.receive = receive;
    }

    /**
     * @return Time required for SSL/TLS negotiation.
     * If this field is defined then the time is also included in the connect field
     * (to ensure backward compatibility with HAR 1.1).
     * {@link #DEFAULT_TIME} if the timing does not apply to the current request.
     */
    public Double getSsl() {
        if (ssl == null) {
            return DEFAULT_TIME;
        }
        return ssl;
    }

    public void setSsl(Double ssl) {
        this.ssl = ssl;
    }

    /**
     * @return Comment provided by the user or application, null if not present.
     */
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return Map with additional keys, which are not officially supported by the HAR specification
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditional() {
        return additional;
    }

    @JsonAnySetter
    public void setAdditionalField(String key, Object value) {
        this.additional.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HarTiming)) return false;
        HarTiming harTiming = (HarTiming) o;
        return Objects.equals(blocked, harTiming.blocked) &&
                Objects.equals(dns, harTiming.dns) &&
                Objects.equals(connect, harTiming.connect) &&
                Objects.equals(send, harTiming.send) &&
                Objects.equals(wait, harTiming.wait) &&
                Objects.equals(receive, harTiming.receive) &&
                Objects.equals(ssl, harTiming.ssl) &&
                Objects.equals(comment, harTiming.comment) &&
                Objects.equals(additional, harTiming.additional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocked, dns, connect, send, wait, receive, ssl, comment, additional);
    }
}
