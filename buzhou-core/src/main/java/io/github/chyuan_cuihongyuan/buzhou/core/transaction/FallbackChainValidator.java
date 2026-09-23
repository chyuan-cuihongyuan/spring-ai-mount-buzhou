package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 降级链配置校验（spec 1922 / T3045 / impl 1523）——Resilience4j/
 * LiteLLM 降级链惯例的静态校验：主模型非空、备链非空、备链无重复、
 * 主模型不在备链中。配置错误启动期拦住——首次降级那一刻才暴雷是
 * 最坏时机。
 *
 * <p>纯函数零状态；一次校验收集全部错误（修一次到位）。
 */
public final class FallbackChainValidator {

    private FallbackChainValidator() {
    }

    /**
     * 校验降级链配置，返回错误列表（空 = 合法）。契约：fallbacks
     * 可为 null（判空规则拦）；模型名大小写敏感精确匹配（口径归
     * 调用方）。
     */
    public static List<String> validate(String primary, String[] fallbacks) {
        List<String> errors = new ArrayList<>();
        if (primary == null || primary.isBlank()) {
            errors.add("primary 为空");
        }
        if (fallbacks == null || fallbacks.length == 0) {
            errors.add("fallbacks 为空");
            return errors;
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < fallbacks.length; i++) {
            String f = fallbacks[i];
            if (f == null || f.isBlank()) {
                errors.add("fallbacks[" + i + "] 为空");
            } else if (!seen.add(f)) {
                errors.add("fallbacks 重复：" + f + "（下标 " + i + "）");
            }
            if (f != null && f.equals(primary)) {
                errors.add("主模型出现在备链：" + f + "（下标 " + i + "）");
            }
        }
        return errors;
    }

    /**
     * 便捷判定：错误列表为空即合法。
     */
    public static boolean isSane(String primary, String[] fallbacks) {
        return validate(primary, fallbacks).isEmpty();
    }
}
