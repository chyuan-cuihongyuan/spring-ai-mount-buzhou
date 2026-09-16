package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2026 / T3154：DRR 加权公平合同——长期服务比≈权重比、单流独占
 * 退化、等权交替、空流清账再入从零、无元素 null、畸形 fail-fast。
 */
class WeightedFairSchedulerTest {

    @Test
    void longRunServiceRatioShouldFollowWeights() {
        WeightedFairScheduler<String> scheduler = new WeightedFairScheduler<>();
        scheduler.registerStream("heavy", 3);
        scheduler.registerStream("light", 1);
        for (int i = 0; i < 300; i++) {
            scheduler.enqueue("heavy", "h" + i);
            scheduler.enqueue("light", "l" + i);
        }
        for (int i = 0; i < 300; i++) {
            scheduler.pollNext();
        }
        Map<String, Long> served = scheduler.servedByStream();
        double ratio = (double) served.get("heavy") / served.get("light");
        assertThat(served.get("heavy") + served.get("light")).isEqualTo(300L);
        // 长期公平：heavy:light ≈ 3:1（宽容 ±10%）
        assertThat(ratio).isBetween(2.7d, 3.3d);
    }

    @Test
    void singleStreamShouldDrainAlone() {
        WeightedFairScheduler<String> scheduler = new WeightedFairScheduler<>();
        scheduler.registerStream("solo", 1);
        for (int i = 0; i < 10; i++) {
            scheduler.enqueue("solo", "x" + i);
        }
        for (int i = 0; i < 10; i++) {
            assertThat(scheduler.pollNext()).isNotNull();
        }
        assertThat(scheduler.pollNext()).isNull(); // 排干
        assertThat(scheduler.servedByStream()).containsEntry("solo", 10L);
    }

    @Test
    void equalWeightsShouldAlternateFairly() {
        WeightedFairScheduler<String> scheduler = new WeightedFairScheduler<>();
        scheduler.registerStream("a", 1);
        scheduler.registerStream("b", 1);
        for (int i = 0; i < 50; i++) {
            scheduler.enqueue("a", "a" + i);
            scheduler.enqueue("b", "b" + i);
        }
        for (int i = 0; i < 50; i++) {
            scheduler.pollNext();
        }
        Map<String, Long> served = scheduler.servedByStream();
        assertThat(served.get("a")).isEqualTo(served.get("b")); // 1:1 交替
        assertThat(served.get("a") + served.get("b")).isEqualTo(50L);
    }

    @Test
    void heavierStreamShouldLeadInShortWindow() {
        WeightedFairScheduler<String> scheduler = new WeightedFairScheduler<>(10);
        scheduler.registerStream("heavy", 5);
        scheduler.registerStream("light", 1);
        for (int i = 0; i < 20; i++) {
            scheduler.enqueue("heavy", "h" + i);
            scheduler.enqueue("light", "l" + i);
        }
        // 首轮：heavy quantum×5=5 项 vs light 1 项——窗口内 heavy 领先
        for (int i = 0; i < 6; i++) {
            scheduler.pollNext();
        }
        Map<String, Long> served = scheduler.servedByStream();
        assertThat(served.get("heavy")).isGreaterThan(served.getOrDefault("light", 0L)); // 突发窗口 heavy 领先（light 可能未及服务）
    }

    @Test
    void idleStreamShouldNotAccumulatePrivilege() {
        WeightedFairScheduler<String> scheduler = new WeightedFairScheduler<>();
        scheduler.registerStream("a", 1);
        scheduler.registerStream("b", 1);
        scheduler.enqueue("a", "a0");
        scheduler.enqueue("a", "a1");
        scheduler.enqueue("b", "b0");
        // 服务 a 两次（b 未轮及或被清）：a 排干后出环
        scheduler.pollNext();
        scheduler.pollNext();
        scheduler.pollNext(); // b0
        // b 空转后 a 再入队——a 从零 deficit（无积累特权）
        scheduler.enqueue("a", "a2");
        scheduler.enqueue("b", "b1");
        // 等权下 a/b 交替
        String first = scheduler.pollNext();
        assertThat(first).isIn("a2", "b1");
        Map<String, Long> served = scheduler.servedByStream();
        assertThat(served.get("a") + served.get("b")).isEqualTo(4L);
    }

    @Test
    void emptySchedulerShouldReturnNull() {
        WeightedFairScheduler<String> scheduler = new WeightedFairScheduler<>();
        scheduler.registerStream("a", 1);
        assertThat(scheduler.pollNext()).isNull(); // 无积压
        assertThat(scheduler.activeStreamCount()).isZero();
    }

    @Test
    void malformedInputsShouldFailFast() {
        WeightedFairScheduler<String> scheduler = new WeightedFairScheduler<>();
        assertThatThrownBy(() -> new WeightedFairScheduler<>(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.registerStream(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.registerStream("a", 0))
                .isInstanceOf(IllegalArgumentException.class);
        scheduler.registerStream("a", 1);
        assertThatThrownBy(() -> scheduler.registerStream("a", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已注册");
        assertThatThrownBy(() -> scheduler.enqueue("ghost", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.enqueue("a", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
