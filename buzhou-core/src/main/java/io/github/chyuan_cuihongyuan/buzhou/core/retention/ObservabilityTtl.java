package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import java.time.Duration;

/**
 * impl-37 / spec 13 §stores-6：观测数据 TTL（ClickHouse 49.2K★ merge_with_ttl_timeout 语义
 * ——低频兑现、批删限量）。可再生流水（events/spans/snapshots）按时间过期删除，
 * 与会话级保留（{@link SessionHistoryPolicy}）正交：TTL 只删旧记录、不删会话。
 *
 * @param ttl       保留 TTL；null/非正 → 默认 PT7D
 * @param batchSize 单次批删限量（摊还单周期成本）；null/≤0 → 默认 500
 */
public record ObservabilityTtl(Duration ttl, Integer batchSize) {

    public static final Duration DEFAULT_TTL = Duration.ofDays(7);
    public static final int DEFAULT_BATCH_SIZE = 500;

    public ObservabilityTtl {
        ttl = ttl == null || ttl.isZero() || ttl.isNegative() ? DEFAULT_TTL : ttl;
        batchSize = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }
}
