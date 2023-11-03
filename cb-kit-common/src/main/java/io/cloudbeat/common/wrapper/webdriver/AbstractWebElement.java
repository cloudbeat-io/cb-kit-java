package io.cloudbeat.common.wrapper.webdriver;

public interface AbstractWebElement {
    public String getText();
    public String getTagName();
    public String getAttribute(final String attributeName);
}
