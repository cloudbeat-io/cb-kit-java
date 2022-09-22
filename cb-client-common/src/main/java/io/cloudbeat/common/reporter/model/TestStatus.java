package io.cloudbeat.common.reporter.model;

public enum TestStatus {
    PASSED("passed"),
    FAILED("failed"),
    SKIPPED("skipped"),
    BROKEN("broken");

    private final String value;

    TestStatus(final String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value();
    }
}
