package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * 空闲会话水位监控（spec 179 / T551，Flink watermark 借鉴）：消费特征仓
 * （spec 161）lastActiveAt 事实——sweep(now) 判定空闲超阈值的会话（清单按
 * 空闲时长降序），<b>翻转才通知</b>（进入/离开空闲态）；无特征会话不误报。
 *
 * <p>只判定不动作（压缩/归档/排水以本清单为候选——分层诚实）。空闲名册
 * LRU 1024。
 */
public final class IdleSessionMonitor {

    /** 一次 sweep 的空闲行。 */
    public record IdleInfo(String sessionId, long idleMillis) {
    }

    private static final int MAX_ROSTER = 1024;

    private final SessionFeatureStore features;
    private final Duration threshold;
    private final List<BiConsumer<String, Boolean>> listeners = new CopyOnWriteArrayList<>();
    private final LinkedHashMap<String, Boolean> idleRoster = new LinkedHashMap<>(16,
            0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_ROSTER;
        }
    };

    public IdleSessionMonitor(SessionFeatureStore features, Duration threshold) {
        if (threshold == null || threshold.isZero() || threshold.isNegative()) {
            throw new IllegalArgumentException("threshold 为正");
        }
        this.features = features == null ? new SessionFeatureStore() : features;
        this.threshold = threshold;
    }

    /** 翻转监听（Boolean = 是否进入空闲；离开为 false）。 */
    public void onChange(BiConsumer<String, Boolean> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 水位判定一轮：返回空闲超阈值的会话（空闲时长降序——最该处理的最前）；
     * 翻转（进入/离开）通知；进入计数。
     */
    public synchronized List<IdleInfo> sweep(Instant now) {
        List<IdleInfo> idle = new ArrayList<>();
        features.snapshot().forEach((sessionId, f) -> {
            if (f.lastActiveAt() == null) {
                return; // 无活动事实不误报
            }
            long idleMillis = Duration.between(f.lastActiveAt(), now).toMillis();
            if (idleMillis >= threshold.toMillis()) {
                idle.add(new IdleInfo(sessionId, idleMillis));
            }
        });
        idle.sort(Comparator.comparingLong(IdleInfo::idleMillis).reversed());

        java.util.Set<String> idleNow = new java.util.HashSet<>();
        idle.forEach(info -> idleNow.add(info.sessionId()));
        // 进入翻转
        idle.forEach(info -> {
            if (!idleRoster.containsKey(info.sessionId())) {
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                        .metrics().counter("buzhou.idle-session.entered", 1);
                listeners.forEach(l -> l.accept(info.sessionId(), true));
            }
        });
        // 离开翻转
        idleRoster.keySet().forEach(sessionId -> {
            if (!idleNow.contains(sessionId)) {
                listeners.forEach(l -> l.accept(sessionId, false));
            }
        });
        idleRoster.clear();
        idleNow.forEach(sessionId -> idleRoster.put(sessionId, true));
        return idle;
    }
}
