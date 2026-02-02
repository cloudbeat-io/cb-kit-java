package io.cloudbeat.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(CbJiraLinks.class)
public @interface CbJira {
    String value();
    String source() default CbLink.SRC_JIRA;
    String type() default "";
}
