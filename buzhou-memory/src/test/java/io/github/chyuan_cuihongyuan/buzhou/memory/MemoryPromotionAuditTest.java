package io.github.chyuan_cuihongyuan.buzhou.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1803 / T2808：层代晋升账目——晋升率、越级归档率、过早晋升周期。 */
class MemoryPromotionAuditTest {

    /** 账目聚合正确：四计数求和、比率高低调得出、过早晋升只计零保留产出轮。 */
    @Test
    void shouldAggregateRatesAndPrematureCycles() {
        MemoryPromotionAudit.PromotionReport report = MemoryPromotionAudit.analyze(List.of(
                // 健康轮：大部分原地保留，年轻代缓冲有效
                new MemoryPromotionAudit.CycleFacts(10, 2, 1, 7),
                // 过早晋升轮：全军晋升零保留
                new MemoryPromotionAudit.CycleFacts(8, 8, 0, 0),
                // 空转轮：无产出，不计过早晋升
                new MemoryPromotionAudit.CycleFacts(0, 0, 0, 0)));
        assertThat(report.cycles()).isEqualTo(3);
        assertThat(report.totalMicroCompacted()).isEqualTo(18);
        assertThat(report.totalPromoted()).isEqualTo(10);
        assertThat(report.totalArchivedDirect()).isEqualTo(1);
        assertThat(report.totalRetainedInPlace()).isEqualTo(7);
        assertThat(report.promotionRate()).isEqualTo(10.0 / 18.0);
        assertThat(report.directArchiveRate()).isEqualTo(1.0 / 18.0);
        assertThat(report.prematurePromotionCycles()).isEqualTo(1);
    }

    /** 全健康（零晋升）与全过早（全晋升）两档读数分开。 */
    @Test
    void healthyVersusPrematureExtremes() {
        MemoryPromotionAudit.PromotionReport healthy = MemoryPromotionAudit.analyze(List.of(
                new MemoryPromotionAudit.CycleFacts(5, 0, 0, 5)));
        assertThat(healthy.promotionRate()).isZero();
        assertThat(healthy.prematurePromotionCycles()).isZero();

        MemoryPromotionAudit.PromotionReport premature = MemoryPromotionAudit.analyze(List.of(
                new MemoryPromotionAudit.CycleFacts(5, 5, 0, 0),
                new MemoryPromotionAudit.CycleFacts(6, 6, 0, 0)));
        assertThat(premature.promotionRate()).isEqualTo(1.0d);
        assertThat(premature.prematurePromotionCycles()).isEqualTo(2);
    }

    /** 空表与 null 同口径：零计数 + 比率 -1 哨兵。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (MemoryPromotionAudit.PromotionReport report : List.of(
                MemoryPromotionAudit.analyze(List.of()),
                MemoryPromotionAudit.analyze(null))) {
            assertThat(report.cycles()).isZero();
            assertThat(report.promotionRate()).isEqualTo(-1d);
            assertThat(report.directArchiveRate()).isEqualTo(-1d);
            assertThat(report.prematurePromotionCycles()).isZero();
        }
    }

    /** 畸形周期事实 fail-fast：负数与去向之和不等于产出。 */
    @Test
    void malformedCycleFactsFailFast() {
        assertThatThrownBy(() -> new MemoryPromotionAudit.CycleFacts(5, 3, 1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("去向之和=micro");
        assertThatThrownBy(() -> new MemoryPromotionAudit.CycleFacts(-1, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
