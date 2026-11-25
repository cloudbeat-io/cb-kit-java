package io.cloudbeat.common.helper;

import io.cloudbeat.common.wrapper.webdriver.AbstractWebDriver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes the automation engine behind a wrapped WebDriver: Appium (mobile) or Selenium (browser), with the
 * platform, automation name, browser and device. Stored in each test result's context as "automation", so CloudBeat
 * (Test Health) knows what a JUnit / TestNG / Cucumber project actually drove and applies the matching rules.
 * Never throws: a driver that cannot tell gives null.
 */
public final class AutomationInfoHelper {
    private AutomationInfoHelper() { }

    public static Map<String, Object> describe(AbstractWebDriver driver) {
        if (driver == null)
            return null;
        try {
            final Map<String, Object> caps = driver.getCapabilities();
            final String platform = cap(caps, "platformName");
            final String automationName = cap(caps, "automationName");
            final String browser = text(caps, "browserName");
            final String device = cap(caps, "deviceName");
            final String platformVersion = cap(caps, "platformVersion");
            final boolean appium = driver.isAppiumDriver() || automationName != null
                    || "ios".equalsIgnoreCase(platform) || "android".equalsIgnoreCase(platform);

            final Map<String, Object> info = new LinkedHashMap<>();
            info.put("engine", appium ? "appium" : "selenium");
            if (platform != null) info.put("platform", platform);
            if (automationName != null) info.put("automationName", automationName);
            if (browser != null) info.put("browser", browser);
            if (device != null) info.put("device", device);
            if (platformVersion != null) info.put("platformVersion", platformVersion);
            return info;
        }
        catch (Throwable e) {
            return null;
        }
    }

    // "platformName" or the W3C-prefixed "appium:platformName"
    private static String cap(Map<String, Object> caps, String name) {
        final String value = text(caps, name);
        return value != null ? value : text(caps, "appium:" + name);
    }

    private static String text(Map<String, Object> caps, String name) {
        if (caps == null)
            return null;
        final Object value = caps.get(name);
        if (value == null)
            return null;
        final String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
