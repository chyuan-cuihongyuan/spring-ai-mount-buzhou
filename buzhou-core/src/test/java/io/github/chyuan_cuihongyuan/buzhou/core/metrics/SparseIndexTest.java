package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.SparseIndex.Block;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5021 / T6144：稀疏索引合同——中块定位、早于首块 -1、
 * 块界精确、晚于末块落末块、乱序/空 fail-fast、确定性。
 */
class SparseIndexTest {

    private static SparseIndex newIndex() {
        return new SparseIndex(List.of(
                new Block(0, "a"),
                new Block(1, "e"),
                new Block(2, "m"),
                new Block(3, "t")));
    }

    @Test
    void locateShouldFindEnclosingBlock() {
        SparseIndex index = newIndex();
        assertThat(index.locate("a")).isEqualTo(0L);    // 块界精确命中
        assertThat(index.locate("b")).isEqualTo(0L);    // 落 [a,e) 块
        assertThat(index.locate("e")).isEqualTo(1L);    // 界键属新块
        assertThat(index.locate("x")).isEqualTo(3L);    // 晚于末块落末块
    }

    @Test
    void beforeFirstBlockShouldBeHonestMinusOne() {
        SparseIndex index = newIndex();
        assertThat(index.locate("0")).isEqualTo(-1L);
        assertThat(index.locate("9")).isEqualTo(-1L);   // '9' < 'a' 字典序
    }

    @Test
    void lastBlockBoundaryShouldResolveToLast() {
        SparseIndex index = newIndex();
        assertThat(index.locate("t")).isEqualTo(3L);
        assertThat(index.locate("zzz")).isEqualTo(3L);
    }

    @Test
    void sameIndexShouldReplaySameLocation() {
        SparseIndex first = newIndex();
        SparseIndex second = newIndex();
        for (String key : List.of("a", "d", "e", "x", "0")) {
            assertThat(first.locate(key)).isEqualTo(second.locate(key));
        }
        assertThat(first.blockCount()).isEqualTo(4);
    }

    @Test
    void unorderedOrEmptyBlocksShouldFailFast() {
        assertThatThrownBy(() -> new SparseIndex(List.of(
                new Block(0, "m"), new Block(1, "a"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SparseIndex(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SparseIndex(List.of(new Block(0, ""))))
                .isInstanceOf(IllegalArgumentException.class);
        SparseIndex index = newIndex();
        assertThatThrownBy(() -> index.locate(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
