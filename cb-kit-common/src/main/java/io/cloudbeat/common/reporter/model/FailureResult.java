package io.cloudbeat.common.reporter.model;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.helper.StackTraceHelper;
import io.cloudbeat.common.helper.WebDriverHelper;
import java.io.PrintWriter;
import java.io.StringWriter;

public class FailureResult {
    public final static String FAILURE_TYPE = "JAVA_ERROR";
    String type;
    String subtype;
    String data;
    String[] stacktrace;
    String message;
    String location;

    public FailureResult() {}

    public FailureResult(String message) {
        type = FAILURE_TYPE;
        this.message = message;
    }
    
    public FailureResult(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();
        final String testPackageName = getTestPackageName();
        StackTraceElement[] filteredStackTrace =
                StackTraceHelper.getStackTraceStartingFromPackage(throwable.getStackTrace(), testPackageName);
        this.subtype = throwable.getClass().getSimpleName();
        this.type = getFailureType(throwable);
        this.data = stackTrace;
        this.stacktrace = StackTraceHelper.stackTraceToStringArray(filteredStackTrace);
        this.message = throwable.getMessage();
        if(this.message == null) {
            this.message = "UNKNOWN_ERROR";
        }
        // set location attribute
        if (filteredStackTrace.length > 0) {
            this.location = filteredStackTrace[0].toString();
        }
    }

    private static String getTestPackageName() {
        CbTestContext ctx = CbTestContext.getInstance();
        if (ctx != null && ctx.isActive() && ctx.getCurrentTestClass() != null
                && ctx.getCurrentTestClass().getPackage() != null)
            return ctx.getCurrentTestClass().getPackage().getName();
        return null;
    }
    private static String getFailureType(Throwable throwable) {
        String className = throwable.getClass().getName();
        if (className.startsWith("org.openqa.selenium")) {
            String simpleName = throwable.getClass().getSimpleName();
            if (simpleName.equals("NoSuchElementException"))
                return "ELEMENT_NOT_FOUND";
            if (simpleName.equals("TimeoutException"))
                return "TIMEOUT";
            if (simpleName.equals("ElementNotVisibleException")
                    || simpleName.equals("ElementNotInteractableException"))
                return "ELEMENT_NOT_VISIBLE";
            return "SELENIUM_ERROR";
        }
        if (throwable instanceof AssertionError)
            return "ASSERT_ERROR";
        return FAILURE_TYPE;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getData() {
        return data;
    }

    public String[] getStacktrace() {
        return stacktrace;
    }

    public String getMessage() {
        return message;
    }

    public String getLocation() {
        return location;
    }

}
