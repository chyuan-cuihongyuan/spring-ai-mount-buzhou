package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 会话特征仓（spec 161 / T519，Feast feature store 借鉴）：per-session 原始
 * 行为计数（turns/toolCalls/toolErrors/modelErrors/lastActiveAt），LRU 1024
 * 封顶逐出最久未活跃；<b>比率查询时派生</b>（零陈旧）。采集由
 * {@link SessionFeaturesHook} 四点自动累积。
 */
public final class SessionFeatureStore {

    /** 会话特征行（比率派生方法内含）。 */
    public record Features(long turns, long toolCalls, long toolErrors,
                           long modelErrors, Instant lastActiveAt) {

        /** 工具错误率（零调用 = 0——不 NaN）。 */
        public double toolErrorRate() {
            return toolCalls == 0 ? 0.0 : (double) toolErrors / toolCalls;
        }

        /** 模型错误率（轮数口径——onModelError 每终态失败一次）。 */
        public double modelErrorRate() {
            return turns == 0 ? 0.0 : (double) modelErrors / turns;
        }
    }

    private static final class MutableFeatures {
        long turns;
        long toolCalls;
        long toolErrors;
        long modelErrors;
        Instant lastActiveAt;
    }

    private static final int MAX_SESSIONS = 1024;

    private final LinkedHashMap<String, MutableFeatures> sessions =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, MutableFeatures> eldest) {
                    return size() > MAX_SESSIONS;
                }
            };

    /** 记一轮开始（turns++ + lastActive 触达）。 */
    public synchronized void recordTurnStart(String sessionId) {
        MutableFeatures features = sessions.computeIfAbsent(sessionId, k -> new MutableFeatures());
        features.turns++;
        features.lastActiveAt = Instant.now();
    }

    /** 记一次工具调用（isErrorFeedback = 结构化标记的错误反馈）。 */
    public synchronized void recordToolCall(String sessionId, boolean isErrorFeedback) {
        MutableFeatures features = sessions.computeIfAbsent(sessionId, k -> new MutableFeatures());
        features.toolCalls++;
        if (isErrorFeedback) {
            features.toolErrors++;
        }
        features.lastActiveAt = Instant.now();
    }

    /** 记一次模型终态失败。 */
    public synchronized void recordModelError(String sessionId) {
        MutableFeatures features = sessions.computeIfAbsent(sessionId, k -> new MutableFeatures());
        features.modelErrors++;
        features.lastActiveAt = Instant.now();
    }

    /** 特征查询（无记录 = 零值行——消费方统一口径）。 */
    public synchronized Features features(String sessionId) {
        MutableFeatures features = sessions.get(sessionId);
        if (features == null) {
            return new Features(0, 0, 0, 0, null);
        }
        sessions.get(sessionId); // accessOrder 触达
        return new Features(features.turns, features.toolCalls, features.toolErrors,
                features.modelErrors, features.lastActiveAt);
    }

    /** 全量快照（稳定序）。 */
    public synchronized Map<String, Features> snapshot() {
        Map<String, Features> out = new TreeMap<>();
        sessions.forEach((id, f) -> out.put(id,
                new Features(f.turns, f.toolCalls, f.toolErrors, f.modelErrors, f.lastActiveAt)));
        return out;
    }

    /** 驻留会话数（观测面）。 */
    public synchronized int size() {
        return sessions.size();
    }
}
