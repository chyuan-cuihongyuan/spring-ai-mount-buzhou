package io.github.chyuan_cuihongyuan.buzhou.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 835 / T1172：尾采样决策台账回归——KEPT/DROPPED 聚合+保留率/原因降序/
 * 环挤老 dropped/脏入参/空真。
 */
class TailSamplingDecisionLogTest {

    @Test
    void decisionsAggregatedWithKeptRatio() {
        TailSamplingDecisionLog log = new TailSamplingDecisionLog();
        log.record("t1", TailSamplingDecisionLog.Decision.KEPT, "always-sample", 1);
        log.record("t2", TailSamplingDecisionLog.Decision.DROPPED, "low-value", 2);
        log.record("t3", TailSamplingDecisionLog.Decision.DROPPED, "low-value", 3);
        log.record("t4", TailSamplingDecisionLog.Decision.KEPT, "error-trace", 4);

        var report = log.snapshot();
        assertThat(report.kept()).isEqualTo(2);
        assertThat(report.droppedCount()).isEqualTo(2);
        assertThat(report.keptRatio()).isCloseTo(0.5, within(1e-9));
        assertThat(report.byReason().get(0).reason()).isEqualTo("low-value");
        assertThat(report.byReason().get(0).count()).isEqualTo(2);
        assertThat(report.recent().get(0).traceId()).isEqualTo("t4"); // 新→旧
        assertThat(report.truncated()).isFalse();
    }

    @Test
    void ringEvictsOldest() {
        TailSamplingDecisionLog log = new TailSamplingDecisionLog();
        for (int i = 0; i < TailSamplingDecisionLog.RING_CAPACITY + 6; i++) {
            log.record("t" + i, TailSamplingDecisionLog.Decision.KEPT, "policy", i);
        }
        var report = log.snapshot();
        assertThat(report.recent()).hasSize(TailSamplingDecisionLog.RING_CAPACITY);
        assertThat(report.ringDropped()).isEqualTo(6);
        assertThat(report.recent().get(0).traceId()).isEqualTo("t" + (TailSamplingDecisionLog.RING_CAPACITY + 5));
    }

    @Test
    void reasonCapOverflowsIntoBucket() {
        TailSamplingDecisionLog log = new TailSamplingDecisionLog();
        for (int i = 0; i < TailSamplingDecisionLog.MAX_REASONS; i++) {
            log.record("t" + i, TailSamplingDecisionLog.Decision.DROPPED, "reason" + i, i);
        }
        log.record("extra", TailSamplingDecisionLog.Decision.DROPPED, "brand-new", 999);

        var report = log.snapshot();
        assertThat(report.byReason()).hasSize(TailSamplingDecisionLog.MAX_REASONS + 1);
        var overflow = report.byReason().stream()
                .filter(r -> r.reason().equals(TailSamplingDecisionLog.OVERFLOW))
                .findFirst().orElseThrow();
        assertThat(overflow.count()).isEqualTo(1);
        assertThat(overflow.decision()).isEqualTo(TailSamplingDecisionLog.Decision.DROPPED);
        assertThat(report.truncated()).isTrue();
    }

    @Test
    void dirtyInputsAndEmptyTruth() {
        TailSamplingDecisionLog log = new TailSamplingDecisionLog();
        log.record(null, TailSamplingDecisionLog.Decision.KEPT, "r", 1);
        log.record("t", null, "r", 1);
        log.record("t", TailSamplingDecisionLog.Decision.KEPT, null, 1);
        log.record("  ", TailSamplingDecisionLog.Decision.KEPT, "r", 1);

        var report = log.snapshot();
        assertThat(report.kept()).isZero();
        assertThat(report.droppedCount()).isZero();
        assertThat(report.keptRatio()).isZero();
        assertThat(report.recent()).isEmpty();
        assertThat(report.byReason()).isEmpty();
    }
}
