package io.cloudbeat.common.helper;

import io.cloudbeat.common.CbTestContext;

public class WebDriverHelper {
    public static String getScreenshotFromAbstractWebDriver() {
        CbTestContext ctx = CbTestContext.getInstance();
        if (ctx == null || ctx.getAbstractWebDriver() == null)
            return null;
        try {
            return ctx.getAbstractWebDriver().getScreenshot();
        }
        catch (Throwable e) {
            System.err.println("stepFailed - failed to take a screenshot: " + e.getMessage());
        }
        return null;
    }
}
