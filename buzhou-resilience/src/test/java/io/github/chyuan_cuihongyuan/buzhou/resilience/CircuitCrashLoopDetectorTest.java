package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 811 / T1124：crash-loop 检测回归——阈值转闩/窗口滑出/闩锁不自动解除/
 * 恢复清除+计数一次/未达阈值不闩/封顶/fail-fast。
 */
class CircuitCrashLoopDetectorTest {

    @Test
    void thresholdLatchesAndRecoveryClears() {
        CircuitCrashLoopDetector detector = new CircuitCrashLoopDetector(3, 60_000);

        detector.recordOpen("m1", 1_000);
        detector.recordOpen("m1", 2_000);
        assertThat(detector.isLooping("m1")).isFalse(); // 2 < 3 不闩
        detector.recordOpen("m1", 3_000);
        assertThat(detector.isLooping("m1")).isTrue(); // 第 3 次 OPEN 闩锁

        // 闩锁不因窗口滑过自动解除（k8s backoff 语义）
        detector.recordOpen("m1", 500_000);
        assertThat(detector.isLooping("m1")).isTrue();

        // 恢复清除
        detector.recordRecovery("m1", 600_000);
        assertThat(detector.isLooping("m1")).isFalse();
        assertThat(detector.snapshot().get(0).opensInWindow()).isZero();
        assertThat(detector.snapshot().get(0).loopsDetected()).isEqualTo(1); // 只计一次进入
    }

    @Test
    void windowSlidesOldOpensOut() {
        CircuitCrashLoopDetector detector = new CircuitCrashLoopDetector(2, 1_000);
        detector.recordOpen("m1", 0);
        detector.recordOpen("m1", 2_000); // 第一个滑出窗
        assertThat(detector.isLooping("m1")).isFalse();
        assertThat(detector.snapshot().get(0).opensInWindow()).isEqualTo(1);
    }

    @Test
    void relatchAfterRecoveryCountsAgain() {
        CircuitCrashLoopDetector detector = new CircuitCrashLoopDetector(2, 60_000);
        detector.recordOpen("m1", 1);
        detector.recordOpen("m1", 2); // 闩 #1
        detector.recordRecovery("m1", 3);
        detector.recordOpen("m1", 4);
        detector.recordOpen("m1", 5); // 闩 #2
        assertThat(detector.snapshot().get(0).loopsDetected()).isEqualTo(2);
        assertThat(detector.isLooping("m1")).isTrue();
    }

    @Test
    void modelsAreIndependentAndCapped() {
        CircuitCrashLoopDetector detector = new CircuitCrashLoopDetector(2, 60_000);
        detector.recordOpen("m1", 1);
        detector.recordOpen("m1", 2);
        detector.recordOpen("m2", 3);
        detector.recordOpen("m2", 4);
        assertThat(detector.isLooping("m1")).isTrue();
        assertThat(detector.isLooping("m2")).isTrue();
        assertThat(detector.isLooping("m3")).isFalse();

        CircuitCrashLoopDetector capped = new CircuitCrashLoopDetector(2, 60_000);
        for (int i = 0; i < CircuitCrashLoopDetector.MAX_MODELS + 3; i++) {
            capped.recordOpen("x" + i, 1);
            capped.recordOpen("x" + i, 2);
        }
        assertThat(capped.snapshot()).hasSize(CircuitCrashLoopDetector.MAX_MODELS);
        assertThat(capped.truncated()).isTrue();
    }

    @Test
    void nullModelIgnoredAndSnapshotSorted() {
        CircuitCrashLoopDetector detector = new CircuitCrashLoopDetector(2, 60_000);
        detector.recordOpen(null, 1);
        detector.recordOpen("  ", 1);
        detector.recordOpen("b", 2);
        detector.recordOpen("b", 3);
        detector.recordOpen("a", 4);
        detector.recordOpen("a", 5);
        assertThat(detector.snapshot()).hasSize(2);
        assertThat(detector.snapshot().get(0).model()).isEqualTo("a");
        assertThat(detector.isLooping(null)).isFalse();
        detector.recordRecovery(null, 9); // no-op 不抛
    }

    @Test
    void failFastOnBadParams() {
        assertThatThrownBy(() -> new CircuitCrashLoopDetector(1, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CircuitCrashLoopDetector(2, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
