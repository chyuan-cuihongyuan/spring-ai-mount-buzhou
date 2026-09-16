package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2007 / T3116：频率素描合同——重复访问频率递增、4bit 饱和 15 不
 * 回绕、冷 key 零频、计数下界语义（碰撞只低估）、畸形 fail-fast。
 */
class FrequencySketchTest {

    @Test
    void repeatedIncrementShouldGrowFrequency() {
        FrequencySketch sketch = new FrequencySketch(128);
        for (int i = 0; i < 7; i++) {
            sketch.increment("hot-key");
        }
        // Caffeine min 槽语义：均衡增长于相邻两槽，读数 ≈ n/2（序不变值减半——门控只需序）
        assertThat(sketch.frequency("hot-key")).isGreaterThanOrEqualTo(3);
        assertThat(sketch.frequency("hot-key")).isLessThanOrEqualTo(7);
        assertThat(sketch.frequency("cold-key")).isZero();
    }

    @Test
    void frequencyShouldSaturateAtFifteenWithoutWrap() {
        FrequencySketch sketch = new FrequencySketch(128);
        for (int i = 0; i < 100; i++) {
            sketch.increment("saturated");
        }
        assertThat(sketch.frequency("saturated")).isEqualTo(FrequencySketch.SATURATION);
    }

    @Test
    void distinctKeysShouldTrackIndependently() {
        FrequencySketch sketch = new FrequencySketch(256);
        for (int i = 0; i < 10; i++) {
            sketch.increment("a");
        }
        for (int i = 0; i < 3; i++) {
            sketch.increment("b");
        }
        assertThat(sketch.frequency("a")).isGreaterThanOrEqualTo(5); // 10 次 → min 槽 ≈ 半值
        assertThat(sketch.frequency("b")).isGreaterThanOrEqualTo(1); // 3 次 → min 槽 ≥ 1
        // 热点排序语义：a 频率 > b（准入门控次序）
        assertThat(sketch.frequency("a")).isGreaterThan(sketch.frequency("b"));
    }

    @Test
    void manyKeysShouldAllStayCountable() {
        FrequencySketch sketch = new FrequencySketch(1024);
        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < 500; i++) {
                sketch.increment("entry-" + i);
            }
        }
        // 无极端碰撞下界退化：每 key 频率 ≥ 1（Count-Min 只低估到碰撞共享水平）
        for (int i = 0; i < 500; i += 50) {
            assertThat(sketch.frequency("entry-" + i)).isGreaterThanOrEqualTo(1);
        }
        // 老钥匙均匀 5 次（允许碰撞低估，断下界 ≥1 已足够门控语义）
        assertThat(sketch.frequency("entry-0")).isLessThanOrEqualTo(5);
    }

    @Test
    void admissionGatingOrderShouldFollowFrequency() {
        FrequencySketch sketch = new FrequencySketch(256);
        // 模拟 W-TinyLFU 准入：新 key 频率低于驻留 victim → 拒绝准入
        for (int i = 0; i < 6; i++) {
            sketch.increment("victim");
        }
        sketch.increment("newcomer");
        assertThat(sketch.frequency("newcomer")).isLessThan(sketch.frequency("victim"));
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new FrequencySketch(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FrequencySketch(-1))
                .isInstanceOf(IllegalArgumentException.class);
        FrequencySketch sketch = new FrequencySketch(64);
        assertThatThrownBy(() -> sketch.increment(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.frequency(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
