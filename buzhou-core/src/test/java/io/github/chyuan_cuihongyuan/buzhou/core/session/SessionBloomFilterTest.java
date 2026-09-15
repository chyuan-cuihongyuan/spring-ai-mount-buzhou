package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1826 / T2854：布隆粗筛——零假阴性、误报有界、确定性、饱和度。 */
class SessionBloomFilterTest {

    /** 零假阴性：见过的全部报见过（布隆的根本契约）。 */
    @Test
    void addedSessionsAreAlwaysReported() {
        SessionBloomFilter filter = new SessionBloomFilter();
        IntStream.range(0, 100).mapToObj(i -> "session-" + i).forEach(filter::add);
        IntStream.range(0, 100)
                .forEach(i -> assertThat(filter.mightContain("session-" + i))
                        .as("session-%d 加入后必须报见过（零假阴性）", i).isTrue());
    }

    /** 空布隆零置位：任何输入都报没见过；误报有界（千探针 <5%）。 */
    @Test
    void emptyFilterRejectsAllAndFalsePositivesBounded() {
        SessionBloomFilter empty = new SessionBloomFilter();
        IntStream.range(0, 50)
                .forEach(i -> assertThat(empty.mightContain("s-" + i)).isFalse());
        assertThat(empty.fillRatio()).isZero();

        SessionBloomFilter filter = new SessionBloomFilter();
        IntStream.range(0, 100).mapToObj(i -> "session-" + i).forEach(filter::add);
        long falsePositives = IntStream.range(0, 1000)
                .filter(i -> filter.mightContain("unseen-" + i)).count();
        // 理论误报率 ~0.1%；确定性哈希下该断言稳定
        assertThat(falsePositives).isLessThan(50L);
        assertThat(filter.fillRatio()).isGreaterThan(0).isLessThan(
                SessionBloomFilter.SATURATION_THRESHOLD);
    }

    /** 确定性：同参两布隆同答案（无随机数可回放）。 */
    @Test
    void deterministicAcrossInstances() {
        SessionBloomFilter a = new SessionBloomFilter();
        SessionBloomFilter b = new SessionBloomFilter();
        IntStream.range(0, 50).mapToObj(i -> "s-" + i).forEach(id -> {
            a.add(id);
            b.add(id);
        });
        IntStream.range(-20, 70)
                .forEach(i -> assertThat(a.mightContain("s-" + i))
                        .isEqualTo(b.mightContain("s-" + i)));
        assertThat(a.fillRatio()).isEqualTo(b.fillRatio());
    }

    /** 幂等加入：重复 add 不再抬升饱和度。 */
    @Test
    void repeatedAddIsIdempotent() {
        SessionBloomFilter filter = new SessionBloomFilter();
        IntStream.range(0, 30).mapToObj(i -> "s-" + i).forEach(filter::add);
        double once = filter.fillRatio();
        IntStream.range(0, 30).mapToObj(i -> "s-" + i).forEach(filter::add);
        assertThat(filter.fillRatio()).isEqualTo(once);
    }

    /** 畸形入参 fail-fast：空白 id、bits/hashes 越界。 */
    @Test
    void malformedInputFailsFast() {
        SessionBloomFilter filter = new SessionBloomFilter();
        assertThatThrownBy(() -> filter.add(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> filter.mightContain(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionBloomFilter(32, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bits 不能小于 64");
        assertThatThrownBy(() -> new SessionBloomFilter(4096, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
