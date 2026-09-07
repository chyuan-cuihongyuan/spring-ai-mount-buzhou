package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryRunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 331 / impl-354：RetentionSweeper 选主门——leader 周期照扫 / 非 leader
 * 跳周期留计数 / 取续异常跳过且调度线程存活（失联宁可少做不可抢做）/
 * stop 主动让位幂等 / 手动 sweepOnce 不设门（运维按钮是人的决定）。
 */
class RetentionSweeperLeaderGateTest {

    private static final Clock CLOCK = Clock.fixed(Instant.now(), ZoneOffset.UTC);

    /** 可编程选主桩：领导态/抛异常可控，计数取续与让位。 */
    private static final class StubElector implements LeaderElector {
        final AtomicInteger tries = new AtomicInteger();
        final AtomicInteger resigns = new AtomicInteger();
        volatile boolean leader = true;
        volatile boolean throwOnTry;

        @Override
        public Leadership tryAcquireOrRenew() {
            tries.incrementAndGet();
            if (throwOnTry) {
                throw new IllegalStateException("redis unreachable");
            }
            return new Leadership(leader ? "me" : "other", leader ? 1 : 0, leader);
        }

        @Override
        public void resign() {
            resigns.incrementAndGet();
        }
    }

    private RetentionSweeper newSweeper(StubElector elector) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        InMemoryRunRegistry runRegistry = new InMemoryRunRegistry();
        InMemoryToolCallLog toolCallLog = new InMemoryToolCallLog();
        return new RetentionSweeper(
                new SessionCleaner(stores, runRegistry, toolCallLog),
                stores.observabilityStore(), stores.summaryStore(),
                toolCallLog, runRegistry,
                SessionHistoryPolicy.defaults(), new ObservabilityTtl(null, null),
                3, Duration.ofDays(7), Duration.ofHours(24),
                MaintenanceTrigger.defaults(), Duration.ofMillis(60), CLOCK, true, elector);
    }

    /** 朴素轮询（core 测试无 awaitility 依赖——诚实边界内自持）。 */
    private static void awaitCondition(String what, java.util.function.BooleanSupplier condition)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时：" + what);
            }
            Thread.sleep(20);
        }
    }

    @Test
    void leaderSweepsOnSchedule() throws InterruptedException {
        StubElector elector = new StubElector();
        RetentionSweeper sweeper = newSweeper(elector);
        List<RetentionSweepReport> reports = new CopyOnWriteArrayList<>();
        sweeper.addSweepListener(reports::add);
        sweeper.start();
        try {
            awaitCondition("leader 周期报告", () -> reports.size() >= 2);
            assertThat(elector.tries.get()).isGreaterThanOrEqualTo(2);
        } finally {
            sweeper.stop();
        }
    }

    @Test
    void nonLeaderSkipsTicks_leavesCounterTrace() throws InterruptedException {
        StubElector elector = new StubElector();
        elector.leader = false;
        RetentionSweeper sweeper = newSweeper(elector);
        List<RetentionSweepReport> reports = new CopyOnWriteArrayList<>();
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
        sweeper.addSweepListener(reports::add);
        sweeper.start();
        try {
            awaitCondition("非 leader 至少试两轮", () -> elector.tries.get() >= 2);
            Thread.sleep(150); // 留出一个绝不执行的周期
            assertThat(reports).isEmpty(); // 一个周期都没扫
            assertThat(counters).contains("buzhou.retention.skipped-not-leader");
        } finally {
            sweeper.stop();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
        }
    }

    @Test
    void unreachableBackendSkipsTick_schedulerSurvives() throws InterruptedException {
        StubElector elector = new StubElector();
        RetentionSweeper sweeper = newSweeper(elector);
        List<RetentionSweepReport> reports = new CopyOnWriteArrayList<>();
        sweeper.addSweepListener(reports::add);
        sweeper.start();
        try {
            awaitCondition("断连前首轮扫过", () -> reports.size() >= 1);
            elector.throwOnTry = true; // Redis 断连
            int triesAtOutage = elector.tries.get();
            int reportsAtOutage = reports.size();
            awaitCondition("调度线程活着继续 try",
                    () -> elector.tries.get() >= triesAtOutage + 2);
            assertThat(reports.size()).isEqualTo(reportsAtOutage); // 失联期间零新增周期
        } finally {
            sweeper.stop();
        }
    }

    @Test
    void stopResignsForFastFailover_idempotent() {
        StubElector elector = new StubElector();
        RetentionSweeper sweeper = newSweeper(elector);
        sweeper.start();
        sweeper.stop();
        assertThat(elector.resigns.get()).isEqualTo(1); // 停机让位
        sweeper.stop();
        assertThat(elector.resigns.get()).isEqualTo(1); // 幂等——二次 stop 不再让
    }

    @Test
    void manualSweepOnceBypassesGate() {
        StubElector elector = new StubElector();
        elector.leader = false; // 非 leader
        RetentionSweeper sweeper = newSweeper(elector);
        RetentionSweepReport report = sweeper.sweepOnce(); // 手动不设门
        assertThat(report).isNotNull();
        assertThat(report.fullySucceeded()).isTrue();
        assertThat(elector.tries.get()).isZero(); // 门根本没被咨询
    }
}
