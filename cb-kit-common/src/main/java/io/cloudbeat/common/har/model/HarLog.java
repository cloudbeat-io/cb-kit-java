package io.cloudbeat.common.har.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

/**
 * Root object of exported data.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#log">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarLog {

    private static final String DEFAULT_HAR_LOG_VERSION = "1.2";

    private String version = DEFAULT_HAR_LOG_VERSION;
    private HarCreator creator = new HarCreator();
    private HarCreator browser;
    private List<HarPage> pages = new ArrayList<>();
    private List<HarEntry> entries = new ArrayList<>();
    private String comment;
    private final Map<String, Object> additional = new HashMap<>();

    /**
     * @return Version number of the format.
     * Defaults to {@link #DEFAULT_HAR_LOG_VERSION}
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version == null || version.trim().equals("")) {
            version = DEFAULT_HAR_LOG_VERSION;
        }
        this.version = version;
    }

    /**
     * @return Information about the application used to generate HAR.
     */
    public HarCreator getCreator() {
        if (creator == null) {
            creator = new HarCreator();
        }
        return creator;
    }

    public void setCreator(HarCreator creator) {
        this.creator = creator;
    }

    /**
     * @return Information about the browser used.
     */
    public HarCreator getBrowser() {
        if (browser == null) {
            browser = new HarCreator();
        }
        return browser;
    }

    public void setBrowser(HarCreator browser) {
        this.browser = browser;
    }

    /**
     * @return List of all exported pages, may be empty.
     */
    public List<HarPage> getPages() {
        if (pages == null) {
            pages = new ArrayList<>();
        }
        return pages;
    }

    public void setPages(List<HarPage> pages) {
        this.pages = pages;
    }

    /**
     * @return List of all exported requests, may be empty.
     */
    public List<HarEntry> getEntries() {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        return entries;
    }

    public void setEntries(List<HarEntry> entries) {
        this.entries = entries;
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
        if (!(o instanceof HarLog)) return false;
        HarLog harLog = (HarLog) o;
        return Objects.equals(version, harLog.version) &&
                Objects.equals(creator, harLog.creator) &&
                Objects.equals(browser, harLog.browser) &&
                Objects.equals(pages, harLog.pages) &&
                Objects.equals(entries, harLog.entries) &&
                Objects.equals(comment, harLog.comment) &&
                Objects.equals(additional, harLog.additional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, creator, browser, pages, entries, comment, additional);
    }
}
