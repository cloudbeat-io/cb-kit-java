package io.cloudbeat.common.har.model;

public class Har {
    private final HarLog harLog;

    public Har(HarLog harLog) {
        this.harLog = harLog;
    }

    public HarLog getLog() {
        return harLog;
    }
}
