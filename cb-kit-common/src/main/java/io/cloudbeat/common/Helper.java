package io.cloudbeat.common;

import io.cloudbeat.common.config.CbConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class Helper {
    public static final String[] STACKTRACE_IGNORE_LIST = {
            "io.cloudbeat",
            "org.openqa.selenium",
            "java."
    };

    public static StackTraceElement[] filterStackTrace(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).filter(e -> {
            final String callStr = e.toString();
            return !Arrays.stream(STACKTRACE_IGNORE_LIST).anyMatch(f -> callStr.startsWith(f));
        }).toArray(StackTraceElement[]::new);
    }

    public static String extractPackageFromClassName(String className) {
        if (StringUtils.isEmpty(className)) {
            return null;
        }
        String[] classNameParts = className.split("\\.");
        if (classNameParts.length <= 1) {
            return StringUtils.EMPTY;
        }
        // remove the last part (e.g. actual class name without package prefix)
        String[] packagePartsOnly = Arrays.copyOf(classNameParts,classNameParts.length - 1);
        return String.join(".", packagePartsOnly);
    }

    public static StackTraceElement[] getStackTrace() {

        return Thread.currentThread().getStackTrace();
    }

    public static StackTraceElement[] getStackTraceStartingFromPackage(StackTraceElement[] stackTrace, String testPackageName) {
        if (StringUtils.isEmpty(testPackageName)) {
            return filterStackTrace(stackTrace);
        }
        int firstTestCallIndex = -1;
        for (int i=0; i < stackTrace.length; i++) {
            if (stackTrace[i].toString().startsWith(testPackageName)) {
                firstTestCallIndex = i;
                break;
            }
        }
        // if we found the index of the first test method call, then return the stacktrace from that call downwards
        if (firstTestCallIndex > -1) {
            return Arrays.copyOfRange(stackTrace, firstTestCallIndex, stackTrace.length - 1);
        }
        // we are not suppose to be here, as if testPackageName is correct,
        // then we will always find related call in the trace
        return filterStackTrace(stackTrace);
    }

    public static String[] stackTraceToStringArray(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).map(call -> call.toString()).toArray(String[]::new);
    }

    private static String lowerCaseIfBrowserName(String key, Object value) {
        // in some cases, CB engine can pass upper-cased browserName value
        // make sure we always send a lower case value back to the web driver
        if (key == "browserName" && value != null && value instanceof String)
            return value.toString().toLowerCase();
        return value.toString();
    }
}
