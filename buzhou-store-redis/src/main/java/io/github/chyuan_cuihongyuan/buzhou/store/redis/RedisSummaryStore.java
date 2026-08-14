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

    /**
     * impl-37 / spec 13 §stores-6：旧版本修剪——SCAN 枚举会话（{@code sum:<sid>:seq} 键形状），
     * 每会话保留最近 keepLatest 版（ZSET 逆序），其余删正文 + 索引项。
     */
    @Override
    public int pruneVersions(int keepLatest) {
        int keep = Math.max(1, keepLatest);
        var c = sync.commands();
        int deleted = 0;
        List<String> seqKeys = new ArrayList<>();
        io.lettuce.core.ScanArgs match = io.lettuce.core.ScanArgs.Builder
                .matches(keys.summarySeqScanPattern()).limit(100);
        io.lettuce.core.ScanCursor cursor = io.lettuce.core.ScanCursor.INITIAL;
        do {
            io.lettuce.core.KeyScanCursor<String> page = c.scan(cursor, match);
            if (page.getKeys() != null) {
                seqKeys.addAll(page.getKeys());
            }
            cursor = page;
        } while (!cursor.isFinished());
        for (String seqKey : seqKeys) {
            String sessionId = keys.sessionIdFromSummarySeqKey(seqKey);
            List<String> versions = c.zrevrange(keys.summaryVersions(sessionId), 0, -1); // 新→旧
            if (versions == null || versions.size() <= keep) {
                continue;
            }
            for (String version : versions.subList(keep, versions.size())) {
                c.del(keys.summaryVersion(sessionId, Long.parseLong(version)));
                c.zrem(keys.summaryVersions(sessionId), version);
                deleted++;
            }
        }
        return deleted;
    }
}
