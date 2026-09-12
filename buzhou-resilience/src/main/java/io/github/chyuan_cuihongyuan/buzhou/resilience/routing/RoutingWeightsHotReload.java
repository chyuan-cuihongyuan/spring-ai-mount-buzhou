package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 路由权重热重载（spec 340 / T672，Spring Cloud rebind——320 舱容量同
 * 模式）：收到 {@link BuzhouConfigRefreshEvent} 重读
 * {@code buzhou.routing.weights} → 对已知路逐个 {@link WeightedChatModel#setWeight}
 * ——金丝雀期微调配比不重启。WRR 动量保留（调权不清零，比例自然收敛）。
 *
 * <p><b>诚实边界</b>：候选面构造期定死——面外名字 WARN 跳过不红（环境间
 * yml 差异不炸刷新）；运行时增删路须重启。
 */
public final class RoutingWeightsHotReload implements ApplicationListener<BuzhouConfigRefreshEvent> {

    private static final System.Logger LOGGER =
            System.getLogger(RoutingWeightsHotReload.class.getName());

    private final WeightedChatModel router;
    private final Environment environment;
    private final AtomicLong reloads = new AtomicLong();
    private final RoutingSlowStart slowStart;

    public RoutingWeightsHotReload(WeightedChatModel router, Environment environment) {
        this(router, environment, null);
    }

    /**
     * spec 702 / T955：慢启动变体——权重<b>上调</b>走 {@link RoutingSlowStart}
     * 爬坡（升配不瞬时打爆），下调/持平仍瞬时（降权永远安全）；null（默认）
     * 行为逐字节不变。
     */
    public RoutingWeightsHotReload(WeightedChatModel router, Environment environment,
                                   RoutingSlowStart slowStart) {
        if (router == null || environment == null) {
            throw new IllegalArgumentException("router/environment 必须非空");
        }
        this.router = router;
        this.environment = environment;
        this.slowStart = slowStart;
    }

    @Override
    public void onApplicationEvent(BuzhouConfigRefreshEvent event) {
        Map<String, Integer> weights = Binder.get(environment)
                .bind("buzhou.routing.weights",
                        Bindable.mapOf(String.class, Integer.class))
                .orElse(Map.of());
        weights.forEach((name, weight) -> {
            if (!router.routes().containsKey(name)) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "路由权重热调跳过面外路「{0}」——候选面构造期定死，面变更须重启（已知路：{1}）",
                        name, router.routes().keySet());
                return;
            }
            int target = weight == null ? 1 : Math.max(1, weight);
            if (slowStart != null && target > router.routes().getOrDefault(name, target)) {
                slowStart.ramp(name, target); // 上调走爬坡（spec 702）
                return;
            }
            router.setWeight(name, target);
        });
        reloads.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter("buzhou.routing.reloaded");
    }

    /** 热调次数（观测面）。 */
    public long reloadCount() {
        return reloads.get();
    }
}
