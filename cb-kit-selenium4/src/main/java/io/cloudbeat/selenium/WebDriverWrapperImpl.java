package io.cloudbeat.selenium;

import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.wrapper.webdriver.AbstractWebDriver;
import io.cloudbeat.common.wrapper.webdriver.WebDriverEventHandler;
import io.cloudbeat.common.wrapper.webdriver.WebDriverWrapper;
import io.cloudbeat.common.wrapper.webdriver.WrapperOptions;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;

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
    public <D> Pair<D, AbstractWebDriver> wrap(D driver, WrapperOptions options) {
        if (!(driver instanceof org.openqa.selenium.WebDriver) || reporter == null)
            return Pair.of(driver, null);
        final AbstractWebDriver abstractDriver;
        CbWebDriverListener listener = new CbWebDriverListener(
                new WebDriverEventHandler(reporter, abstractDriver = new Selenium4WebDriver((WebDriver) driver)),
                options
        );
        org.openqa.selenium.WebDriver decorated = new EventFiringDecorator(listener).decorate((org.openqa.selenium.WebDriver) driver);
        return Pair.of((D)decorated, abstractDriver);
    }
    @Override
    public <D, L> Pair<L, AbstractWebDriver> getListener(D driver, WrapperOptions options) {
        if (!(driver instanceof org.openqa.selenium.WebDriver) || reporter == null)
            return Pair.of(null, null);
        final AbstractWebDriver abstractDriver;
        CbWebDriverListener listener = new CbWebDriverListener(
                new WebDriverEventHandler(reporter, abstractDriver = new Selenium4WebDriver((WebDriver) driver)),
                options
        );
        return Pair.of((L)listener, abstractDriver);
    }

}
