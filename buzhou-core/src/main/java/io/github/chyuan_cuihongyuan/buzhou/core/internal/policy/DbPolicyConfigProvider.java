package io.github.chyuan_cuihongyuan.buzhou.core.internal.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyChangeListener;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DbPolicyConfigProvider implements PolicyConfigProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DbPolicyConfigProvider.class);

    /** 连续轮询失败达到该次数后，日志由 WARN 升级为 ERROR（简单退避：不改轮询节奏本身）。 */
    private static final int POLL_FAILURE_ERROR_THRESHOLD = 3;

    private final BindingPolicyStore store;
    private final Duration pollInterval;
    private final List<BindingPolicyChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Long> knownVersions = new ConcurrentHashMap<>();
    private final Map<String, WatchedKey> watched = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final AtomicBoolean started = new AtomicBoolean();
    /** ticket 29 日志基线：连续失败计数（成功一次即清零；只影响日志级别，不影响轮询节奏）。 */
    private final AtomicInteger consecutivePollFailures = new AtomicInteger();

    private record WatchedKey(String appId, String agentName) {
    }

    public DbPolicyConfigProvider(BindingPolicyStore store, Duration pollInterval) {
        this.store = store;
        this.pollInterval = pollInterval;
    }

    @Override
    public BindingPolicy getBindingPolicy(String appId, String agentName) {
        return store.find(appId, agentName)
                .orElseGet(() -> BindingPolicy.empty(appId, agentName));
    }

    @Override
    public void addChangeListener(BindingPolicyChangeListener listener) {
        listeners.add(listener);
    }

    public void startWatching(String appId, String agentName) {
        watched.put(BindingPolicy.key(appId, agentName), new WatchedKey(appId, agentName));
        knownVersions.put(BindingPolicy.key(appId, agentName),
                getBindingPolicy(appId, agentName).version());
        if (started.compareAndSet(false, true)) {
            scheduler.scheduleWithFixedDelay(this::pollSafely,
                    pollInterval.toMillis(), pollInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void pollSafely() {
        try {
            watched.values().forEach(key -> {
                String mapKey = BindingPolicy.key(key.appId(), key.agentName());
                BindingPolicy current = getBindingPolicy(key.appId(), key.agentName());
                long known = knownVersions.getOrDefault(mapKey, 0L);
                if (current.version() > known) {
                    knownVersions.put(mapKey, current.version());
                    listeners.forEach(listener -> listener.onChange(current));
                }
            });
            consecutivePollFailures.set(0);
        } catch (RuntimeException e) {
            // ticket 29 日志基线：轮询异常不再静默吞——WARN 起报，连续失败达阈值升级 ERROR
            //（简单退避只作用于日志级别，轮询节奏仍由 pollInterval 决定）。
            int failures = consecutivePollFailures.incrementAndGet();
            if (failures >= POLL_FAILURE_ERROR_THRESHOLD) {
                LOG.error("策略配置轮询连续失败 {} 次（已达阈值 {}），策略热更新可能停摆，请检查 BindingPolicyStore 可用性",
                        failures, POLL_FAILURE_ERROR_THRESHOLD, e);
            } else {
                LOG.warn("策略配置轮询失败（连续第 {} 次，达 {} 次升级 ERROR）", failures,
                        POLL_FAILURE_ERROR_THRESHOLD, e);
            }
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
