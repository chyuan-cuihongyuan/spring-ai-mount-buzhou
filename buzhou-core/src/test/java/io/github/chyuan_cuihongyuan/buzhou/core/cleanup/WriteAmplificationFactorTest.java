package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1914 / T3030：LSM 写放大——WAF、压实债、限速、畸形。 */
class WriteAmplificationFactorTest {

    /** WAF：落盘 300/逻辑 100 = 3.0；零写入 0.0。 */
    @Test
    void wafRatio() {
        assertThat(WriteAmplificationFactor.waf(300, 100))
                .isCloseTo(3.0, within(1e-12));
        assertThat(WriteAmplificationFactor.waf(0, 100))
                .isCloseTo(0.0, within(1e-12));
    }

    /** 压实债：待压实 50GB/盘 500GB = 10%。 */
    @Test
    void compactionDebt() {
        long gb = 1024L * 1024 * 1024;
        assertThat(WriteAmplificationFactor.compactionDebtRatio(50 * gb, 500 * gb))
                .isCloseTo(0.1, within(1e-12));
    }

    /** 限速判定：WAF ≥ 10 建议、< 10 不建议、恰 10 含上。 */
    @Test
    void throttleVerdict() {
        assertThat(WriteAmplificationFactor.needsThrottle(12.0, 10.0)).isTrue();
        assertThat(WriteAmplificationFactor.needsThrottle(9.0, 10.0)).isFalse();
        assertThat(WriteAmplificationFactor.needsThrottle(10.0, 10.0)).isTrue();
    }

    /** 畸形入参 fail-fast：负落盘、零逻辑、负债务、阈值 < 1。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> WriteAmplificationFactor.waf(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("落盘字节不能为负");
        assertThatThrownBy(() -> WriteAmplificationFactor.waf(100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("逻辑写入字节不能小于 1");
        assertThatThrownBy(() -> WriteAmplificationFactor.compactionDebtRatio(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("待压实字节不能为负");
        assertThatThrownBy(() -> WriteAmplificationFactor.needsThrottle(10, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold 不能小于 1");
    }
}
