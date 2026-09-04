package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 341 / impl-364：空闲压缩选主门回归——非 leader 跳周期留计数 /
 * 手动 sweepOnce 不设门 / stop 让位。331 同手法（stub elector）。
 */
class IdleCompactionLeaderGateTest {

    private static final class StubElector implements LeaderElector {
        final AtomicInteger tries = new AtomicInteger();
        final AtomicInteger resigns = new AtomicInteger();
        volatile boolean leader = true;

        @Override
        public Leadership tryAcquireOrRenew() {
            tries.incrementAndGet();
            return new Leadership(leader ? "me" : "other", leader ? 1 : 0, leader);
        }

        @Override
        public void resign() {
            resigns.incrementAndGet();
        }
    }

    /** 空索引（无候选——门行为与候选无关；list 契约同 IdleCompactionHousekeeperTest）。 */
    private static final class EmptyIndex implements SessionIndexStore {
        @Override
        public void upsert(SessionInfo info) {
        }

        @Override
        public Optional<SessionInfo> get(String sessionId) {
            return Optional.empty();
        }

        @Override
        public List<SessionInfo> list(SessionIndexQuery query) {
            return List.of();
        }

        @Override
        public void delete(String sessionId) {
        }
    }

    private static ManualCompactor.CompactResult folded() {
        return new ManualCompactor.CompactResult(false, 1, 1, 4, 1, 100, null);
    }

    @Test
    void nonLeaderSkipsTicks_leavesCounterTrace() throws InterruptedException {
        StubElector elector = new StubElector();
        elector.leader = false;
        ConcurrentLinkedQueue<String> compacted = new ConcurrentLinkedQueue<>();
        List<String> counters = new CopyOnWriteArrayList<>();
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(
                new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics() {
                    @Override
                    public void counter(String name, long delta, String... tagKeyValue) {
                        counters.add(name);
                    }

                    @Override
                    public void timer(String name, Duration duration, String... tagKeyValue) {
                    }
                });
        IdleCompactionHousekeeper keeper = new IdleCompactionHousekeeper(new EmptyIndex(),
                id -> {
                    compacted.add(id);
                    return folded();
                }, Duration.ofHours(1), Duration.ofMillis(50), 4, elector);
        keeper.start();
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
            while (elector.tries.get() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            Thread.sleep(100); // 留出一个绝不执行的周期
            assertThat(compacted).isEmpty();
            assertThat(counters).contains("buzhou.idle-compaction.skipped-not-leader");
        } finally {
            keeper.stop();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
        }
    }

    @Test
    void manualSweepOnceBypassesGate() {
        StubElector elector = new StubElector();
        elector.leader = false;
        ConcurrentLinkedQueue<String> compacted = new ConcurrentLinkedQueue<>();
        IdleCompactionHousekeeper keeper = new IdleCompactionHousekeeper(new EmptyIndex(),
                id -> {
                    compacted.add(id);
                    return folded();
                }, Duration.ofHours(1), Duration.ofMinutes(10), 4, elector);
        assertThat(keeper.sweepOnce(Instant.now())).isZero(); // 手动不设门（空候选 0 轮）
        assertThat(elector.tries.get()).isZero(); // 门根本没被咨询
    }

    @Test
    void stopResignsForFastFailover() {
        StubElector elector = new StubElector();
        IdleCompactionHousekeeper keeper = new IdleCompactionHousekeeper(new EmptyIndex(),
                id -> folded(), Duration.ofHours(1), Duration.ofMinutes(10), 4, elector);
        keeper.start();
        keeper.stop();
        assertThat(elector.resigns.get()).isEqualTo(1);
    }
}
