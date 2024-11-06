package io.cloudbeat.common.reporter.model;

public class OutputDataEntry {
    private String name;
    private Object data;

    public OutputDataEntry(String name, Object data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public Object getData() {
        return data;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
