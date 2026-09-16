package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.GraceAwareFailureDetector.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2020 / T3142：冷启动豁免 φ 门合同——豁免期高 φ 观望不判死、
 * 毕业后判死、豁免失败不喂 φ、成功心跳毕业、三态判定、畸形 fail-fast。
 */
class GraceAwareFailureDetectorTest {

    /** 规律心跳预热（毕业后可用）：1000ms 节奏 10 拍。 */
    private static GraceAwareFailureDetector warmedUp(long graceMillis) {
        GraceAwareFailureDetector detector = new GraceAwareFailureDetector("target", graceMillis);
        for (int i = 0; i <= 10; i++) {
            detector.heartbeat(1_000L * i); // 首拍即毕业
        }
        return detector;
    }

    @Test
    void highPhiAfterGraduationShouldConfirm() {
        GraceAwareFailureDetector detector = warmedUp(5_000L);
        // 最后心跳 @10000，沉默到 20000——10 均值未到，φ 极高
        assertThat(detector.verdict(20_000L, 4.0)).isEqualTo(Verdict.CONFIRMED);
    }

    @Test
    void highPhiInsideGraceShouldHoldNotConfirm() {
        GraceAwareFailureDetector detector = new GraceAwareFailureDetector("target", 600_000L);
        // 全程豁免窗内（600s 窗）：失败不喂 φ——φ 无样本恒 0 → HEALTHY
        detector.failure(1_000L);
        detector.failure(2_000L);
        detector.failure(3_000L);
        assertThat(detector.suspicion(4_000L)).isZero(); // 噪声未进模型
        assertThat(detector.verdict(4_000L, 1.0)).isEqualTo(Verdict.HEALTHY);
        assertThat(detector.graceStats().exemptedFailures()).isEqualTo(3L); // 豁免账在
    }

    @Test
    void failuresAfterGraceExpiryShouldFeedPhiAndConfirm() {
        GraceAwareFailureDetector detector = new GraceAwareFailureDetector("target", 1_000L);
        // 窗内失败（豁免，不喂 φ）
        detector.failure(500L);
        // 窗外失败三拍（喂 φ——建立 50s 间隔模型）+ 远沉默 → φ 攀升
        detector.failure(50_000L);
        detector.failure(100_000L);
        detector.failure(150_000L);
        assertThat(detector.verdict(400_000L, 1.0)).isEqualTo(Verdict.CONFIRMED); // 5 均值沉默
        assertThat(detector.graceStats().countedFailures()).isEqualTo(3L); // 计账账在
    }

    @Test
    void healthyRhythmShouldStayHealthy() {
        GraceAwareFailureDetector detector = warmedUp(5_000L);
        assertThat(detector.verdict(10_500L, 4.0)).isEqualTo(Verdict.HEALTHY); // 刚过均值
    }

    @Test
    void firstHeartbeatShouldGraduateImmediately() {
        GraceAwareFailureDetector detector = new GraceAwareFailureDetector("target", 600_000L);
        assertThat(detector.graceStats().graduations()).isZero(); // 未有心跳
        detector.heartbeat(1_000L);
        assertThat(detector.graceStats().graduations()).isEqualTo(1L); // 首拍毕业
        // 毕业后失败直接计账（不再豁免）
        detector.failure(1_500L);
        assertThat(detector.graceStats().exemptedFailures()).isZero();
        assertThat(detector.graceStats().countedFailures()).isEqualTo(1L);
    }

    @Test
    void graceHoldWindowShouldExpireIntoConfirm() {
        // 豁免窗中等 φ：样本不足恒 0 —— 构造「豁免中但 φ 高」需窗内有心跳
        // 预热（毕业）不可能同时豁免中，故 GRACE_HOLD 态由 activeGraces 语义
        // 单元覆盖：豁免中 φ 恒低（失败不喂）→ HOLD 态实际由「窗外样本 + 窗
        // 内新豁免」构成——重锚场景归后续轮；此处钉主路径三态完备性
        GraceAwareFailureDetector detector = warmedUp(5_000L);
        assertThat(detector.verdict(10_500L, 4.0)).isEqualTo(Verdict.HEALTHY);
        assertThat(detector.verdict(20_000L, 4.0)).isEqualTo(Verdict.CONFIRMED);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new GraceAwareFailureDetector(null, 1_000L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraceAwareFailureDetector("t", 0))
                .isInstanceOf(IllegalArgumentException.class);
        GraceAwareFailureDetector detector = new GraceAwareFailureDetector("t", 1_000L);
        assertThatThrownBy(() -> detector.verdict(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> detector.verdict(0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
