package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouConfigSnapshotEndpoint;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 配置漂移审计（spec 414 / T719，ArgoCD drift detection 借鉴）：SmartLifecycle
 * 周期快照 {@code buzhou.*} 全属性（EnumerablePropertySource——343 同法；末段
 * 掩码同一判定宁掩勿漏）→ 与上轮 diff → 变更经 listener 回调（宿主接
 * webhook/日志/告警）+ 计数。基线首拍建立（首拍零事件——基线不是漂移）；
 * 独立调度——关审计不影响任何机制。轮询对账两栖（plain boot 与 cloud 均工作）。
 */
public final class ConfigDriftAuditor implements org.springframework.context.SmartLifecycle {

    /** 一处漂移（值均经掩码）。 */
    public record Change(String key, String from, String to, Instant at) {
    }

    private static final String UNSET = "(unset)";

    private final Environment environment;
    private final Duration interval;
    private final Consumer<List<Change>> listener;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ScheduledExecutorService scheduler;
    private volatile Map<String, String> baseline;

    public ConfigDriftAuditor(Environment environment, Duration interval,
            Consumer<List<Change>> listener) {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval 为正");
        }
        this.environment = environment;
        this.interval = interval;
        this.listener = listener == null ? c -> { } : listener;
    }

    /** 手动对账一轮（测试确定性——不依赖调度节拍）；返回本轮变更。 */
    public List<Change> pollOnce(Instant at) {
        Map<String, String> current = snapshot();
        Map<String, String> previous = baseline;
        baseline = current;
        if (previous == null) {
            return List.of(); // 首拍基线——不是漂移
        }
        List<Change> changes = new ArrayList<>();
        for (Map.Entry<String, String> e : current.entrySet()) {
            String old = previous.get(e.getKey());
            if (old == null) {
                changes.add(new Change(e.getKey(), UNSET, e.getValue(), at));
            } else if (!old.equals(e.getValue())) {
                changes.add(new Change(e.getKey(), old, e.getValue(), at));
            }
        }
        for (String gone : previous.keySet()) {
            if (!current.containsKey(gone)) {
                changes.add(new Change(gone, previous.get(gone), UNSET, at));
            }
        }
        if (!changes.isEmpty()) {
            for (Change ignored : changes) {
                BuzhouMetricsHolder.metrics().counter("buzhou.config.changed");
            }
            listener.accept(List.copyOf(changes));
        }
        return List.copyOf(changes);
    }

    /** 当前生效快照（掩码后）。 */
    public Map<String, String> snapshot() {
        Map<String, String> snapshot = new TreeMap<>();
        if (!(environment instanceof ConfigurableEnvironment configurable)) {
            return snapshot;
        }
        for (PropertySource<?> source : configurable.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String name : enumerable.getPropertyNames()) {
                if (!name.startsWith("buzhou.")) {
                    continue;
                }
                Object value = environment.getProperty(name);
                if (value != null) {
                    snapshot.put(name, BuzhouConfigSnapshotEndpoint.maskIfNeeded(
                            name, String.valueOf(value)));
                }
            }
        }
        return snapshot;
    }

    // ---- SmartLifecycle ----

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory
                            .platform("buzhou-config-audit"));
            scheduler.scheduleAtFixedRate(() -> pollOnce(Instant.now()),
                    interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            ScheduledExecutorService s = scheduler;
            scheduler = null;
            if (s != null) {
                s.shutdownNow();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
