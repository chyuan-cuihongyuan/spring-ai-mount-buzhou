package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3010 / T5022：MinHash 合同——同集合估计 1、无交集估计 0、
 * 半重叠跟踪 Jaccard（J=1/3 ±0.1）、offer 幂等（重复不变形）、
 * 空集合语义（双空 NaN/单空 0）、签名长度/回读、交换律、路数
 * 校验（0/负抛、不一致抛）。
 */
class MinHashSketchTest {

    private static MinHashSketch of(int hashes, String... elements) {
        MinHashSketch sketch = new MinHashSketch(hashes);
        for (String e : elements) {
            sketch.offer(e);
        }
        return sketch;
    }

    @Test
    void identicalSetsShouldEstimateOne() {
        MinHashSketch a = of(128, "alpha", "beta", "gamma", "delta");
        MinHashSketch b = of(128, "gamma", "delta", "alpha", "beta");
        assertThat(a.similarityTo(b)).isEqualTo(1.0);
        assertThat(b.similarityTo(a)).isEqualTo(1.0);
    }

    @Test
    void disjointSetsShouldEstimateZero() {
        MinHashSketch a = of(128, "a1", "a2", "a3");
        MinHashSketch b = of(128, "b1", "b2", "b3");
        assertThat(a.similarityTo(b)).isZero();
    }

    @Test
    void oneThirdOverlapShouldTrackJaccard() {
        // A={a..h} 8 元，B={a..d}∪{w..z}——J = 4/12 = 1/3
        MinHashSketch a = of(256, "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8");
        MinHashSketch b = of(256, "e1", "e2", "e3", "e4", "w1", "w2", "w3", "w4");
        assertThat(a.similarityTo(b)).isCloseTo(1.0 / 3.0, within(0.10));
    }

    @Test
    void duplicateOffersShouldBeIdempotent() {
        MinHashSketch once = of(64, "x", "y", "z");
        MinHashSketch twice = new MinHashSketch(64);
        twice.offer("x");
        twice.offer("x");
        twice.offer("y");
        twice.offer("y");
        twice.offer("z");
        assertThat(twice.offerCount()).isEqualTo(5);
        assertThat(twice.similarityTo(once)).isEqualTo(1.0);
    }

    @Test
    void emptySemanticsShouldBeHonest() {
        MinHashSketch empty = new MinHashSketch(64);
        MinHashSketch nonEmpty = of(64, "solo");
        assertThat(empty.similarityTo(new MinHashSketch(64))).isNaN();
        assertThat(empty.similarityTo(nonEmpty)).isZero();
        assertThat(nonEmpty.similarityTo(empty)).isZero();
    }

    @Test
    void signatureShouldMatchHashCountAndBeDefensive() {
        MinHashSketch sketch = of(96, "k");
        assertThat(sketch.hashCount()).isEqualTo(96);
        long[] signature = sketch.signature();
        assertThat(signature).hasSize(96);
        signature[0] = 42L;
        assertThat(sketch.signature()[0]).isNotEqualTo(42L);
    }

    @Test
    void sameElementsShouldYieldDeterministicSignature() {
        long[] first = of(64, "m", "n").signature();
        long[] second = of(64, "n", "m").signature();
        assertThat(first).isEqualTo(second);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new MinHashSketch(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MinHashSketch(-5)).isInstanceOf(IllegalArgumentException.class);
        MinHashSketch a = of(32, "x");
        MinHashSketch b = of(64, "x");
        assertThatThrownBy(() -> a.similarityTo(b)).isInstanceOf(IllegalArgumentException.class);
    }
}
