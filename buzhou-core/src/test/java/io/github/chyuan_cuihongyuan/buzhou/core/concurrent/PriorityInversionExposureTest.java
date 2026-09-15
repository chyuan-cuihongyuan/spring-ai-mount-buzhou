package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1821 / T2844：优先级反转暴露——低优挡高优计数、最坏差、诚实账。 */
class PriorityInversionExposureTest {

    /** 低优持有者挡高优等待者：反转计数 + 最坏差 + 反转率。 */
    @Test
    void shouldCountInversionsWithWorstGap() {
        PriorityInversionExposure.Exposure exposure = PriorityInversionExposure.analyze(
                List.of(new PriorityInversionExposure.HeldResource("lock-a", 10),
                        new PriorityInversionExposure.HeldResource("lock-b", 5)),
                List.of(new PriorityInversionExposure.Waiter("lock-a", 1),
                        new PriorityInversionExposure.Waiter("lock-a", 8),
                        new PriorityInversionExposure.Waiter("lock-b", 5)));
        // lock-a rank10 持有：waiter rank1 反转（gap 9）、waiter rank8 反转（gap 2）
        // lock-b rank5 持有：waiter rank5 同级不反转
        assertThat(exposure.inversions()).isEqualTo(2);
        assertThat(exposure.worstRankGap()).isEqualTo(9);
        assertThat(exposure.inversionRatio()).isEqualTo(2.0 / 3.0);
        assertThat(exposure.resources()).isEqualTo(2);
        assertThat(exposure.waiters()).isEqualTo(3);
    }

    /** 持有者更关键（或同级）不反转；引用未持有资源诚实入账不算反转。 */
    @Test
    void noInversionWhenHolderAtLeastAsCritical() {
        PriorityInversionExposure.Exposure exposure = PriorityInversionExposure.analyze(
                List.of(new PriorityInversionExposure.HeldResource("lock", 1)),
                List.of(new PriorityInversionExposure.Waiter("lock", 5),
                        new PriorityInversionExposure.Waiter("lock", 1),
                        new PriorityInversionExposure.Waiter("lock-x", 0)));
        assertThat(exposure.inversions()).isZero();
        assertThat(exposure.worstRankGap()).isZero();
        assertThat(exposure.unknownResourceWaiters()).isEqualTo(1);
        assertThat(exposure.inversionRatio()).isZero();
    }

    /** 空表与 null 同口径：零账目 + 比率 -1 哨兵。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (PriorityInversionExposure.Exposure exposure : List.of(
                PriorityInversionExposure.analyze(List.of(), List.of()),
                PriorityInversionExposure.analyze(null, null))) {
            assertThat(exposure.resources()).isZero();
            assertThat(exposure.waiters()).isZero();
            assertThat(exposure.inversionRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形入参 fail-fast：空白资源 id（两侧）。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new PriorityInversionExposure.HeldResource("", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PriorityInversionExposure.Waiter(" ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
