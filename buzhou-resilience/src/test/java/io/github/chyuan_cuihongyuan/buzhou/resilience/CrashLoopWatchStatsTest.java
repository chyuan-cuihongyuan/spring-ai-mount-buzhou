package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1056 / impl 808：崩循环探测器类级水位——OPEN 记录量、MAX_MODELS 封顶截断量
 * （静默蒸发显形）、循环检出总数（重复周期重复计）、恢复次数、null 不入账、归零。
 */
class CrashLoopWatchStatsTest {

    @BeforeEach
    void reset() {
        CircuitCrashLoopDetector.resetForTest();
    }

    @Test
    void opensCountTowardRecorded() {
        CircuitCrashLoopDetector d = new CircuitCrashLoopDetector(2, 60_000);
        d.recordOpen("m1", 1_000);
        d.recordOpen("m1", 2_000);

        CircuitCrashLoopDetector.CrashLoopWatchStats stats = CircuitCrashLoopDetector.stats();
        assertThat(stats.opensRecorded()).isEqualTo(2);
        assertThat(stats.opensTruncated()).isZero();
    }

    @Test
    void overflowModelsCountTruncated() {
        CircuitCrashLoopDetector d = new CircuitCrashLoopDetector(2, 60_000);
        for (int i = 0; i < CircuitCrashLoopDetector.MAX_MODELS + 1; i++) {
            d.recordOpen("model-" + i, 1_000L + i);
        }

        CircuitCrashLoopDetector.CrashLoopWatchStats stats = CircuitCrashLoopDetector.stats();
        assertThat(stats.opensRecorded()).isEqualTo(CircuitCrashLoopDetector.MAX_MODELS);
        // 第 33 个模型：截断丢弃量化（truncated 布尔之外的对账信号）
        assertThat(stats.opensTruncated()).isEqualTo(1);
        assertThat(d.truncated()).isTrue();
    }

    @Test
    void loopingDetectionCountsLoops() {
        CircuitCrashLoopDetector d = new CircuitCrashLoopDetector(3, 60_000);
        d.recordOpen("m1", 1_000);
        d.recordOpen("m1", 2_000);
        d.recordOpen("m1", 3_000); // 窗口内达 minOpens → 闩锁

        assertThat(CircuitCrashLoopDetector.stats().loopsDetected()).isEqualTo(1);
        assertThat(d.isLooping("m1")).isTrue();
    }

    @Test
    void repeatedCyclesCountRepeatedly() {
        CircuitCrashLoopDetector d = new CircuitCrashLoopDetector(2, 60_000);
        d.recordOpen("m1", 1_000);
        d.recordOpen("m1", 2_000);   // 第一轮成环
        d.recordRecovery("m1", 3_000);
        d.recordOpen("m1", 4_000);
        d.recordOpen("m1", 5_000);   // 第二轮成环

        CircuitCrashLoopDetector.CrashLoopWatchStats stats = CircuitCrashLoopDetector.stats();
        assertThat(stats.loopsDetected()).isEqualTo(2);
        assertThat(stats.recoveriesRecorded()).isEqualTo(1);
    }

    @Test
    void blankModelFallsIntoNoBucket() {
        CircuitCrashLoopDetector d = new CircuitCrashLoopDetector(2, 60_000);
        d.recordOpen(null, 1_000);
        d.recordOpen("  ", 1_000);
        d.recordRecovery("ghost", 1_000);

        CircuitCrashLoopDetector.CrashLoopWatchStats stats = CircuitCrashLoopDetector.stats();
        assertThat(stats.opensRecorded()).isZero();
        assertThat(stats.opensTruncated()).isZero();
        assertThat(stats.recoveriesRecorded()).isZero();
    }

    @Test
    void resetForTestZeroesCounters() {
        CircuitCrashLoopDetector d = new CircuitCrashLoopDetector(2, 60_000);
        d.recordOpen("m1", 1_000);
        d.recordOpen("m1", 2_000);
        assertThat(CircuitCrashLoopDetector.stats().opensRecorded()).isEqualTo(2);

        CircuitCrashLoopDetector.resetForTest();

        CircuitCrashLoopDetector.CrashLoopWatchStats stats = CircuitCrashLoopDetector.stats();
        assertThat(stats.opensRecorded()).isZero();
        assertThat(stats.loopsDetected()).isZero();
        assertThat(stats.recoveriesRecorded()).isZero();
    }
}
