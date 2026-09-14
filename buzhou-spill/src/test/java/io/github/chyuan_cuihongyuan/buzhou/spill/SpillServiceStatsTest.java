package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1101 / impl 853：SpillService 服务层分支读面——阈值内直返、新落盘、
 * 幂等复用、降级透传、守恒恒等式、resetForTest 归零。
 */
class SpillServiceStatsTest {

    @TempDir
    Path rootDir;

    @BeforeEach
    void reset() {
        SpillService.resetForTest();
    }

    private SpillService service() {
        return new SpillService(new DiskSpillStore(rootDir), 64, 3);
    }

    private static final String BIG = "x".repeat(200); // 超阈值

    @Test
    void belowThresholdCountsItsBucket() {
        SpillService svc = service();
        svc.tryOffload("app", "s1", "tc1", "tool", "short", 1000);

        SpillService.SpillServiceStats stats = SpillService.stats();
        assertThat(stats.tryOffloadCalls()).isEqualTo(1);
        assertThat(stats.belowThreshold()).isEqualTo(1);
        assertThat(stats.freshStores()).isZero();
    }

    @Test
    void freshStoreThenIdempotentReuse() {
        SpillService svc = service();
        svc.tryOffload("app", "s1", "tc1", "tool", BIG, 100);   // 新落盘
        svc.tryOffload("app", "s1", "tc1", "tool", BIG, 100);   // 同内容重放 → 幂等复用

        SpillService.SpillServiceStats stats = SpillService.stats();
        assertThat(stats.freshStores()).isEqualTo(1);
        assertThat(stats.idempotentReuses()).isEqualTo(1);
    }

    @Test
    void degradedCountsItsBucket() {
        SpillService failing = new SpillService(new SpillOffloadHookTest.FailingSpillStore(), 64, 3);
        SpillService.SpillServiceStats before = SpillService.stats();
        failing.tryOffload("app", "s1", "tc1", "tool", BIG, 100);

        SpillService.SpillServiceStats stats = SpillService.stats();
        assertThat(stats.degraded()).isEqualTo(before.degraded() + 1);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        SpillService svc = service();
        svc.tryOffload("app", "s1", "tc1", "tool", BIG, 100);  // fresh
        svc.tryOffload("app", "s1", "tc1", "tool", BIG, 100);  // idempotent
        svc.tryOffload("app", "s1", "tc2", "tool", "tiny", 100); // below

        SpillService.SpillServiceStats stats = SpillService.stats();
        assertThat(stats.tryOffloadCalls()).isEqualTo(3);
        assertThat(stats.tryOffloadCalls())
                .isEqualTo(stats.freshStores() + stats.idempotentReuses()
                        + stats.degraded() + stats.belowThreshold());
    }

    @Test
    void resetForTestZeroesCounters() {
        SpillService svc = service();
        svc.tryOffload("app", "s1", "tc1", "tool", BIG, 100);
        assertThat(SpillService.stats().tryOffloadCalls()).isEqualTo(1);

        SpillService.resetForTest();

        SpillService.SpillServiceStats stats = SpillService.stats();
        assertThat(stats.tryOffloadCalls()).isZero();
        assertThat(stats.freshStores()).isZero();
    }
}
