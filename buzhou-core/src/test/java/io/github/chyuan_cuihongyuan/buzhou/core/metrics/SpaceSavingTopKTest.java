package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.SpaceSavingTopK.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4002 / T6006：Space-Saving 合同——少容量精确、淘汰继承高估
 * 单侧、榜单排序、守恒与畸形 fail-fast。
 */
class SpaceSavingTopKTest {

    @Test
    void withinCapacityShouldCountExactly() {
        SpaceSavingTopK top = new SpaceSavingTopK(5);
        for (int i = 0; i < 7; i++) {
            top.observe("a");
        }
        for (int i = 0; i < 3; i++) {
            top.observe("b");
        }
        top.observe("c");
        assertThat(top.estimate("a")).isEqualTo(7);   // 无淘汰即精确
        assertThat(top.estimate("b")).isEqualTo(3);
        assertThat(top.estimate("c")).isEqualTo(1);
        assertThat(top.top()).extracting(Entry::key, Entry::count)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("a", 7L),
                        org.assertj.core.groups.Tuple.tuple("b", 3L),
                        org.assertj.core.groups.Tuple.tuple("c", 1L));
        assertThat(top.totalObservations()).isEqualTo(11);
        assertThat(top.minCount()).isEqualTo(1);
    }

    @Test
    void evictionShouldInheritMinCountAndOverestimateOnly() {
        SpaceSavingTopK top = new SpaceSavingTopK(2);
        for (int i = 0; i < 5; i++) {
            top.observe("a");
        }
        top.observe("b");   // a:5 b:1
        top.observe("c");   // 淘汰 b(min=1) → c:2
        top.observe("d");   // 淘汰 c(min=2) → d:3
        top.observe("e");   // 淘汰 d(min=3) → e:4
        assertThat(top.estimate("a")).isEqualTo(5);   // 热键幸存且精确
        assertThat(top.estimate("e")).isEqualTo(4);   // 继承链 ≥ 真值 1
        assertThat(top.estimate("e")).isGreaterThanOrEqualTo(1);
        assertThat(top.estimate("b")).isZero();       // 被淘汰键读零
        assertThat(top.top()).extracting(Entry::key).containsExactly("a", "e");
        assertThat(top.minCount()).isEqualTo(4);      // 误差界随继承累积
        assertThat(top.totalObservations()).isEqualTo(9);
    }

    @Test
    void heavyKeysShouldSurviveChurn() {
        SpaceSavingTopK top = new SpaceSavingTopK(3);
        for (int round = 0; round < 10; round++) {
            top.observe("hot1");
            top.observe("hot2");
            top.observe("churn-" + round);   // 每轮一次性新键搅局
        }
        java.util.List<Entry> board = top.top();
        assertThat(board.get(0).key()).isEqualTo("hot1");
        assertThat(board.get(0).count()).isEqualTo(10);   // 恒在榜者精确
        assertThat(board.get(1).key()).isEqualTo("hot2");
        assertThat(board.get(1).count()).isEqualTo(10);
        assertThat(board.get(2).key()).startsWith("churn-");   // 第三席被搅局键轮替
        assertThat(board.get(2).count()).isGreaterThanOrEqualTo(10);   // 继承 ≥ 真值
        assertThat(top.totalObservations()).isEqualTo(30);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SpaceSavingTopK(0))
                .isInstanceOf(IllegalArgumentException.class);
        SpaceSavingTopK top = new SpaceSavingTopK(1);
        assertThatThrownBy(() -> top.observe(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> top.estimate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
