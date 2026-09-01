package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 310 / impl-333：空闲会话后台压缩回归——空闲 ACTIVE 压到 / 新鲜与
 * CLOSED 不动 / 批上限 / 失败隔离与计数 / 构造校验。
 */
class IdleCompactionHousekeeperTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    /** 内存索引（list 按 lastActiveAt 倒序——与 SPI 契约一致）。 */
    private static final class RecordingIndex implements SessionIndexStore {
        final Map<String, SessionInfo> rows = new LinkedHashMap<>();

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
            List<SessionInfo> matched = rows.values().stream()
                    .filter(r -> query.status() == null || query.status().equals(r.status()))
                    .sorted(Comparator.comparingLong(SessionInfo::lastActiveAtEpochMs).reversed())
                    .toList();
            return matched.stream()
                    .skip(query.offset())
                    .limit(query.limit())
                    .toList();
        }

        @Override
        public void delete(String sessionId) {
            rows.remove(sessionId);
        }
    }

    private static SessionInfo session(String id, String status, long lastActiveEpochMs) {
        return new SessionInfo(id, "app", "agent", status, lastActiveEpochMs - 3_600_000,
                lastActiveEpochMs, 5, java.util.Map.of());
    }

    private IdleCompactionHousekeeper housekeeper(RecordingIndex index,
            java.util.function.Function<String, ManualCompactor.CompactResult> action, int maxPerSweep) {
        return new IdleCompactionHousekeeper(index, action,
                Duration.ofHours(1), Duration.ofMinutes(10), maxPerSweep);
    }

    private static ManualCompactor.CompactResult folded(int messages) {
        return new ManualCompactor.CompactResult(false, messages, 1, 4, 1, 100, null);
    }

    @Test
    void compactsIdleActiveSessionsOnly() {
        RecordingIndex index = new RecordingIndex();
        long nowMs = NOW.toEpochMilli();
        index.upsert(session("idle-1", SessionInfo.STATUS_ACTIVE, nowMs - Duration.ofHours(2).toMillis()));
        index.upsert(session("fresh-1", SessionInfo.STATUS_ACTIVE, nowMs - Duration.ofMinutes(5).toMillis()));
        index.upsert(session("closed-idle", SessionInfo.STATUS_CLOSED, nowMs - Duration.ofHours(3).toMillis()));
        ConcurrentLinkedQueue<String> compacted = new ConcurrentLinkedQueue<>();
        IdleCompactionHousekeeper keeper = housekeeper(index,
                id -> {
                    compacted.add(id);
                    return folded(12);
                }, 8);

        int done = keeper.sweepOnce(NOW);

        assertThat(done).isEqualTo(1);
        assertThat(compacted).containsExactly("idle-1");
        assertThat(keeper.compactedCount()).isEqualTo(1);
        assertThat(keeper.foldedMessagesTotal()).isEqualTo(12);
    }

    @Test
    void boundsPerSweepAndPrefersLongestIdle() {
        RecordingIndex index = new RecordingIndex();
        long nowMs = NOW.toEpochMilli();
        for (int i = 1; i <= 5; i++) {
            index.upsert(session("idle-" + i, SessionInfo.STATUS_ACTIVE,
                    nowMs - Duration.ofHours(i + 1).toMillis())); // idle-5 最久
        }
        ConcurrentLinkedQueue<String> compacted = new ConcurrentLinkedQueue<>();
        IdleCompactionHousekeeper keeper = housekeeper(index,
                id -> {
                    compacted.add(id);
                    return folded(1);
                }, 2);

        assertThat(keeper.sweepOnce(NOW)).isEqualTo(2);
        assertThat(compacted).containsExactly("idle-5", "idle-4"); // 最久优先
    }

    @Test
    void failureIsolatedPerSession() {
        RecordingIndex index = new RecordingIndex();
        long nowMs = NOW.toEpochMilli();
        index.upsert(session("boom", SessionInfo.STATUS_ACTIVE, nowMs - Duration.ofHours(2).toMillis()));
        index.upsert(session("fine", SessionInfo.STATUS_ACTIVE, nowMs - Duration.ofHours(3).toMillis()));
        ConcurrentLinkedQueue<String> compacted = new ConcurrentLinkedQueue<>();
        IdleCompactionHousekeeper keeper = housekeeper(index, id -> {
            if ("boom".equals(id)) {
                throw new IllegalStateException("compactor down");
            }
            compacted.add(id);
            return folded(3);
        }, 8);

        assertThat(keeper.sweepOnce(NOW)).isEqualTo(1);
        assertThat(compacted).containsExactly("fine");
        assertThat(keeper.failedCount()).isEqualTo(1);
    }

    @Test
    void constructorValidatesArguments() {
        RecordingIndex index = new RecordingIndex();
        assertThatThrownBy(() -> new IdleCompactionHousekeeper(index, id -> folded(1),
                Duration.ZERO, Duration.ofMinutes(10), 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdleCompactionHousekeeper(index, id -> folded(1),
                Duration.ofHours(1), Duration.ofMinutes(10), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skippedResultCountedNotFailed() {
        RecordingIndex index = new RecordingIndex();
        long nowMs = NOW.toEpochMilli();
        index.upsert(session("nothing-to-fold", SessionInfo.STATUS_ACTIVE,
                nowMs - Duration.ofHours(2).toMillis()));
        IdleCompactionHousekeeper keeper = housekeeper(index,
                id -> new ManualCompactor.CompactResult(true, 0, 0, 0, 0, 0, null), 8);

        assertThat(keeper.sweepOnce(NOW)).isEqualTo(1);
        assertThat(keeper.skippedCount()).isEqualTo(1);
        assertThat(keeper.compactedCount()).isZero();
    }
}
