package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4001 / T6004：Count-Min 合同——单键精确、估计单侧不低估、
 * 总量守恒、畸形 fail-fast。
 */
class CountMinSketchTest {

    @Test
    void singleKeyShouldEstimateExactly() {
        CountMinSketch sketch = new CountMinSketch(4, 512);
        for (int i = 0; i < 100; i++) {
            sketch.increment("only-key", 1);
        }
        assertThat(sketch.estimate("only-key")).isEqualTo(100);   // 零碰撞即零噪声
        assertThat(sketch.estimate("never-seen")).isZero();       // 未见键为零
    }

    @Test
    void estimatesShouldNeverUnderestimateAndStayBounded() {
        CountMinSketch sketch = new CountMinSketch(5, 1024);
        int keys = 100;
        long perKey = 100;
        for (int k = 0; k < keys; k++) {
            sketch.increment("key-" + k, perKey);
        }
        long total = keys * perKey;
        for (int k = 0; k < keys; k++) {
            long est = sketch.estimate("key-" + k);
            assertThat(est).as("键 %d 估计不低估", k).isGreaterThanOrEqualTo(perKey);
            assertThat(est).as("键 %d 估计有上界（≤ 真值 + N/8）", k)
                    .isLessThanOrEqualTo(perKey + total / 8);
        }
        assertThat(sketch.totalCount()).isEqualTo(total);   // 守恒账与噪声无关
    }

    @Test
    void weightedIncrementShouldScale() {
        CountMinSketch sketch = new CountMinSketch(3, 256);
        sketch.increment("heavy", 10_000);
        sketch.increment("light", 1);
        assertThat(sketch.estimate("heavy")).isEqualTo(10_000);
        assertThat(sketch.totalCount()).isEqualTo(10_001);
        assertThat(sketch.depth()).isEqualTo(3);
        assertThat(sketch.width()).isEqualTo(256);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new CountMinSketch(0, 512))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CountMinSketch(4, 0))
                .isInstanceOf(IllegalArgumentException.class);
        CountMinSketch sketch = new CountMinSketch(2, 64);
        assertThatThrownBy(() -> sketch.increment(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.increment("k", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.estimate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
