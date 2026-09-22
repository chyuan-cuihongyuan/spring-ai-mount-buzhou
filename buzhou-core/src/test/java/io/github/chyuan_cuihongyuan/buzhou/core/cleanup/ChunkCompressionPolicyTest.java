package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1905 / T3012：分块压缩——阈值判定、节省、读代价、畸形。 */
class ChunkCompressionPolicyTest {

    /** 阈值判定：恰阈值含上压缩、未到不压。 */
    @Test
    void thresholdInclusive() {
        assertThat(ChunkCompressionPolicy.shouldCompress(7 * 86_400_000L,
                7 * 86_400_000L)).isTrue();
        assertThat(ChunkCompressionPolicy.shouldCompress(7 * 86_400_000L - 1,
                7 * 86_400_000L)).isFalse();
    }

    /** 节省估计：100GB×3:1 → 省 2/3。 */
    @Test
    void savingsEstimatePrecise() {
        long gb = 1024L * 1024 * 1024;
        assertThat(ChunkCompressionPolicy.savingsEstimate(100 * gb, 3.0))
                .isEqualTo(100 * gb * 2 / 3);
    }

    /** 读代价：= 压缩比 3.0——压缩块查询解压的诚实预期。 */
    @Test
    void readPenaltyHonest() {
        assertThat(ChunkCompressionPolicy.readPenaltyFactor(3.0))
                .isCloseTo(3.0, within(1e-12));
    }

    /** 畸形入参 fail-fast：负年龄、负阈值、ratio ≤ 1。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ChunkCompressionPolicy.shouldCompress(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("块龄不能为负");
        assertThatThrownBy(() -> ChunkCompressionPolicy.shouldCompress(100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("compressAfter 不能为负");
        assertThatThrownBy(() -> ChunkCompressionPolicy.savingsEstimate(100, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("压缩比须 > 1");
        assertThatThrownBy(() -> ChunkCompressionPolicy.readPenaltyFactor(0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("压缩比须 > 1");
    }
}
