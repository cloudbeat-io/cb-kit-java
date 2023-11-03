package io.cloudbeat.common.wrapper.webdriver;

public interface AbstractLocator {
    public LocatorSelectorType getType();
    public String toString();
}
