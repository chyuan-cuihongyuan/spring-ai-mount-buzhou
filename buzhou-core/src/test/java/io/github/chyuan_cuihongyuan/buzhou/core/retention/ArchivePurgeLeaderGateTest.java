package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 341 / impl-364：ArchivePurgeJob 选主门回归——非 leader 跳周期留
 * 计数 / 手动 purgeOnce 不设门 / stop 让位。331 同手法（stub elector）。
 */
class ArchivePurgeLeaderGateTest {

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

    /** 真实 SessionArchiver（final 类）+ 内存 stores：铺过期归档条目（每轮真实可删）。 */
    private static SessionArchiver realArchiverWithExpiringArchive() {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores =
                io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        // 值 = ArchiveEntry JSON（archivedAt 30 天前——TTL 7 天即过期）
        String entryJson = "{\"sessionId\":\"expired-session\",\"archivedAt\":\""
                + Instant.now().minus(Duration.ofDays(30)) + "\",\"messages\":[],\"states\":{}}";
        stores.sessionStateStore().put(
                io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver.ARCHIVE_SESSION_ID,
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver.ARCHIVE_PREFIX
                                + "expired-session",
                        entryJson, "test", 0, null,
                        Instant.now().minus(Duration.ofDays(30))));
        return new SessionArchiver(stores, null);
    }

    @Test
    void nonLeaderSkipsTicks_leavesCounterTrace() throws InterruptedException {
        StubElector elector = new StubElector();
        elector.leader = false;
        List<Integer> rounds = new CopyOnWriteArrayList<>(); // purge 监听计轮（0 也通知）
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
        ArchivePurgeJob job = new ArchivePurgeJob(realArchiverWithExpiringArchive(),
                Duration.ofDays(7), Duration.ofMillis(50), true, null, elector);
        job.addPurgeListener(rounds::add);
        job.start();
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
            while (elector.tries.get() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            Thread.sleep(100); // 留出一个绝不执行的周期
            assertThat(rounds).isEmpty(); // 一轮都没清（监听零通知）
            assertThat(counters).contains("buzhou.archive.skipped-not-leader");
        } finally {
            job.stop();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
        }
    }

    @Test
    void manualPurgeOnceBypassesGate() {
        StubElector elector = new StubElector();
        elector.leader = false;
        ArchivePurgeJob job = new ArchivePurgeJob(realArchiverWithExpiringArchive(),
                Duration.ofDays(7), Duration.ofHours(1), true, null, elector);
        assertThat(job.purgeOnce()).isEqualTo(1); // 手动不设门——过期条目真删
        assertThat(elector.tries.get()).isZero(); // 门根本没被咨询
    }

    @Test
    void stopResignsForFastFailover() {
        StubElector elector = new StubElector();
        ArchivePurgeJob job = new ArchivePurgeJob(realArchiverWithExpiringArchive(),
                Duration.ofDays(7), Duration.ofHours(1), true, null, elector);
        job.start();
        job.stop();
        assertThat(elector.resigns.get()).isEqualTo(1);
    }
}
