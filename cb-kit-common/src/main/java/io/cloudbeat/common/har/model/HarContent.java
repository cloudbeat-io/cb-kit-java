package io.cloudbeat.common.har.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Information about the response's content.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#content">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarContent {

    private Long size;
    private Long compression;
    private String mimeType;
    private String text;
    private String encoding;
    private String comment;
    private final Map<String, Object> additional = new HashMap<>();

    /**
     * @return Length of returned content in bytes, null if not present.
     */
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    /**
     * @return Number of bytes saved by compression, null if not present.
     */
    public Long getCompression() {
        return compression;
    }

    public void setCompression(Long compression) {
        this.compression = compression;
    }

    /**
     * @return MIME-Type of response, null if not present. May include the charset.
     */
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return Response body loaded from server or cache, null if not present.
     * Binary content may be encoded using encoding specified by {@link #getEncoding()}.
     */
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return Encoding used for encoding response body, null if not present.
     * @see #getText()
     */
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
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
        if (!(o instanceof HarContent)) return false;
        HarContent that = (HarContent) o;
        return Objects.equals(size, that.size) &&
                Objects.equals(compression, that.compression) &&
                Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(text, that.text) &&
                Objects.equals(encoding, that.encoding) &&
                Objects.equals(comment, that.comment) &&
                Objects.equals(additional, that.additional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, compression, mimeType, text, encoding, comment, additional);
    }
}
