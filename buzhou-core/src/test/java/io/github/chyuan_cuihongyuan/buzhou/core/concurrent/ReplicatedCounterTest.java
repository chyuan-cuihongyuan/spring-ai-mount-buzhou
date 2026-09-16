package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2022 / T3146：复制计数器合同——per-writer 分量、merge 逐分量
 * max、CRDT 三性质（幂等/交换/结合）、负增量扣减、快照稳定、畸形
 * fail-fast。
 */
class ReplicatedCounterTest {

    @Test
    void singleWriterShouldAccumulate() {
        ReplicatedCounter counter = new ReplicatedCounter();
        counter.increment("w1", 3);
        counter.increment("w1", 4);
        assertThat(counter.value()).isEqualTo(7L);
        assertThat(counter.components()).containsEntry("w1", 7L);
    }

    @Test
    void multipleWritersShouldSumIndependentComponents() {
        ReplicatedCounter counter = new ReplicatedCounter();
        counter.increment("a", 10);
        counter.increment("b", 5);
        counter.increment("a", 2);
        assertThat(counter.value()).isEqualTo(17L); // Σ分量
        assertThat(counter.components()).containsEntry("a", 12L).containsEntry("b", 5L);
    }

    @Test
    void negativeDeltaShouldDeduct() {
        ReplicatedCounter counter = new ReplicatedCounter();
        counter.increment("w", 10);
        counter.increment("w", -4); // PN 扣减
        assertThat(counter.value()).isEqualTo(6L);
    }

    @Test
    void mergeShouldTakePerComponentMax() {
        ReplicatedCounter a = new ReplicatedCounter();
        a.increment("a", 5);
        a.increment("b", 3);
        ReplicatedCounter b = new ReplicatedCounter();
        b.increment("a", 7);  // a 分量更高
        b.increment("c", 2);  // 新 writer 并入
        a.merge(b);
        assertThat(a.components())
                .containsEntry("a", 7L)   // max(5,7)
                .containsEntry("b", 3L)   // 仅 a 有
                .containsEntry("c", 2L);  // 仅 b 有
        assertThat(a.value()).isEqualTo(12L);
    }

    @Test
    void mergeShouldBeIdempotent() {
        ReplicatedCounter a = new ReplicatedCounter();
        a.increment("w", 5);
        Map<String, Long> snapshot = a.components();
        ReplicatedCounter b = new ReplicatedCounter();
        b.increment("w", 3);
        b.merge(snapshot);
        long afterFirst = b.value();
        b.merge(snapshot); // 重复 merge
        assertThat(b.value()).isEqualTo(afterFirst); // 幂等
        assertThat(b.value()).isEqualTo(5L); // max(3,5)
    }

    @Test
    void mergeShouldBeCommutative() {
        ReplicatedCounter x = new ReplicatedCounter();
        x.increment("w", 4);
        ReplicatedCounter y = new ReplicatedCounter();
        y.increment("w", 9);
        ReplicatedCounter xy = new ReplicatedCounter();
        xy.merge(x.components());
        xy.merge(y.components());
        ReplicatedCounter yx = new ReplicatedCounter();
        yx.merge(y.components());
        yx.merge(x.components());
        assertThat(xy.value()).isEqualTo(yx.value()); // 交换
        assertThat(xy.components()).isEqualTo(yx.components());
    }

    @Test
    void concurrentMergeShouldConverge() {
        // 三方并发计数后两两 merge——终态一致（结合性质）
        ReplicatedCounter n1 = new ReplicatedCounter();
        n1.increment("n1", 5);
        ReplicatedCounter n2 = new ReplicatedCounter();
        n2.increment("n2", 3);
        ReplicatedCounter n3 = new ReplicatedCounter();
        n3.increment("n3", 8);
        n1.merge(n2);
        n1.merge(n3);
        n3.merge(n2);
        n3.merge(n1);
        assertThat(n1.components()).isEqualTo(n3.components()); // 收敛同分量
        assertThat(n1.value()).isEqualTo(16L);
    }

    @Test
    void malformedInputsShouldFailFast() {
        ReplicatedCounter counter = new ReplicatedCounter();
        assertThatThrownBy(() -> counter.increment(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> counter.merge((Map<String, Long>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> counter.merge((ReplicatedCounter) null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
