package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class LongContentParams {

    private LongContentParams() {
    }

    public static List<LongContentParamPair> scan(Method method) {
        Set<String> paramNames = Arrays.stream(method.getParameters())
                .map(Parameter::getName)
                .collect(Collectors.toSet());
        List<LongContentParamPair> pairs = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            LongContentParam annotation = param.getAnnotation(LongContentParam.class);
            if (annotation == null) {
                continue;
            }
            String pathParam = resolvePathParam(param.getName(), annotation.pathParam(), paramNames);
            if (pathParam != null) {
                pairs.add(new LongContentParamPair(param.getName(), pathParam));
            }
        }
        return List.copyOf(pairs);
    }

    private static String resolvePathParam(String contentName, String explicit, Set<String> paramNames) {
        if (explicit != null && !explicit.isBlank()) {
            return paramNames.contains(explicit) ? explicit : null;
        }
        String base = contentName.endsWith("Content") && contentName.length() > "Content".length()
                ? contentName.substring(0, contentName.length() - "Content".length())
                : contentName;
        for (String candidate : List.of(base + "Path", base + "FilePath")) {
            if (paramNames.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
