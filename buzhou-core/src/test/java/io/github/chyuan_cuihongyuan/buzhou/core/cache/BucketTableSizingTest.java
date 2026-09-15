package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1851 / T2904：桶表容量——建议幂、扩容判定边界含、装填度。 */
class BucketTableSizingTest {

    /** 建议容量：⌈n/lf⌉ 向上取 2 的幂。 */
    @Test
    void shouldSuggestPowerOfTwoCapacity() {
        // 100/0.75 = 133.3 → 256
        assertThat(BucketTableSizing.suggestCapacity(100, 0.75d)).isEqualTo(256);
        assertThat(BucketTableSizing.suggestCapacity(3, 0.75d)).isEqualTo(4);
        assertThat(BucketTableSizing.suggestCapacity(0, 0.75d)).isEqualTo(1);
        assertThat(BucketTableSizing.suggestCapacity(100, 1.0d)).isEqualTo(128);
    }

    /** 扩容判定边界含上：装填度 == 负载因子即 RESIZE。 */
    @Test
    void resizeVerdictBoundaryIsInclusive() {
        assertThat(BucketTableSizing.verdict(4, 3, 0.75d))
                .isEqualTo(BucketTableSizing.Verdict.RESIZE_NEEDED);
        assertThat(BucketTableSizing.verdict(4, 2, 0.75d))
                .isEqualTo(BucketTableSizing.Verdict.OK);
        assertThat(BucketTableSizing.verdict(1, 1, 1.0d))
                .isEqualTo(BucketTableSizing.Verdict.RESIZE_NEEDED);
    }

    /** 装填度读数：容量 8 装 6 = 0.75。 */
    @Test
    void loadReadout() {
        assertThat(BucketTableSizing.load(8, 6)).isEqualTo(0.75d);
        assertThat(BucketTableSizing.load(8, 0)).isZero();
    }

    /** 畸形入参 fail-fast：负条目、容量 < 1、负 size、负载因子越界/NaN。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> BucketTableSizing.suggestCapacity(-1, 0.75d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为负");
        assertThatThrownBy(() -> BucketTableSizing.verdict(0, 1, 0.75d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity 不能小于 1");
        assertThatThrownBy(() -> BucketTableSizing.verdict(4, -1, 0.75d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BucketTableSizing.suggestCapacity(10, 0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loadFactor 须在 (0,1]");
        assertThatThrownBy(() -> BucketTableSizing.verdict(4, 1, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
