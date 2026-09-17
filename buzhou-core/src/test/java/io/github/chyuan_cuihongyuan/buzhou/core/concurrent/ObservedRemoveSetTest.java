package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3027 / T5056：OR-Set 合同——增删基础、顺序重加胜、并发
 * add-remove add 胜（皇冠场景：remove 只墓碑观察到的标签）、全量
 * 同步后删除两副本皆清、merge 幂等、merge 交换律、多元素隔离、
 * 副本号校验。
 */
class ObservedRemoveSetTest {

    @Test
    void addAndRemoveBasics() {
        ObservedRemoveSet<String> set = new ObservedRemoveSet<>("a");
        set.add("x");
        assertThat(set.contains("x")).isTrue();
        assertThat(set.size()).isEqualTo(1);
        set.remove("x");
        assertThat(set.contains("x")).isFalse();
        assertThat(set.elements()).isEmpty();
        // remove 不存在的元素无副作用
        set.remove("never");
        assertThat(set.size()).isZero();
    }

    @Test
    void sequentialReAddAfterRemoveShouldWin() {
        ObservedRemoveSet<String> set = new ObservedRemoveSet<>("a");
        set.add("x");
        set.remove("x");
        set.add("x");
        assertThat(set.contains("x")).isTrue();  // 新标签未墓碑
    }

    @Test
    void concurrentAddRemoveShouldLetAddWin() {
        // 皇冠场景：A 加 x → 同步 B → B 删 x（只墓碑 A 首标签）→
        // A 再加 x（新标签）→ 互相同步 → x 幸存（add 胜）
        ObservedRemoveSet<String> a = new ObservedRemoveSet<>("A");
        ObservedRemoveSet<String> b = new ObservedRemoveSet<>("B");
        a.add("x");
        b.merge(a);
        b.remove("x");
        a.add("x");
        a.merge(b);
        b.merge(a);
        assertThat(a.contains("x")).isTrue();
        assertThat(b.contains("x")).isTrue();
    }

    @Test
    void removeAfterFullSyncShouldClearEverywhere() {
        // 两副本各自加同一元素并互相同步后，A 删除（观察到两标签）
        // → 再同步 → 两副本皆清
        ObservedRemoveSet<String> a = new ObservedRemoveSet<>("A");
        ObservedRemoveSet<String> b = new ObservedRemoveSet<>("B");
        a.add("k");
        b.add("k");
        a.merge(b);
        b.merge(a);
        a.remove("k");
        b.merge(a);
        assertThat(a.contains("k")).isFalse();
        assertThat(b.contains("k")).isFalse();
    }

    @Test
    void mergeShouldBeIdempotent() {
        ObservedRemoveSet<String> a = new ObservedRemoveSet<>("A");
        ObservedRemoveSet<String> b = new ObservedRemoveSet<>("B");
        a.add("x");
        a.add("y");
        a.remove("y");
        b.add("z");
        a.merge(b);
        var once = a.elements();
        a.merge(b);
        assertThat(a.elements()).isEqualTo(once);
    }

    @Test
    void mergeShouldBeCommutative() {
        ObservedRemoveSet<String> a = new ObservedRemoveSet<>("A");
        ObservedRemoveSet<String> b = new ObservedRemoveSet<>("B");
        a.add("x");
        b.add("y");
        b.remove("y");
        ObservedRemoveSet<String> ab = new ObservedRemoveSet<>("A");
        ab.merge(a);
        ab.merge(b);
        ObservedRemoveSet<String> ba = new ObservedRemoveSet<>("B");
        ba.merge(b);
        ba.merge(a);
        assertThat(ab.elements()).isEqualTo(ba.elements());
        assertThat(ab.replicaId()).isEqualTo("A");
        assertThat(ba.replicaId()).isEqualTo("B");
    }

    @Test
    void elementsShouldBeIsolated() {
        ObservedRemoveSet<String> set = new ObservedRemoveSet<>("a");
        set.add("p");
        set.add("q");
        set.add("r");
        set.remove("q");
        assertThat(set.elements()).containsExactly("p", "r");
        assertThat(set.size()).isEqualTo(2);
    }

    @Test
    void replicaIdMustBePresent() {
        assertThatThrownBy(() -> new ObservedRemoveSet<String>(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ObservedRemoveSet<String>(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
