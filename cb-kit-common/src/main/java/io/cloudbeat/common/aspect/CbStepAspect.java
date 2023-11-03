package io.cloudbeat.common.aspect;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.helper.WebDriverHelper;
import io.cloudbeat.common.annotation.CbStep;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.AbstractMap;
import java.util.List;

@Aspect
public class CbStepAspect {
    static CbTestContext ctx = CbTestContext.getInstance();

    // @Pointcut("@annotation(io.cloudbeat.common.annotation.CbStep)")
    @Pointcut("@annotation(step)")
    public void withStepAnnotation(final CbStep step) {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    // @Pointcut("execution(* *.*(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before(value = "anyMethod() && withStepAnnotation(step)", argNames = "joinPoint,step")
    public void stepStart(final JoinPoint joinPoint, final CbStep step) {
        if (!ctx.isActive() || ctx.getReporter() == null)
            return;
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final List<AbstractMap.SimpleImmutableEntry<String, Object>> parameters = AspectUtils.getParameters(methodSignature, joinPoint.getArgs());
        ctx.getReporter().startStep(step.value());
    }

    @AfterThrowing(value = "anyMethod() && withStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
    public void stepFailed(final CbStep step, final Throwable throwable) {
        if (!ctx.isActive() || ctx.getReporter() == null)
            return;
        if (throwable instanceof AssertionError && ctx.getAbstractWebDriver() != null)
            ctx.getReporter().failLastStep(throwable, WebDriverHelper.getScreenshotFromAbstractWebDriver());
        else
            ctx.getReporter().failLastStep(throwable);
    }

    @AfterReturning(value = "anyMethod() && withStepAnnotation(step)", argNames = "step")
    public void stepEnd(final CbStep step) {
        if (!ctx.isActive() || ctx.getReporter() == null)
            return;
        ctx.getReporter().passLastStep();
    }
}
