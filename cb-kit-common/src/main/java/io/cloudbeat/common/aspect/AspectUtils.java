package io.cloudbeat.common.aspect;

import org.aspectj.lang.reflect.MethodSignature;

import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class AspectUtils {
    public static List<AbstractMap.SimpleImmutableEntry<String, Object>> getParameters(final MethodSignature signature, final Object... args) {
        final java.lang.reflect.Parameter[] params = signature.getMethod().getParameters();
        return IntStream
            .range(0, args.length)
            .mapToObj(index -> {
                final String paramName = signature.getParameterNames()[index];
                final Object paramVal = args[index];
                final java.lang.reflect.Parameter ref = params[index];
                /*Stream.of(ref.getAnnotationsByType(Param.class))
                        .findFirst()
                        .ifPresent(param -> {
                            Stream.of(param.value(), param.name())
                                    .map(String::trim)
                                    .filter(name -> name.length() > 0)
                                    .findFirst()
                                    .ifPresent(parameter::setName);

                            parameter.setMode(param.mode());
                            parameter.setExcluded(param.excluded());
                        });*/
                return new AbstractMap.SimpleImmutableEntry<String, Object>(paramName, paramVal);
            }).collect(Collectors.toList());
    }

    public static String resolveStepArguments(String stepName, List<String> argList) {
        if (argList == null || argList.size() == 0)
            return stepName;
        return String.format(stepName, argList.toArray());
    }
}
