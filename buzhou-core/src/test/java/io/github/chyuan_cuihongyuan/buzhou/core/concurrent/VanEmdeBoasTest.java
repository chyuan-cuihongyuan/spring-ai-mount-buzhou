package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6046：VanEmdeBoas 合同——有界宇宙 O(log log u) 集合。
 * 随机操作序列与 TreeSet 圣像逐步全等（insert/delete/contains/
 * successor/最值）；宇宙边界（2、4、1M）；集合语义重复幂等；
 * fail-fast。
 */
class VanEmdeBoasTest {

    @Test
    void randomOpsMatchTreeSetOracle() {
        VanEmdeBoas veb = new VanEmdeBoas(8);
        TreeSet<Integer> oracle = new TreeSet<>();
        Random rng = new Random(6046L);
        for (int i = 0; i < 400; i++) {
            int x = rng.nextInt(256);
            int op = rng.nextInt(3);
            if (op == 0) {
                veb.insert(x);
                oracle.add(x);
            } else if (op == 1) {
                if (oracle.contains(x)) {
                    veb.delete(x);
                    oracle.remove(x);
                } else {
                    assertThatThrownBy(() -> veb.delete(x))
                            .isInstanceOf(IllegalArgumentException.class);
                }
            } else {
                assertThat(veb.contains(x)).isEqualTo(oracle.contains(x));
            }
            assertThat(veb.size()).isEqualTo(oracle.size());
            assertThat(veb.minimum()).isEqualTo(oracle.isEmpty() ? -1 : oracle.first());
            assertThat(veb.maximum()).isEqualTo(oracle.isEmpty() ? -1 : oracle.last());
            Integer oracleSucc = oracle.higher(x);
            assertThat(veb.successor(x)).isEqualTo(oracleSucc == null ? -1 : oracleSucc.intValue());
        }
    }

    @Test
    void tinyUniversesEndToEnd() {
        VanEmdeBoas one = new VanEmdeBoas(1);
        assertThat(one.universeSize()).isEqualTo(2);
        one.insert(0);
        assertThat(one.successor(0)).isEqualTo(-1);
        one.insert(1);
        assertThat(one.successor(0)).isEqualTo(1);
        assertThat(one.minimum()).isEqualTo(0);
        assertThat(one.maximum()).isEqualTo(1);
        one.delete(0);
        assertThat(one.minimum()).isEqualTo(1);
        one.delete(1);
        assertThat(one.isEmpty()).isTrue();
        assertThat(one.minimum()).isEqualTo(-1);

        VanEmdeBoas four = new VanEmdeBoas(2);
        four.insert(3);
        four.insert(0);
        four.insert(1);
        assertThat(four.successor(0)).isEqualTo(1);
        assertThat(four.successor(1)).isEqualTo(3);
        assertThat(four.successor(3)).isEqualTo(-1);
        four.delete(1);
        assertThat(four.successor(0)).isEqualTo(3);
    }

    @Test
    void duplicateInsertIdempotentAndSparseMemory() {
        VanEmdeBoas veb = new VanEmdeBoas(20);
        veb.insert(1 << 19);
        veb.insert(1 << 19);
        veb.insert(7);
        assertThat(veb.size()).isEqualTo(2);
        assertThat(veb.contains(1 << 19)).isTrue();
        assertThat(veb.successor(7)).isEqualTo(1 << 19);
        veb.delete(1 << 19);
        assertThat(veb.maximum()).isEqualTo(7);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new VanEmdeBoas(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VanEmdeBoas(21)).isInstanceOf(IllegalArgumentException.class);
        VanEmdeBoas veb = new VanEmdeBoas(4);
        assertThatThrownBy(() -> veb.insert(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> veb.insert(16)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> veb.contains(16)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> veb.successor(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> veb.delete(5)).isInstanceOf(IllegalArgumentException.class);
    }
}
