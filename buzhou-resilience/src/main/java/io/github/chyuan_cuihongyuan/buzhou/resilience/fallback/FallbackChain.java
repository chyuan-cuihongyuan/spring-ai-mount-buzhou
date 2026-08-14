package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;

import java.util.List;
import java.util.Locale;

/**
 * 备模型降级链（spec 15「备模型降级链」，T82 / impl-57）：有序备模型列表 + 触发类别判定。
 * 降级执行逻辑在 {@code ResilienceAdvisor} 内（与其重试 / 熔断记账协同），本类只承载配置语义。
 *
 * <p>触发口径：主模型终态失败 category ∈ trigger-categories（默认 NETWORK/SERVER/TIMEOUT/AUTH）；
 * 熔断 OPEN 恒触发（advisor 侧硬编码，不受本表控制）。
 */
public final class FallbackChain {

    /** 降级切换事件（from/to/category）。 */
    public static final String EVENT_SWITCHED = "fallback.switched";
    /** 全部备模型耗尽事件（from/category）。 */
    public static final String EVENT_EXHAUSTED = "fallback.exhausted";

    private final List<NamedFallbackModel> models;
    private final List<String> triggerCategories;

    public FallbackChain(List<NamedFallbackModel> models, ResilienceProperties.Fallback config) {
        this.models = models == null ? List.of() : List.copyOf(models);
        this.triggerCategories = (config == null ? new ResilienceProperties.Fallback(null, null) : config)
                .triggerCategories().stream()
                .map(c -> c.toUpperCase(Locale.ROOT))
                .toList();
    }

    /** 是否存在可用备模型。 */
    public boolean isEmpty() {
        return models.isEmpty();
    }

    /** 有序备模型列表（调用方按序逐个尝试）。 */
    public List<NamedFallbackModel> models() {
        return models;
    }

    /** 主模型终态类别是否触发降级（CIRCUIT_OPEN 恒触发，由 advisor 判定，不经本方法）。 */
    public boolean triggers(String category) {
        return category != null && triggerCategories.contains(category.toUpperCase(Locale.ROOT));
    }
}
