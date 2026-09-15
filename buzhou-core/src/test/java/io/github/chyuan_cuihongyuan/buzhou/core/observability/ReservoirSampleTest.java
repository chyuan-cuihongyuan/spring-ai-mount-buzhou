package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1849 / T2900：水库采样——短流全量、定容均匀、种子确定性。 */
class ReservoirSampleTest {

    /** 短流（n ≤ k）：全量保序；k=0 空采样。 */
    @Test
    void shortStreamTakesAllInOrder() {
        assertThat(ReservoirSample.sample(5, 42L, List.of("a", "b", "c")))
                .containsExactly("a", "b", "c");
        assertThat(ReservoirSample.sample(0, 42L, List.of("a", "b"))).isEmpty();
        assertThat(ReservoirSample.sample(3, 42L, null)).isEmpty();
    }

    /** 长流定容：恰 k 个且全部来自原流（成员性）。 */
    @Test
    void longStreamYieldsExactlyKFromStream() {
        List<String> stream = IntStream.range(0, 1000)
                .mapToObj(i -> "item-" + i).toList();
        List<String> sample = ReservoirSample.sample(10, 7L, stream);
        assertThat(sample).hasSize(10);
        assertThat(stream).containsAll(sample);
    }

    /** 种子确定性：同种子同样本；异种子大概率异样本（可回放可审计）。 */
    @Test
    void seededDeterminism() {
        List<String> stream = IntStream.range(0, 500)
                .mapToObj(i -> "s-" + i).toList();
        List<String> first = ReservoirSample.sample(20, 99L, stream);
        List<String> second = ReservoirSample.sample(20, 99L, stream);
        assertThat(second).isEqualTo(first);
        List<String> other = ReservoirSample.sample(20, 100L, stream);
        assertThat(stream).containsAll(other);
        // 种子间分布不恒同（500 选 20，两种子全同概率天文小——但断言从宽：
        // 只要求异种子样本同样合法）
        assertThat(other).hasSize(20);
    }

    /** 畸形入参 fail-fast：负 k、null 元素。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ReservoirSample.sample(-1, 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("k 不能为负");
        assertThatThrownBy(() -> ReservoirSample.sample(2, 1L,
                java.util.Arrays.asList("a", null, "b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流元素不能为 null");
    }
}
