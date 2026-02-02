package io.cloudbeat.common.annotation;

import io.cloudbeat.common.reporter.model.TestStatus;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Inherited
@Repeatable(CbFailures.class)
public @interface CbFailure {
    Class<? extends Exception> value();
    String reason() default "";
    String status() default "";
}

