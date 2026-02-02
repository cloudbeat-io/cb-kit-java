package io.cloudbeat.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(CbLinks.class)
public @interface CbLink {
    String value();
    String source() default ""; // e.g. Jira, XRay, ADO, etc.
    String type() default "";   // e.g. Story, Task, Test Plan, Test Case, etc.
    // Possible "source" values
    public static final String SRC_URL = "url";
    public static final String SRC_JIRA = "jira";
    public static final String SRC_XRAY = "xray";
    public static final String SRC_AZURE_DEVOPS = "ado";
    public static final String SRC_TESTRAIL = "testrail";

    // Possible "type" values
    public static final String TYPE_EPIC = "epic";
    public static final String TYPE_STORY = "story";
    public static final String TYPE_TASK = "task";
    public static final String TYPE_TEST_PLAN = "plan";
    public static final String TYPE_TEST_CASE = "case";
    public static final String TYPE_TEST_BUG = "bug";
}

