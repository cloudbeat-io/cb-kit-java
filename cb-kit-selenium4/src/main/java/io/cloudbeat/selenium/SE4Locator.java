package io.cloudbeat.selenium;

import io.cloudbeat.common.webdriver.AbstractLocator;
import io.cloudbeat.common.webdriver.LocatorSelectorType;
import org.openqa.selenium.By;

public class SE4Locator implements AbstractLocator {
    private final By seBy;
    private final LocatorSelectorType type;

    public SE4Locator(By by) {
        this.seBy = by;

        if (by instanceof By.ById)
            type = LocatorSelectorType.Id;
        else if (by instanceof By.ByClassName)
            type = LocatorSelectorType.ClassName;
        else if (by instanceof By.ByCssSelector)
            type = LocatorSelectorType.CssSelector;
        else if (by instanceof By.ByLinkText)
            type = LocatorSelectorType.LinkText;
        else if (by instanceof By.ByName)
            type = LocatorSelectorType.Name;
        else if (by instanceof By.ByPartialLinkText)
            type = LocatorSelectorType.PartialLinkText;
        else if (by instanceof By.ByTagName)
            type = LocatorSelectorType.TagName;
        else if (by instanceof By.ByXPath)
            type = LocatorSelectorType.XPath;
        else
            type = LocatorSelectorType.Custom;
    }
    @Override
    public LocatorSelectorType getType() {
        return type;
    }
    @Override
    public String toString() {
        return seBy.toString();
    }
}
