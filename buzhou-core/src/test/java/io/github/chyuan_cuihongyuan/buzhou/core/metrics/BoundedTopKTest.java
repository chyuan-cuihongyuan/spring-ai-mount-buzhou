package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2055 / T3212：Top-K 合同——守门员逐换、严格大于同分保位、降序
 * 快照、O(K) 有界、双计数、畸形 fail-fast。
 */
class BoundedTopKTest {

    @Test
    void topScoresShouldBeKeptInOrder() {
        BoundedTopK<String> board = new BoundedTopK<>(3);
        board.offer(5, "a");
        board.offer(9, "b");
        board.offer(7, "c");
        board.offer(1, "d"); // 落选
        List<BoundedTopK.Entry<String>> top = board.top();
        assertThat(top).extracting(BoundedTopK.Entry::item)
                .containsExactly("b", "c", "a"); // 降序 9,7,5
        assertThat(board.stats().rejected()).isEqualTo(1L);
    }

    @Test
    void beatingGatekeeperShouldEvictHim() {
        BoundedTopK<String> board = new BoundedTopK<>(2);
        board.offer(1, "small");
        board.offer(2, "mid");
        assertThat(board.gatekeeperScore()).isEqualTo(1.0d); // 守门员=最小
        board.offer(3, "big"); // 胜守门员
        assertThat(board.top()).extracting(BoundedTopK.Entry::item)
                .containsExactly("big", "mid"); // small 被逐
        assertThat(board.stats().evicted()).isEqualTo(1L);
    }

    @Test
    void tieWithGatekeeperShouldKeepIncumbent() {
        BoundedTopK<String> board = new BoundedTopK<>(2);
        board.offer(5, "first");
        board.offer(5, "second");
        board.offer(5, "third"); // 同分守门员——先入者保位
        assertThat(board.size()).isEqualTo(2);
        assertThat(board.top()).extracting(BoundedTopK.Entry::item)
                .containsExactlyInAnyOrder("first", "second");
        assertThat(board.stats().rejected()).isEqualTo(1L);
    }

    @Test
    void underfilledBoardShouldAcceptEverything() {
        BoundedTopK<Integer> board = new BoundedTopK<>(10);
        for (int i = 0; i < 5; i++) {
            board.offer(i, i);
        }
        assertThat(board.size()).isEqualTo(5);
        assertThat(board.stats().evicted()).isZero();
        assertThat(board.gatekeeperScore()).isZero(); // 未满时堆顶=最小=0
    }

    @Test
    void emptyBoardGatekeeperShouldBeNegativeInfinity() {
        assertThat(new BoundedTopK<String>(3).gatekeeperScore())
                .isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    void streamOfManyShouldStayBounded() {
        BoundedTopK<Integer> board = new BoundedTopK<>(5);
        for (int i = 0; i < 10_000; i++) {
            board.offer(i, i);
        }
        assertThat(board.size()).isEqualTo(5); // O(K) 恒定
        assertThat(board.top()).extracting(BoundedTopK.Entry::item)
                .containsExactly(9_999, 9_998, 9_997, 9_996, 9_995);
        // 递增流：每值都胜守门员——全程逐换零落选（rejected=0、evicted=9995）
        assertThat(board.stats().rejected()).isZero();
        assertThat(board.stats().evicted()).isEqualTo(9_995L);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new BoundedTopK<String>(0))
                .isInstanceOf(IllegalArgumentException.class);
        BoundedTopK<String> board = new BoundedTopK<>(2);
        assertThatThrownBy(() -> board.offer(1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> board.offer(Double.NaN, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
