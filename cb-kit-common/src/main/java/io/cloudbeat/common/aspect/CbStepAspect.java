package io.cloudbeat.common.aspect;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.helper.WebDriverHelper;
import io.cloudbeat.common.annotation.CbStep;
import io.cloudbeat.common.reporter.model.StepResult;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;

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
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        List<AbstractMap.SimpleImmutableEntry<String, Object>> parameters = AspectUtils.getParameters(methodSignature, joinPoint.getArgs());
        List<String> argList = parameters.stream().map(p -> p.getValue().toString()).collect(Collectors.toList());
        String stepName = AspectUtils.resolveStepArguments(step.value(), argList);
        StepResult stepResult = ctx.getReporter().startStep(stepName);
        if (argList != null && argList.size() > 0)
            stepResult.setArgs(argList);
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
