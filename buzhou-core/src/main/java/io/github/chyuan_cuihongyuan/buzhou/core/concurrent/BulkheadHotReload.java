package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 舱容量热重载（spec 320 / T631）：收到 {@link BuzhouConfigRefreshEvent} 重读
 * {@code buzhou.bulkhead.agents} → {@link AgentBulkhead#resize} 全局舱——
 * SRE 改 yml 发一个事件，容量热生效不重启（在飞不受扰）。acquire-timeout
 * 构造期定死不热改（诚实边界）。
 */
public final class BulkheadHotReload implements ApplicationListener<BuzhouConfigRefreshEvent> {

    private final AgentBulkhead bulkhead;
    private final Environment environment;
    private final AtomicLong reloads = new AtomicLong();

    public BulkheadHotReload(AgentBulkhead bulkhead, Environment environment) {
        if (bulkhead == null) {
            throw new IllegalArgumentException("bulkhead 必须非空");
        }
        if (environment == null) {
            throw new IllegalArgumentException("environment 必须非空");
        }
        this.bulkhead = bulkhead;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(BuzhouConfigRefreshEvent event) {
        Map<String, Integer> limits = Binder.get(environment)
                .bind("buzhou.bulkhead.agents",
                        Bindable.mapOf(String.class, Integer.class))
                .orElse(Map.of());
        bulkhead.resize(limits);
        reloads.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter("buzhou.bulkhead.reloaded");
    }

    /** 热重载次数（观测面）。 */
    public long reloadCount() {
        return reloads.get();
    }
}
