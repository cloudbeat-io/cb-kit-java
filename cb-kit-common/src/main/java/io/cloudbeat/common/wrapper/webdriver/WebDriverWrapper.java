package io.cloudbeat.common.wrapper.webdriver;

import org.apache.commons.lang3.tuple.Pair;

public interface WebDriverWrapper {
    public <D> Pair<D, AbstractWebDriver> wrap(D driver, WrapperOptions options);
    public <D, L> Pair<L, AbstractWebDriver> getListener(D driver, WrapperOptions options);
}
