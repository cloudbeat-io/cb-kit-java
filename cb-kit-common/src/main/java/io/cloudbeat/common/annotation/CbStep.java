package io.cloudbeat.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CbStep {

    String value() default "";

    String description() default "";
}
