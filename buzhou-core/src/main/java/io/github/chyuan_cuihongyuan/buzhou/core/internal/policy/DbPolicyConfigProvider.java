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

    /** 连续轮询失败达到该次数后，日志由 WARN 升级为 ERROR（告警事件）。 */
    private static final int POLL_FAILURE_ERROR_THRESHOLD = 3;
    /** impl-34：指数退避上限（pollInterval × 2^6，防故障期忙轮询打爆下游）。 */
    private static final int BACKOFF_CAP_POWER = 6;

    private final BindingPolicyStore store;
    private final Duration pollInterval;
    private final List<BindingPolicyChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Long> knownVersions = new ConcurrentHashMap<>();
    private final Map<String, WatchedKey> watched = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(
                    io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory.virtual("policy-poll"));
    private final AtomicBoolean started = new AtomicBoolean();
    /** impl-34 / spec 13 §core-4：连续失败计数（成功一次即清零；驱动指数退避与告警级别）。 */
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
            // impl-34：固定节奏 scheduleWithFixedDelay 改自排程——失败时指数退避（重试风暴防线）
            scheduler.schedule(this::pollSafely, pollInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** 成功 → 节奏回归 pollInterval；失败 → pollInterval × 2^min(failures, 6) 退避。 */
    private void scheduleNext(int failures) {
        long delayMillis = pollInterval.toMillis();
        if (failures > 0) {
            long factor = 1L << Math.min(failures, BACKOFF_CAP_POWER);
            delayMillis = pollInterval.toMillis() * factor;
        }
        scheduler.schedule(this::pollSafely, delayMillis, TimeUnit.MILLISECONDS);
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
            scheduleNext(0);
        } catch (RuntimeException e) {
            // ticket 29 日志基线 + impl-34 指数退避：WARN 起报，连续失败达阈值升级 ERROR；
            // 下一轮延迟 = pollInterval × 2^min(failures, 6)（重试风暴防线，成功即清零）
            int failures = consecutivePollFailures.incrementAndGet();
            if (failures >= POLL_FAILURE_ERROR_THRESHOLD) {
                LOG.error("策略配置轮询连续失败 {} 次（已达阈值 {}），退避 {}ms 后重试；策略热更新可能停摆，请检查 BindingPolicyStore 可用性",
                        failures, POLL_FAILURE_ERROR_THRESHOLD, backoffMillis(failures), e);
            } else {
                LOG.warn("策略配置轮询失败（连续第 {} 次，退避 {}ms 后重试）", failures,
                        backoffMillis(failures), e);
            }
            scheduleNext(failures);
        }
    }

    private long backoffMillis(int failures) {
        long base = pollInterval.toMillis() * (1L << Math.min(failures, BACKOFF_CAP_POWER));
        // spec 50 §B / T179 / impl-148：±25% 抖动（防多实例同相位重试雷鸣羊群；0.75~1.25×base）
        double factor = 0.75 + 0.5 * java.util.concurrent.ThreadLocalRandom.current().nextDouble();
        return Math.round(base * factor);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
