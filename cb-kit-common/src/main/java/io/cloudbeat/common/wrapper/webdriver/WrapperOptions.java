package io.cloudbeat.common.wrapper.webdriver;

public final class WrapperOptions {
    private boolean ignoreFindElement = true;
    private boolean ignoreFindElements = false;
    private boolean fullPageScreenshot = true;

    public WrapperOptions() {}

    public WrapperOptions(boolean ignoreFindElement, boolean fullPageScreenshot) {
        this.ignoreFindElement = ignoreFindElement;
        this.fullPageScreenshot = fullPageScreenshot;
    }

    public boolean isIgnoreFindElement() {
        return ignoreFindElement;
    }

    public boolean isIgnoreFindElements() {
        return ignoreFindElements;
    }

    public boolean isFullPageScreenshot() {
        return fullPageScreenshot;
    }

    public void setIgnoreFindElement(boolean ignoreFindElement) {
        this.ignoreFindElement = ignoreFindElement;
    }

    public void setIgnoreFindElements(boolean ignoreFindElements) {
        this.ignoreFindElements = ignoreFindElements;
    }

    public void setFullPageScreenshot(boolean fullPageScreenshot) {
        this.fullPageScreenshot = fullPageScreenshot;
    }
}
