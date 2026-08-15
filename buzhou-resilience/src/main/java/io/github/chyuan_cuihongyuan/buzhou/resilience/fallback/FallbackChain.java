package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;

import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    /** 金丝雀首选落点事件（spec 48 §B / T175；sessionId + model，每会话一次）。 */
    public static final String EVENT_CANARY_SELECTED = "canary.selected";

    private final List<NamedFallbackModel> models;
    private final List<String> triggerCategories;
    /** spec 48 §B / T175：金丝雀开关与候选权重（配置态；null=默认关/权重 1）。 */
    private final boolean canaryEnabled;
    private final Map<String, Integer> weights;

    public FallbackChain(List<NamedFallbackModel> models, ResilienceProperties.Fallback config) {
        this.models = models == null ? List.of() : List.copyOf(models);
        this.triggerCategories = (config == null ? new ResilienceProperties.Fallback(null, null) : config)
                .triggerCategories().stream()
                .map(c -> c.toUpperCase(Locale.ROOT))
                .toList();
        this.canaryEnabled = config != null && Boolean.TRUE.equals(config.canaryEnabled());
        Map<String, Integer> normalized = new java.util.LinkedHashMap<>();
        if (config != null && config.weights() != null) {
            config.weights().forEach((name, w) -> {
                if (w != null && w > 0) {
                    normalized.put(name, w);
                }
            });
        }
        this.weights = Map.copyOf(normalized);
    }

    /** 是否存在可用备模型。 */
    public boolean isEmpty() {
        return models.isEmpty();
    }

    /** 有序备模型列表（调用方按序逐个尝试）。 */
    public List<NamedFallbackModel> models() {
        return models;
    }

    /** spec 48 §B / T175：金丝雀是否启用。 */
    public boolean canaryEnabled() {
        return canaryEnabled;
    }

    /** spec 48 §B / T175：候选权重（未列名默认 1）。 */
    public int weightOf(String name) {
        return weights.getOrDefault(name, 1);
    }

    /** 按名取备模型条目（不存在返回 null）。 */
    public NamedFallbackModel byName(String name) {
        for (NamedFallbackModel m : models) {
            if (m.name().equals(name)) {
                return m;
            }
        }
        return null;
    }

    /**
     * spec 48 §B / T175：初始目标稳定加权选择——候选池 = [主模型 + 备模型]，累计权重区间上取
     * {@code hash(sessionId) % total}（String.hashCode；算法钉住不换——换算法 = 存量会话全体
     * 漂移一次）。LiteLLM simple-shuffle 的随机加权收窄为会话稳定（同会话不漂移）。
     * 未启用/无备/无 sessionId 返回主模型名。
     */
    public String selectInitialTarget(String primaryName, String sessionId) {
        if (!canaryEnabled || models.isEmpty() || sessionId == null) {
            return primaryName;
        }
        int total = weightOf(primaryName);
        for (NamedFallbackModel m : models) {
            total += weightOf(m.name());
        }
        int slot = Math.floorMod(sessionId.hashCode(), total);
        int acc = weightOf(primaryName);
        if (slot < acc) {
            return primaryName;
        }
        for (NamedFallbackModel m : models) {
            acc += weightOf(m.name());
            if (slot < acc) {
                return m.name();
            }
        }
        return primaryName; // 防御（数学上不达）
    }

    /** 主模型终态类别是否触发降级（CIRCUIT_OPEN 恒触发，由 advisor 判定，不经本方法）。 */
    public boolean triggers(String category) {
        return category != null && triggerCategories.contains(category.toUpperCase(Locale.ROOT));
    }
}
