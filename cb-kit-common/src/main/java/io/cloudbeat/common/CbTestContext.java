package io.cloudbeat.common;

import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.config.CbConfigLoader;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.webdriver.WebDriverWrapper;

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

    public <D, L> L getWebDriverListener(D driver) {
        try {
            Class wrapperClass = Class.forName("io.cloudbeat.selenium.WebDriverWrapperImpl");
            WebDriverWrapper wrapper = (WebDriverWrapper) wrapperClass
                    .getDeclaredConstructor(new Class[] { CbTestReporter.class })
                    .newInstance(this.reporter);
            if (wrapper != null)
                return wrapper.getListener(driver);
        }
        catch (ClassNotFoundException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (NoSuchMethodException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (InstantiationException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (IllegalAccessException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (InvocationTargetException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (RuntimeException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        return null;
    }
    public <D> D wrapWebDriver(D driver) {
        if (this.reporter == null || !this.isActive())
            return driver;
        try {
            Class wrapperClass = Class.forName("io.cloudbeat.selenium.WebDriverWrapperImpl");
            WebDriverWrapper wrapper = (WebDriverWrapper) wrapperClass
                    .getDeclaredConstructor(new Class[] { CbTestReporter.class })
                    .newInstance(this.reporter);
            if (wrapper != null)
                return wrapper.wrap(driver);
        }
        catch (ClassNotFoundException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (NoSuchMethodException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (InstantiationException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (IllegalAccessException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (InvocationTargetException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        catch (RuntimeException e) {
            // ignore
            System.out.println(e.getMessage());
        }
        return driver;
    }
}
