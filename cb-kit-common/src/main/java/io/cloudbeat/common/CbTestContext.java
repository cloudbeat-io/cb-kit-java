package io.cloudbeat.common;

import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.config.CbConfigLoader;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.wrapper.restassured.CbRestAssuredFilter;
import io.cloudbeat.common.wrapper.restassured.RestAssuredFailureListener;
import io.cloudbeat.common.wrapper.webdriver.AbstractWebDriver;
import io.cloudbeat.common.wrapper.webdriver.WebDriverWrapper;
import io.cloudbeat.common.wrapper.webdriver.WrapperOptions;
import io.restassured.RestAssured;
import io.restassured.config.FailureConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class CbTestContext {
    /**
     * Thread local context that stores information about not finished tests and steps.
     */
    private static class ThreadContext extends InheritableThreadLocal<CbTestContext> {
        @Override
        public CbTestContext initialValue() {
            return new CbTestContext();
        }

        @Override
        protected CbTestContext childValue(final CbTestContext parentStepContext) {
            return parentStepContext;
        }
    }
    //private static final Logger LOGGER = LoggerFactory.getLogger(CbTestContext.class);
    private static final ThreadContext CURRENT_CONTEXT = new ThreadContext();

    private final ThreadLocal<AbstractWebDriver> currentAbstractWebDriver = new ThreadLocal();

            //InheritableThreadLocal.withInitial(() -> new CbTestContext());
    /*new ThreadLocal<CbTestContext>() {
        @Override
        protected CbTestContext initialValue() {
            return new CbTestContext();
        }
    };*/
            //new InheritableThreadLocal<>();

    private CbTestReporter reporter;
    private CbConfig config;
    private boolean isActive;
    private Class currentTestClass;

    public CbTestContext() {
        isActive = false;
        try {
            //System.setOut(new ConsoleOutputWrapper());
            config = CbConfigLoader.load();
            if (config != null) {
                reporter = new CbTestReporter(config);
                isActive = true;
            }
        }
        catch (IOException e) {
            System.err.println("Failed to initialize CbTestContext: " + e);
        }
        CURRENT_CONTEXT.set(this);
    }
    /**
     * Returns a current test context linked to the current thread.
     *
     * @return test context instance
     */
    public static CbTestContext getInstance() {
        return CURRENT_CONTEXT.get();
    }

    @SuppressWarnings("unused")
    public void setCurrentTestClass(Class testClass) { this.currentTestClass = testClass; }

    public CbTestReporter getReporter() {
        return this.reporter;
    }

    public CbConfig getConfig() { return config; }

    public Class getCurrentTestClass() { return currentTestClass; }

    public boolean isActive() { return isActive; }

    public void wrapRestAssured() {
        RestAssured.filters(new CbRestAssuredFilter(this));
        RestAssured.config = RestAssured.config()
                .failureConfig(FailureConfig.failureConfig().with().failureListeners(
                        new RestAssuredFailureListener(this)));
    }
    public <D, L> L getWebDriverListener(D driver) {
        return getWebDriverListener(driver, null);
    }
    public <D, L> L getWebDriverListener(D driver, WrapperOptions options) {
        try {
            Class wrapperClass = Class.forName("io.cloudbeat.selenium.WebDriverWrapperImpl");
            WebDriverWrapper wrapper = (WebDriverWrapper) wrapperClass
                    .getDeclaredConstructor(new Class[] { CbTestReporter.class })
                    .newInstance(this.reporter);
            Pair<L, AbstractWebDriver> pair = wrapper.getListener(driver, options);
            currentAbstractWebDriver.set(pair.getRight());
            return pair.getLeft();
        }
        catch (ClassNotFoundException e) {
            // ignore
            System.err.println(e.getMessage());
        }
        catch (NoSuchMethodException e) {
            // ignore
            System.err.println("getWebDriverListener failed: " + e.toString());
        }
        catch (InstantiationException e) {
            // ignore
            System.err.println("getWebDriverListener failed: " + e.toString());
        }
        catch (IllegalAccessException e) {
            // ignore
            System.err.println("getWebDriverListener failed: " + e.toString());
        }
        catch (InvocationTargetException e) {
            // ignore
            System.err.println("getWebDriverListener failed: " + e.toString());
        }
        catch (RuntimeException e) {
            // ignore
            System.err.println("getWebDriverListener failed: " + e.toString());
        }
        currentAbstractWebDriver.remove();
        return null;
    }
    public <D> D wrapWebDriver(D driver) {
        return wrapWebDriver(driver, null);
    }
    public <D> D wrapWebDriver(D driver, WrapperOptions options) {
        if (this.reporter == null || !this.isActive())
            return driver;
        try {
            Class wrapperClass = Class.forName("io.cloudbeat.selenium.WebDriverWrapperImpl");
            WebDriverWrapper wrapper = (WebDriverWrapper) wrapperClass
                    .getDeclaredConstructor(new Class[] { CbTestReporter.class })
                    .newInstance(this.reporter);
            if (wrapper != null) {
                Pair<D, AbstractWebDriver> pair =
                        wrapper.wrap(driver, options != null ? options : new WrapperOptions());
                currentAbstractWebDriver.set(pair.getRight());
                return pair.getLeft();
            }
        }
        catch (ClassNotFoundException e) {
            // ignore
            System.err.println("wrapWebDriver failed: " + e.toString());
        }
        catch (NoSuchMethodException e) {
            // ignore
            System.err.println("wrapWebDriver failed: " + e.toString());
        }
        catch (InstantiationException e) {
            // ignore
            System.err.println("wrapWebDriver failed: " + e.toString());
        }
        catch (IllegalAccessException e) {
            // ignore
            System.err.println("wrapWebDriver failed: " + e.toString());
        }
        catch (InvocationTargetException e) {
            // ignore
            System.err.println("wrapWebDriver failed: " + e.toString());
        }
        catch (RuntimeException e) {
            // ignore
            System.err.println("wrapWebDriver failed: " + e.toString());
        }
        currentAbstractWebDriver.remove();
        return driver;
    }

    public AbstractWebDriver getAbstractWebDriver() {
        return currentAbstractWebDriver.get();
    }
}
