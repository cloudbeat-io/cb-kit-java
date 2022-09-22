package io.cloudbeat.common.webdriver;

public interface WebDriverWrapper {
    public <D> D wrap(D driver);
}
