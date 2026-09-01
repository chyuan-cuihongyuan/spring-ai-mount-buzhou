package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 303 / impl-326：围栏纪元判定矩阵——同纪元四态既有 / 纪元跃升显式 RESET /
 * 旧纪元 STALE 基线不动 / 同纪元倒退防御性重基线 / 旧发送方无纪元路径兼容。
 */
class SequenceFenceEpochTest {

    private static final String SUB = "sub-1";

    @Test
    void sameEpochKeepsLegacyFourVerdicts() {
        SequenceFence fence = new SequenceFence();
        assertThat(fence.observe(SUB, 7L, 1L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
        assertThat(fence.observe(SUB, 7L, 2L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
        assertThat(fence.observe(SUB, 7L, 2L)).isEqualTo(SequenceFence.Verdict.DUPLICATE);
        assertThat(fence.observe(SUB, 7L, 5L)).isEqualTo(SequenceFence.Verdict.GAP);
    }

    @Test
    void epochBumpIsExplicitReset_andContinuityResumesPerEpoch() {
        SequenceFence fence = new SequenceFence();
        fence.observe(SUB, 7L, 3L);
        // 发送方重启：纪元跃升 → RESET（显式），seq 从 1 重新连续
        assertThat(fence.observe(SUB, 8L, 1L)).isEqualTo(SequenceFence.Verdict.RESET);
        assertThat(fence.observe(SUB, 8L, 2L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
    }

    @Test
    void staleEpochArrivalIsSafeDrop_baselineUntouched() {
        SequenceFence fence = new SequenceFence();
        fence.observe(SUB, 9L, 5L);
        // 旧纪元（重启期间滞留重试）迟到投递：STALE，基线不动
        assertThat(fence.observe(SUB, 8L, 6L)).isEqualTo(SequenceFence.Verdict.STALE);
        // 新纪元 9 的连续性不受旧纪元污染
        assertThat(fence.observe(SUB, 9L, 6L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
    }

    @Test
    void sameEpochSeqRegressIsDefensiveReset() {
        SequenceFence fence = new SequenceFence();
        fence.observe(SUB, 7L, 5L);
        // 同纪元倒退：非重启（纪元未变）——异常，防御性重基线
        assertThat(fence.observe(SUB, 7L, 2L)).isEqualTo(SequenceFence.Verdict.RESET);
        assertThat(fence.observe(SUB, 7L, 3L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
    }

    @Test
    void legacyNoEpochPathUnchanged() {
        SequenceFence fence = new SequenceFence();
        assertThat(fence.observe(SUB, 5L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
        assertThat(fence.observe(SUB, 6L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
        assertThat(fence.observe(SUB, 6L)).isEqualTo(SequenceFence.Verdict.DUPLICATE);
        // 旧语义：seq 变小 = 推断重启
        assertThat(fence.observe(SUB, 1L)).isEqualTo(SequenceFence.Verdict.RESET);
    }

    @Test
    void legacyBaselineUpgradesToEpochBaselineOnFirstEpochEnvelope() {
        SequenceFence fence = new SequenceFence();
        fence.observe(SUB, 5L); // 旧发送方建基线（无纪元）
        // 升级后首只带纪元信封：基线纪元为空 → 视为显式新纪元
        assertThat(fence.observe(SUB, 7L, 1L)).isEqualTo(SequenceFence.Verdict.RESET);
        assertThat(fence.observe(SUB, 7L, 2L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
    }
}
