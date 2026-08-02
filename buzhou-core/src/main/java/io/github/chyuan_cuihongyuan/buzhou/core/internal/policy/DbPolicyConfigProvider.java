package io.github.chyuan_cuihongyuan.buzhou.core.internal.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyChangeListener;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DbPolicyConfigProvider implements PolicyConfigProvider, AutoCloseable {

    private final BindingPolicyStore store;
    private final Duration pollInterval;
    private final List<BindingPolicyChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Long> knownVersions = new ConcurrentHashMap<>();
    private final Map<String, WatchedKey> watched = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final AtomicBoolean started = new AtomicBoolean();

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
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
