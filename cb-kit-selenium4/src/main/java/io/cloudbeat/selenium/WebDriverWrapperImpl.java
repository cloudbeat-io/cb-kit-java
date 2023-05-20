package io.cloudbeat.selenium;

import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.LogMessage;
import io.cloudbeat.common.webdriver.AbstractWebDriver;
import io.cloudbeat.common.webdriver.WebDriverEventHandler;
import io.cloudbeat.common.webdriver.WebDriverWrapper;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.util.List;
import java.util.Map;

public class WebDriverWrapperImpl implements WebDriverWrapper {
    private CbTestReporter reporter;
    private WebDriverEventHandler eventHandler;
    private org.openqa.selenium.WebDriver webDriver;

    public WebDriverWrapperImpl(CbTestReporter reporter) {
        init(reporter);
    }

    private boolean init(CbTestReporter reporter) {
        if (reporter == null || !reporter.isStarted())
            return false;   // CB plugin is not active
        if (this.reporter != null && this.eventHandler != null)
            return true;    // already initialized
        this.reporter = reporter;
        return true;
    }

    @Override
    public <D> D wrap(D driver) {
        if (!(driver instanceof org.openqa.selenium.WebDriver))
            return driver;
        CbWebDriverListener listener = new CbWebDriverListener(
                new WebDriverEventHandler(reporter, new Selenium4WebDriver((WebDriver) driver))
        );
        org.openqa.selenium.WebDriver decorated = new EventFiringDecorator(listener).decorate((org.openqa.selenium.WebDriver) driver);
        return (D)decorated;
    }
    @Override
    public <D, L> L getListener(D driver) {
        if (!(driver instanceof org.openqa.selenium.WebDriver))
            return null;
        CbWebDriverListener listener = new CbWebDriverListener(
                new WebDriverEventHandler(reporter, new Selenium4WebDriver((WebDriver) driver))
        );
        return (L)listener;
    }

}
