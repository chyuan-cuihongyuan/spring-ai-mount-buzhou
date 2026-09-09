package io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模型并发舱热重载（spec 429 / T750，320/340 rebind 同模式）：收到
 * {@link BuzhouConfigRefreshEvent} 重读 {@code buzhou.resilience.
 * model-concurrency.limits} → {@link ModelConcurrencyLimiter#resize}——
 * SRE 改 yml 发一个事件，供应商调并发额度热生效不重启（在飞不受扰）。
 * acquire-timeout 构造期定死不热改（诚实边界——320 同注记）。
 */
public final class ModelConcurrencyHotReload implements ApplicationListener<BuzhouConfigRefreshEvent> {

    private final ModelConcurrencyLimiter limiter;
    private final Environment environment;
    private final AtomicLong reloads = new AtomicLong();

    public ModelConcurrencyHotReload(ModelConcurrencyLimiter limiter, Environment environment) {
        if (limiter == null || environment == null) {
            throw new IllegalArgumentException("limiter/environment 必须非空");
        }
        this.limiter = limiter;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(BuzhouConfigRefreshEvent event) {
        Map<String, Integer> limits = Binder.get(environment)
                .bind("buzhou.resilience.model-concurrency.limits",
                        Bindable.mapOf(String.class, Integer.class))
                .orElse(Map.of());
        limiter.resize(limits);
        reloads.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter("buzhou.resilience.concurrency-reloaded");
    }

    /** 热重载次数（观测面）。 */
    public long reloadCount() {
        return reloads.get();
    }
}
