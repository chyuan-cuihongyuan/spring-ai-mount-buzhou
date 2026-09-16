package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2004 / T3110：φ 累积故障嫌疑度合同——规律心跳近期低嫌疑/超期高
 * 嫌疑、φ 随超时单调增、样本不足恒 0、std 下界防爆、φ 上界 12、畸形
 * fail-fast。
 */
class PhiAccrualFailureDetectorTest {

    private static PhiAccrualFailureDetector regularBeater(int beats) {
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector(100, 100L);
        for (int i = 0; i <= beats; i++) {
            detector.heartbeat(1_000L * i); // 1000ms 规律间隔
        }
        return detector;
    }

    @Test
    void regularHeartbeatRecentlyShouldStayLowSuspicion() {
        PhiAccrualFailureDetector detector = regularBeater(10);
        // 最后心跳 @10000，均值 1000——刚过均值一点：低嫌疑
        assertThat(detector.phi(10_500L)).isLessThan(2.0d);
    }

    @Test
    void overdueBeyondTailShouldReachHighSuspicion() {
        PhiAccrualFailureDetector detector = regularBeater(10);
        // 最后心跳 @10000，5 个均值未到——右尾极小：高嫌疑
        assertThat(detector.phi(15_000L)).isGreaterThan(4.0d);
    }

    @Test
    void phiShouldGrowMonotonicallyWithSilence() {
        PhiAccrualFailureDetector detector = regularBeater(10);
        double phi1 = detector.phi(10_500L);
        double phi2 = detector.phi(11_500L);
        double phi3 = detector.phi(13_000L);
        assertThat(phi2).isGreaterThan(phi1);
        assertThat(phi3).isGreaterThan(phi2);
    }

    @Test
    void phiShouldCapAtTwelve() {
        PhiAccrualFailureDetector detector = regularBeater(10);
        assertThat(detector.phi(1_000_000L)).isLessThanOrEqualTo(12.0d);
        // 远超期 → p 触 1e−12 下界 → φ 恰 12
        assertThat(detector.phi(1_000_000L)).isCloseTo(12.0d, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void insufficientSamplesShouldStayZero() {
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector();
        assertThat(detector.phi(10_000L)).isZero(); // 从未心跳
        detector.heartbeat(0);
        assertThat(detector.phi(10_000L)).isZero(); // 单样本（无间隔统计）
    }

    @Test
    void stdFloorShouldKeepRegularBeaterBounded() {
        // 规律心跳 std=0 → minStdDev=100 兜底：均值处 φ≈0.5 量级（有界非爆）
        PhiAccrualFailureDetector detector = regularBeater(10);
        assertThat(detector.meanIntervalMillis()).isCloseTo(1000.0d,
                org.assertj.core.data.Offset.offset(1e-9));
        // x=mean 处右尾恰 0.5 → φ = −log10(0.5) ≈ 0.301
        assertThat(detector.phi(11_000L)).isCloseTo(-Math.log10(0.5d),
                org.assertj.core.data.Offset.offset(0.2d));
    }

    @Test
    void windowShouldSlideAndAdaptToNewRhythm() {
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector(3, 50L);
        // 旧节奏 1000ms × 4（窗 3 只留最近 3 个间隔）
        for (int i = 0; i <= 4; i++) {
            detector.heartbeat(1_000L * i);
        }
        assertThat(detector.sampleCount()).isEqualTo(3);
        // 换节奏 100ms × 5——窗被新节奏占满
        long t = 4_000L;
        for (int i = 0; i <= 5; i++) {
            detector.heartbeat(t);
            t += 100L;
        }
        assertThat(detector.sampleCount()).isEqualTo(3);
        assertThat(detector.meanIntervalMillis()).isCloseTo(100.0d,
                org.assertj.core.data.Offset.offset(1e-9));
        // 新节奏下 600ms 未到（6 个均值）→ 高嫌疑
        assertThat(detector.phi(t + 500L)).isGreaterThan(3.0d);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new PhiAccrualFailureDetector(1, 100L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhiAccrualFailureDetector(100, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector();
        detector.heartbeat(5_000L);
        assertThatThrownBy(() -> detector.heartbeat(4_999L)) // 时间回拨
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("回拨");
    }
}
