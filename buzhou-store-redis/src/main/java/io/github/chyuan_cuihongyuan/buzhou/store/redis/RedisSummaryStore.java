package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Redis {@link SummaryStore}：版本号经 {@code INCR} 单调递增（避免 read-then-write 竞态）；
 * 每版本正文存 STRING，版本索引 ZSET（score=version）。
 *
 * <p>latest/history 经 ZSET 逆序取版本号再回读正文。
 */
public class RedisSummaryStore implements SummaryStore {

    private final RedisSync sync;
    private final RedisKeys keys;

    public RedisSummaryStore(RedisSync sync, RedisKeys keys) {
        this.sync = sync;
        this.keys = keys;
    }

    @Override
    public long save(String sessionId, StructuredSummary summary) {
        var c = sync.commands();
        long version = c.incr(keys.summarySeq(sessionId));
        StructuredSummary stored = new StructuredSummary(summary.sessionId(), version,
                summary.sections(), summary.tokenEstimate(), summary.createdAt());
        c.set(keys.summaryVersion(sessionId, version), RedisJson.write(stored));
        c.zadd(keys.summaryVersions(sessionId), version, Long.toString(version));
        return version;
    }

    @Override
    public Optional<StructuredSummary> latest(String sessionId) {
        List<String> top = sync.commands().zrevrange(keys.summaryVersions(sessionId), 0, 0);
        if (top == null || top.isEmpty()) {
            return Optional.empty();
        }
        return readVersion(sessionId, top.get(0));
    }

    @Override
    public List<StructuredSummary> history(String sessionId, int limit) {
        List<String> versions = sync.commands().zrevrange(keys.summaryVersions(sessionId), 0,
                Math.max(0, limit - 1));
        List<StructuredSummary> out = new ArrayList<>();
        if (versions != null) {
            for (String v : versions) {
                readVersion(sessionId, v).ifPresent(out::add);
            }
        }
        return out;
    }

    private Optional<StructuredSummary> readVersion(String sessionId, String versionStr) {
        long version = Long.parseLong(versionStr);
        return Optional.ofNullable(RedisJson.read(
                sync.commands().get(keys.summaryVersion(sessionId, version)), StructuredSummary.class));
    }

    /** impl-35 / spec 13 §stores-6：按会话键集删——ZSET 枚举版本删正文，再删索引与计数器。幂等。 */
    @Override
    public void deleteSession(String sessionId) {
        var c = sync.commands();
        List<String> versions = c.zrange(keys.summaryVersions(sessionId), 0, -1);
        if (versions != null && !versions.isEmpty()) {
            String[] versionKeys = versions.stream()
                    .map(v -> keys.summaryVersion(sessionId, Long.parseLong(v)))
                    .toArray(String[]::new);
            c.del(versionKeys);
        }
        c.del(keys.summaryVersions(sessionId), keys.summarySeq(sessionId));
    }
}
