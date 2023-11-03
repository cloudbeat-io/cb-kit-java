package io.cloudbeat.selenium;

import io.cloudbeat.common.reporter.model.LogLevel;
import org.openqa.selenium.devtools.events.ConsoleEvent;
import org.openqa.selenium.devtools.v117.runtime.model.PropertyPreview;
import org.openqa.selenium.devtools.v117.runtime.model.RemoteObject;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class DevToolsHelper {
    public static String stringifyRemoteObjectValue(RemoteObject ro) {
        return ro.getValue().isPresent() ? ro.getValue().get().toString() : null;
    }

    public static LogLevel getLogLevelForConsoleEvent(ConsoleEvent consoleEvent) {
        if (consoleEvent == null)
            return null;
        if (consoleEvent.getType().equalsIgnoreCase("log"))
            return LogLevel.INFO;
        else if (consoleEvent.getType().equalsIgnoreCase("error"))
            return LogLevel.ERROR;
        else if (consoleEvent.getType().equalsIgnoreCase("warn"))
            return LogLevel.WARNING;
        else if (consoleEvent.getType().equalsIgnoreCase("debug"))
            return LogLevel.DEBUG;
        return LogLevel.INFO;
    }

    public static LogLevel getLogLevelForJavaLevel(Level javaLevel) {
        if (javaLevel == null)
            return null;
        if (javaLevel == Level.INFO)
            return LogLevel.INFO;
        else if (javaLevel == Level.SEVERE)
            return LogLevel.ERROR;
        else if (javaLevel == Level.WARNING)
            return LogLevel.WARNING;
        return LogLevel.INFO;
    }

    public static List<Object> getArgumentListForConsoleEvent(ConsoleEvent consoleEvent) {
        if (consoleEvent.getArgs().size() > 0 && consoleEvent.getArgs().get(0) instanceof List) {
            List<RemoteObject> args = (List) consoleEvent.getArgs().get(0);
            try {
                List<Object> modifiedArgs = args.stream()
                        .map(ro -> serializeRemoteObject(ro)/*DevToolsHelper.stringifyRemoteObjectValue(ro)*/)
                        .skip(1)
                        .filter(v -> v != null)
                        .collect(Collectors.toList());
                return modifiedArgs;
            } catch (Throwable e) {
                return null;
            }
        }
        return null;
    }

    private static Object serializeRemoteObject(RemoteObject ro) {
        if (ro.getValue().isPresent())
            return ro.getValue().get();
        if (ro.getPreview().isPresent()) {
            if (!ro.getPreview().get().getProperties().isEmpty()) {
                String propsSerializedJson = ro.getPreview().get().getProperties().stream()
                        .map(p -> serializePropertyPreview(p))
                        .collect(Collectors.joining(", "));
                return propsSerializedJson;
            }
        }
        return  null;
    }

    private static String serializePropertyPreview(PropertyPreview prop) {
        final String propName = prop.getName();
        final Object propVal = prop.getValue().isPresent() ? prop.getValue().get().toString() : "null";
        if (propVal instanceof String)
            return String.format("%s: '%s'", propName, propVal);
        return String.format("%s: %s", propName, propVal);
    }
}
