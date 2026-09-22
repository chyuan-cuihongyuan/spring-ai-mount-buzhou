package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1906 / T3014：分片偏斜——偏斜比、触发判定、畸形。 */
class ShardSkewAuditTest {

    /** 偏斜比：{100,50,50} → 1.5；{90,10,10} → ~2.45。 */
    @Test
    void skewRatioPrecise() {
        assertThat(ShardSkewAudit.skewRatio(new long[]{100, 50, 50}))
                .isCloseTo(1.5, within(1e-12));
        assertThat(ShardSkewAudit.skewRatio(new long[]{90, 10, 10}))
                .isCloseTo(90.0 / (110.0 / 3), within(1e-12));
    }

    /** 绝对均衡 {50,50,50} → 1.0。 */
    @Test
    void perfectBalanceIsOne() {
        assertThat(ShardSkewAudit.skewRatio(new long[]{50, 50, 50}))
                .isCloseTo(1.0, within(1e-12));
    }

    /** 触发判定：阈值 2 两侧行为。 */
    @Test
    void reshardTriggerBoundary() {
        assertThat(ShardSkewAudit.needsReshard(new long[]{90, 10, 10}, 2.0)).isTrue();
        assertThat(ShardSkewAudit.needsReshard(new long[]{100, 50, 50}, 2.0)).isFalse();
    }

    /** 畸形入参 fail-fast：空表、全零负载、负负载、阈值 < 1。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ShardSkewAudit.skewRatio(new long[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("负载表不能为空");
        assertThatThrownBy(() -> ShardSkewAudit.skewRatio(new long[]{0, 0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("负载全零");
        assertThatThrownBy(() -> ShardSkewAudit.skewRatio(new long[]{-5, 10}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("负载不能为负");
        assertThatThrownBy(() -> ShardSkewAudit.needsReshard(new long[]{10}, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skewThreshold 不能小于 1");
    }
}
