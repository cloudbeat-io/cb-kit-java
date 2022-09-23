package io.cloudbeat.cucumber;

import io.cloudbeat.common.CbTestContext;

public final class CbCucumber {
    private static volatile CbCucumber instance;
    private CbTestContext ctx;

    public static CbCucumber getInstance() {
        if (instance == null) {
            synchronized (CbCucumber.class) {
                if (instance == null) {
                    instance = new CbCucumber();
                }
            }
        }

        return instance;
    }

    public static void beforeAllHook(final String hookName, final Runnable hookFunc) {
        beforeAllHook(hookName, hookFunc, false);
    }
    public static void beforeAllHook(final String hookName, final Runnable hookFunc, final boolean continueOnError) {
        CbCucumber instance = getInstance();
        if (!instance.ctx.isActive() || !instance.ctx.getReporter().isStarted())
            hookFunc.run();
        getInstance().ctx.getReporter().beforeAllHook(hookName, hookFunc, continueOnError);
    }

    public static void afterAllHook(final String hookName, final Runnable hookFunc) {
        afterAllHook(hookName, hookFunc, false);
    }
    public static void afterAllHook(final String hookName, final Runnable hookFunc, final boolean continueOnError) {
        CbCucumber instance = getInstance();
        if (!instance.ctx.isActive() || !instance.ctx.getReporter().isStarted())
            hookFunc.run();
        getInstance().ctx.getReporter().afterAllHook(hookName, hookFunc, continueOnError);
    }

    public static <D> D wrapWebDriver(D driver) {
        return getInstance().ctx.wrapWebDriver(driver);
    }

    private CbCucumber() {
        ctx = CbTestContext.getInstance();
        if (ctx.isActive() && !ctx.getReporter().isStarted()) {
            ctx.getReporter().setFramework("Cucumber", "1");
            ctx.getReporter().startInstance();
        }
    }
}
