package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1890 / T2982：阻塞期审计——三类主导、并列固定序、守恒破坏。 */
class WaitProfileAuditTest {

    /** 锁主导：2/20/8/总 30 → LOCK_WAIT、阻塞比 28/30。 */
    @Test
    void lockWaitDominates() {
        WaitProfileAudit.Profile p = WaitProfileAudit.profile(2, 20, 8, 30);
        assertThat(p.dominant()).isEqualTo(WaitProfileAudit.DominantClass.LOCK_WAIT);
        assertThat(p.blockingRatio()).isCloseTo(28.0 / 30, within(1e-12));
    }

    /** IO 主导与 CPU 主导各一例。 */
    @Test
    void ioAndCpuDominance() {
        assertThat(WaitProfileAudit.profile(5, 2, 13, 20).dominant())
                .isEqualTo(WaitProfileAudit.DominantClass.IO_WAIT);
        assertThat(WaitProfileAudit.profile(18, 1, 1, 20).dominant())
                .isEqualTo(WaitProfileAudit.DominantClass.ON_CPU);
    }

    /** 并列固定序：三值相等 → ON_CPU；CPU=IO>LOCK → ON_CPU。 */
    @Test
    void tiesResolveInFixedOrder() {
        assertThat(WaitProfileAudit.dominantClass(10, 10, 10))
                .isEqualTo(WaitProfileAudit.DominantClass.ON_CPU);
        assertThat(WaitProfileAudit.dominantClass(10, 10, 5))
                .isEqualTo(WaitProfileAudit.DominantClass.ON_CPU);
        assertThat(WaitProfileAudit.dominantClass(5, 10, 10))
                .isEqualTo(WaitProfileAudit.DominantClass.IO_WAIT);
    }

    /** 零时长画像：哨兵阻塞比 0.0。 */
    @Test
    void zeroElapsedSentinel() {
        WaitProfileAudit.Profile p = WaitProfileAudit.profile(0, 0, 0, 0);
        assertThat(p.blockingRatio()).isCloseTo(0.0, within(1e-12));
        assertThat(p.dominant()).isEqualTo(WaitProfileAudit.DominantClass.ON_CPU);
    }

    /** 守恒破坏 fail-fast：和 > 总、和 < 总、负时长。 */
    @Test
    void conservationBreakFailsFast() {
        assertThatThrownBy(() -> WaitProfileAudit.profile(10, 10, 10, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("账面不守恒");
        assertThatThrownBy(() -> WaitProfileAudit.profile(5, 5, 5, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("账面不守恒");
        assertThatThrownBy(() -> WaitProfileAudit.profile(-1, 5, 5, 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时长不能为负");
    }
}
