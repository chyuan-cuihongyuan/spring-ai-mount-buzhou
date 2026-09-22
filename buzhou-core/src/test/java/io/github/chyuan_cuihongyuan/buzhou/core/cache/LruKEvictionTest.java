package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1898 / T2998：LRU-K——扫描抗性、K=1 退化、并列、畸形。 */
class LruKEvictionTest {

    /** 扫描抗性：K=2 下扫描键（历史不足）先于热键被逐。 */
    @Test
    void scanResistantEviction() {
        LruKEviction keeper = new LruKEviction(2);
        keeper.record("hot", 10);
        keeper.record("hot", 20);
        keeper.record("hot", 30);
        keeper.record("cold", 40);
        keeper.record("scanned", 50);
        // scanned 与 cold 均只 1 次历史（-∞），hot 已满 2 次 → 先逐 scanned（候选中最早 -∞ 并列取首个）
        String victim = keeper.evictVictim(List.of("hot", "cold", "scanned"));
        assertThat(victim).isIn("cold", "scanned");
        // hot 永不在 victim 位
        assertThat(keeper.evictVictim(List.of("hot"))).isEqualTo("hot");
    }

    /** K=1 退化为传统 LRU：最久未访问者先逐。 */
    @Test
    void kOneDegeneratesToLru() {
        LruKEviction keeper = new LruKEviction(1);
        keeper.record("a", 10);
        keeper.record("b", 20);
        keeper.record("a", 30);
        // a 最近访问 30、b 最近 20 → 逐 b
        assertThat(keeper.evictVictim(List.of("a", "b"))).isEqualTo("b");
    }

    /** 同候选并列：倒数第 K 次更早者先逐（K=2 两满历史键比较）。 */
    @Test
    void earliestKthAccessEvictedFirst() {
        LruKEviction keeper = new LruKEviction(2);
        keeper.record("x", 5);
        keeper.record("x", 100);
        keeper.record("y", 50);
        keeper.record("y", 200);
        // x 的倒数第 2 次 = 5 早于 y 的 50 → 逐 x
        assertThat(keeper.evictVictim(List.of("x", "y"))).isEqualTo("x");
    }

    /** forget 后历史清零；畸形两型 fail-fast（K=0、时间倒退）。 */
    @Test
    void forgetAndMalformed() {
        LruKEviction keeper = new LruKEviction(2);
        keeper.record("a", 10);
        keeper.forget("a");
        assertThat(keeper.evictVictim(List.of("a"))).isNull();
        assertThatThrownBy(() -> new LruKEviction(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("k 不能小于 1");
        assertThatThrownBy(() -> {
            keeper.record("b", 100);
            keeper.record("b", 50);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时间倒退");
    }
}
