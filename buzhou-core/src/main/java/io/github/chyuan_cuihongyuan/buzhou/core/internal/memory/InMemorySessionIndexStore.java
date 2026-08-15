package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 会话索引内存实现（spec 30 / T109 / impl-84）：进程内 ConcurrentHashMap，
 * 查询时快照排序——索引量级（活跃会话数）下无热点。重启后索引为空、随会话活动重建
 * （最终一致口径，spec 30）。
 *
 * @since 1.0.0
 */
public class InMemorySessionIndexStore implements SessionIndexStore {

    private final Map<String, SessionInfo> rows = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void upsert(SessionInfo info) {
        rows.put(info.sessionId(), info);
    }

    @Override
    public Optional<SessionInfo> get(String sessionId) {
        return Optional.ofNullable(rows.get(sessionId));
    }

    @Override
    public List<SessionInfo> list(SessionIndexQuery query) {
        lock.readLock().lock();
        try {
            return rows.values().stream()
                    .filter(info -> matches(info, query))
                    .sorted(Comparator.comparingLong(SessionInfo::lastActiveAtEpochMs).reversed()
                            .thenComparing(SessionInfo::sessionId))
                    .skip(query.offset())
                    .limit(query.limit())
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void delete(String sessionId) {
        rows.remove(sessionId);
    }

    /** spec 37 §C / T134：过期 CLOSED/DELETED 淘汰（ACTIVE 永不扫）。 */
    @Override
    public int purgeOlderThan(java.time.Instant cutoff, int limit) {
        int purged = 0;
        for (String id : rows.keySet()) {
            if (purged >= limit) {
                break;
            }
            SessionInfo info = rows.get(id);
            if (info != null && !SessionInfo.STATUS_ACTIVE.equals(info.status())
                    && info.lastActiveAtEpochMs() < cutoff.toEpochMilli()) {
                rows.remove(id);
                purged++;
            }
        }
        return purged;
    }

    private static boolean matches(SessionInfo info, SessionIndexQuery q) {
        if (q.appId() != null && !q.appId().equals(info.appId())) {
            return false;
        }
        if (q.agentName() != null && !q.agentName().equals(info.agentName())) {
            return false;
        }
        if (q.status() == null ? SessionInfo.STATUS_DELETED.equals(info.status())
                : !q.status().equals(info.status())) {
            return false; // 默认排除 DELETED（审计行仅显式过滤可见——spec 33 §B）
        }
        if (q.tagKey() != null && !q.tagValue().equals(info.tags().get(q.tagKey()))) {
            return false;
        }
        return true;
    }
}
