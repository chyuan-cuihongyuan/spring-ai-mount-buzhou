package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7034：ZobristHashing 合同——表驱动异或哈希。
 * 同表确定性；异或交换律（顺序无关增量）；空态 0；不同
 * 状态不同哈希（种子钉住）；fail-fast。
 */
class ZobristHashingTest {

    @Test
    void deterministicAndOrderIndependent() {
        ZobristHashing board = new ZobristHashing(8, 3, 7034L);
        long whole = board.hash(new long[]{0, 1, 2, -1, 1});
        long incremental = 0;
        incremental = board.xorIn(incremental, 0, 0);
        incremental = board.xorIn(incremental, 4, 1);
        incremental = board.xorIn(incremental, 1, 1);
        incremental = board.xorIn(incremental, 2, 2);
        assertThat(incremental).isEqualTo(whole);
        assertThat(board.hash(new long[]{0, 1, 2, -1, 1}))
                .isEqualTo(board.hash(new long[]{0, 1, 2, -1, 1}));
        assertThat(board.hash(new long[]{-1, -1, -1, -1, -1})).isZero();
    }

    @Test
    void xorOutReversesIn() {
        ZobristHashing board = new ZobristHashing(4, 2, 9L);
        long hash = 0;
        hash = board.xorIn(hash, 0, 0);
        hash = board.xorIn(hash, 3, 1);
        hash = board.xorOut(hash, 0, 0);
        long direct = board.xorIn(0, 3, 1);
        assertThat(hash).isEqualTo(direct);
        assertThat(board.hash(new long[]{-1, -1, -1, 1})).isEqualTo(direct);
    }

    @Test
    void distinctStatesDistinctHashesAndFailFast() {
        ZobristHashing board = new ZobristHashing(4, 2, 7034L);
        assertThat(board.hash(new long[]{0, -1, -1, -1}))
                .isNotEqualTo(board.hash(new long[]{-1, 0, -1, -1}));
        assertThat(board.codeAt(0, 0)).isEqualTo(board.codeAt(0, 0));
        assertThatThrownBy(() -> board.codeAt(4, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> board.codeAt(0, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ZobristHashing(0, 1, 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ZobristHashing(1, 0, 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> board.hash(new long[]{5})).isInstanceOf(IllegalArgumentException.class);
    }
}
