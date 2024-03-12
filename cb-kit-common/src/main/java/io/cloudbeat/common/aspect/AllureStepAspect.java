package io.cloudbeat.common.aspect;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.helper.WebDriverHelper;
import io.cloudbeat.common.reporter.model.StepResult;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import io.qameta.allure.Step;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;

@Aspect
public class AllureStepAspect {
    static CbTestContext ctx = CbTestContext.getInstance();

    @Pointcut("@annotation(step)")
    public void withAllureStepAnnotation(Step step) {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before(value = "anyMethod() && withAllureStepAnnotation(step)", argNames = "joinPoint,step")
    public void allureStepStart(final JoinPoint joinPoint, final Step step) {
        if (!ctx.isActive() || ctx.getReporter() == null)
            return;
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final List<AbstractMap.SimpleImmutableEntry<String, Object>> parameters = AspectUtils.getParameters(methodSignature, joinPoint.getArgs());
        StepResult stepResult = ctx.getReporter().startStep(step.value());
        if (parameters != null && parameters.size() > 0) {
            stepResult.setArgs(
                    parameters.stream().map(p -> p.getValue().toString()).collect(Collectors.toList())
            );
        }
    }

    @AfterThrowing(value = "anyMethod() && withAllureStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
    public void stepFailed(final Step step, final Throwable throwable) {
        if (!ctx.isActive() || ctx.getReporter() == null)
            return;
        if (throwable instanceof AssertionError && ctx.getAbstractWebDriver() != null)
            ctx.getReporter().failLastStep(throwable, WebDriverHelper.getScreenshotFromAbstractWebDriver());
        else
            ctx.getReporter().failLastStep(throwable);
    }

    @AfterReturning(value = "anyMethod() && withAllureStepAnnotation(step)", argNames = "step")
    public void stepEnd(final Step step) {
        if (!ctx.isActive() || ctx.getReporter() == null)
            return;
        ctx.getReporter().passLastStep();
    }
}
