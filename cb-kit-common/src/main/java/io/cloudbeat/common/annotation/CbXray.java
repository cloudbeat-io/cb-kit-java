package io.cloudbeat.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(CbXrayLinks.class)
public @interface CbXray {
    String value();
    String source() default CbLink.SRC_XRAY;
    String type() default "";
}
