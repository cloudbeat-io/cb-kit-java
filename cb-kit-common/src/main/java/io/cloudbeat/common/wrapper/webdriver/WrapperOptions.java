package io.cloudbeat.common.wrapper.webdriver;

public final class WrapperOptions {
    private boolean ignoreFindElement = true;
    private boolean ignoreFindElements = false;
    private boolean fullPageScreenshot = true;
    private boolean takeScreenshotOnError = true;
    private boolean savePageSourceOnError = false;

    public WrapperOptions() {}

    public WrapperOptions(boolean ignoreFindElement, boolean fullPageScreenshot) {
        this.ignoreFindElement = ignoreFindElement;
        this.fullPageScreenshot = fullPageScreenshot;
    }
    public WrapperOptions(boolean ignoreFindElement, boolean fullPageScreenshot, boolean takeScreenshotOnError, boolean savePageSourceOnError) {
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
    public boolean isTakeScreenshotOnError() {
        return takeScreenshotOnError;
    }
    public boolean isSavePageSourceOnError() {
        return savePageSourceOnError;
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
    public void setTakeScreenshotOnError(boolean takeScreenshotOnError) {
        this.takeScreenshotOnError = takeScreenshotOnError;
    }
    public void setSavePageSourceOnError(boolean savePageSourceOnError) {
        this.savePageSourceOnError = savePageSourceOnError;
    }
}
