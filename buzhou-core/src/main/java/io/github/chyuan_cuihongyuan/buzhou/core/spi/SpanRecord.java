package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Map;

/**
 * 观测 span 持久化记录（ObservabilityStore span 族载荷）。
 */
public record SpanRecord(
        String spanId,
        String parentSpanId,
        String sessionId,
        int turnSeq,
        String kind,
        String name,
        Instant startedAt,
        Instant endedAt,
        String status,
        Map<String, Object> attributes) {

    public SpanRecord {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /** 活动时刻：endedAt 兜底 startedAt（RUNNING 中间态未关闭）；两参均空返回 null。 */
    public Instant activityAt() {
        return endedAt == null ? startedAt : endedAt;
    }
}
