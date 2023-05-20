package io.cloudbeat.selenium;

import io.cloudbeat.common.webdriver.AbstractWebElement;
import org.openqa.selenium.WebElement;

public class SE4Element implements AbstractWebElement {
    private final WebElement seElm;
    public SE4Element(WebElement element) {
        seElm = element;
    }

    @Override
    public String getText() {
        return seElm.getText();
    }

    @Override
    public String getTagName() {
        return seElm.getTagName();
    }

    @Override
    public String getAttribute(String attributeName) {
        return seElm.getAttribute(attributeName);
    }
}
