package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由阶段注册表（spec 723 / T997，MLflow model stages / argo-rollouts canary
 * 借鉴）：route → Stage{STABLE, CANARY, ARCHIVED}——端点生命周期的显式声明。
 * 纯函数 {@link #filter} 在 WeightedChatModel <b>构造前</b>按可见阶段集过滤
 * 候选权重表（与「候选面构造期定死」裁决一致——阶段变更 = 重新构造路由）。
 *
 * <p>未标注路由恒可见（默认零变化）；注册表有界（{@value #CAP}——基数纪律）。
 */
public final class RouteStages {

    /** 生命周期阶段。 */
    public enum Stage { STABLE, CANARY, ARCHIVED }

    /** 注册表容量（基数有界纪律）。 */
    public static final int CAP = 64;

    private final Map<String, Stage> stages = new ConcurrentHashMap<>();

    /** 标注路由阶段（超限拒绝 false；幂等覆盖 true）。 */
    public boolean tag(String route, Stage stage) {
        if (route == null || route.isBlank() || stage == null) {
            return false;
        }
        if (!stages.containsKey(route) && stages.size() >= CAP) {
            return false;
        }
        stages.put(route, stage);
        return true;
    }

    /** 路由阶段（未标注 = null——恒可见）。 */
    public Stage stageOf(String route) {
        return route == null ? null : stages.get(route);
    }

    /** 阶段视图（不可变快照）。 */
    public Map<String, Stage> view() {
        return Map.copyOf(stages);
    }

    /**
     * 构造期选型过滤：stage 不在 visible 集的候选剔除；未标注恒保留。
     * 剔除发 {@code buzhou.routing.stage-filtered}（tag stage）计数。
     */
    public static Map<String, Integer> filter(Map<String, Integer> weights,
                                              RouteStages stages, Set<Stage> visible) {
        if (weights == null || visible == null) {
            throw new IllegalArgumentException("weights/visibleStages 非空");
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        weights.forEach((route, weight) -> {
            Stage stage = stages == null ? null : stages.stageOf(route);
            if (stage == null || visible.contains(stage)) {
                out.put(route, weight);
            } else {
                BuzhouMetricsHolder.metrics()
                        .counter("buzhou.routing.stage-filtered", "stage", stage.name());
            }
        });
        return java.util.Collections.unmodifiableMap(out);
    }
}
