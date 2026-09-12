package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.CircuitState;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.CircuitTransitionJournal;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 健康加权路由抑制（spec 703 / T1006，HAProxy agent-check 权重反馈思想）：
 * 模型跳闸 → 路由权重压到地板（默认 1——保留最后通道，panic-threshold 思想
 * 不全零黑洞）；恢复 CLOSED → 权重回声明值。HALF_OPEN 维持地板（探测 trickle）。
 *
 * <p>用法：{@code RoutingHealthDampener.attach(router, breaker, 1)}——挂在
 * 路由/熔断同域装配处。modelName 与路由 beanName 按名相等匹配，未匹配静默
 * 忽略。恢复基线取 attach 时刻 routes() 快照——运行期人工 setWeight 会被
 * 恢复覆盖（诚实边界）。
 */
public final class RoutingHealthDampener {

    private final WeightedChatModel router;
    private final int floorWeight;
    /** 声明权重基线（attach 时刻快照——恢复目标）。 */
    private final Map<String, Integer> declaredWeights;
    /** 当前被压权模型（幂等去重与读数面）。 */
    private final Map<String, Boolean> dampened = new ConcurrentHashMap<>();

    private RoutingHealthDampener(WeightedChatModel router, int floorWeight) {
        this.router = router;
        this.floorWeight = floorWeight;
        this.declaredWeights = new LinkedHashMap<>(router.routes());
    }

    /**
     * 装配原语：注册变迁监听并返回抑制器（读数面 dampened()）。
     *
     * @param router      加权路由（候选 ≥2 路）
     * @param breaker     模型熔断器（变迁源）
     * @param floorWeight 跳闸压权地板（≥1——0 会造成全零黑洞，构造 fail-fast）
     */
    public static RoutingHealthDampener attach(WeightedChatModel router, ModelCircuitBreaker breaker,
                                               int floorWeight) {
        Objects.requireNonNull(router, "router");
        Objects.requireNonNull(breaker, "breaker");
        if (floorWeight < 1) {
            throw new IllegalArgumentException("floorWeight 必须 >= 1（0=全零黑洞，禁止）");
        }
        RoutingHealthDampener dampener = new RoutingHealthDampener(router, floorWeight);
        breaker.addTransitionListener(dampener::onTransition);
        return dampener;
    }

    /** 变迁回调（listener 异常已由 breaker 隔离——本方法内不再抛）。 */
    void onTransition(CircuitTransitionJournal.Transition transition) {
        String model = transition.model();
        if (!declaredWeights.containsKey(model)) {
            return; // 路由候选外——静默忽略（按名匹配不猜映射）
        }
        if (CircuitState.OPEN.name().equals(transition.to())) {
            if (dampened.putIfAbsent(model, Boolean.TRUE) == null) {
                router.setWeight(model, floorWeight);
            }
        } else if (CircuitState.CLOSED.name().equals(transition.to())) {
            if (dampened.remove(model) != null) {
                router.setWeight(model, declaredWeights.get(model));
            }
        }
        // HALF_OPEN：维持地板（探测 trickle——单探测本就是最小流量）
    }

    /** 读数面：当前被压权模型视图（名字 → 地板权重）。 */
    public Map<String, Integer> dampened() {
        Map<String, Integer> view = new LinkedHashMap<>();
        dampened.keySet().forEach(name -> view.put(name, floorWeight));
        return view;
    }
}
