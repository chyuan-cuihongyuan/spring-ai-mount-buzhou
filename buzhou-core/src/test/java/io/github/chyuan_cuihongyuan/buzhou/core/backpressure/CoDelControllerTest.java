package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4018 / T6038：CoDel 合同——观察窗不丢、超窗首丢、间隔递缩、
 * 回标复位、队列门面、畸形 fail-fast。
 */
class CoDelControllerTest {

    private static final long MS = 1_000_000L;

    @Test
    void belowTargetShouldPassAndResetState() {
        long[] tick = {0};
        CoDelController codel = new CoDelController(5 * MS, 100 * MS, () -> tick[0] * MS);
        assertThat(codel.shouldDrop(4 * MS)).isFalse();   // 目标下直过
        tick[0] = 50;
        assertThat(codel.shouldDrop(200 * MS)).isFalse();   // 首超目标——观察窗起算
        assertThat(codel.shouldDrop(200 * MS)).isFalse();   // 窗内不丢
        tick[0] = 60;
        assertThat(codel.shouldDrop(1 * MS)).isFalse();   // 回标——复位
        assertThat(codel.dropCount()).isZero();
    }

    @Test
    void sustainedOverloadShouldDropWithShrinkingInterval() {
        long[] tick = {0};
        CoDelController codel = new CoDelController(5 * MS, 100 * MS, () -> tick[0] * MS);
        tick[0] = 10;
        assertThat(codel.shouldDrop(300 * MS)).isFalse();   // 首超（firstAbove=10ms）
        tick[0] = 60;
        assertThat(codel.shouldDrop(300 * MS)).isFalse();   // 窗内（60−10<100）
        tick[0] = 120;
        assertThat(codel.shouldDrop(300 * MS)).isTrue();   // 超窗首丢
        assertThat(codel.dropCount()).isEqualTo(1);
        tick[0] = 150;
        assertThat(codel.shouldDrop(300 * MS)).isFalse();   // 递缩间隔内（nextDrop=220）
        tick[0] = 230;
        assertThat(codel.shouldDrop(300 * MS)).isTrue();   // 第二丢
        assertThat(codel.dropCount()).isEqualTo(2);
    }

    @Test
    void queueFacadeShouldDropStaleHeadAndPassFresh() {
        long[] tick = {0};
        CoDelController.Queue<String> queue = new CoDelController.Queue<>(5 * MS, 100 * MS,
                () -> tick[0] * MS);
        queue.offer("a");
        queue.offer("b");
        queue.offer("c");
        queue.offer("d");
        tick[0] = 10;   // sojourn 10ms ≥ target 5ms
        assertThat(queue.poll()).isEqualTo("a");   // 首超——观察窗起算仍过
        assertThat(queue.poll()).isEqualTo("b");   // 窗内仍过
        tick[0] = 120;   // 超窗——c 首丢后 d 在递缩间隔内（nextDrop=220）同轮通过
        assertThat(queue.poll()).isEqualTo("d");
        assertThat(queue.poll()).isNull();   // 队空
        assertThat(queue.dropped()).isEqualTo(1);
        assertThat(queue.passed()).isEqualTo(3);
        assertThat(queue.depth()).isZero();
    }

    @Test
    void freshQueueShouldPassEverything() {
        long[] tick = {0};
        CoDelController.Queue<Integer> queue = new CoDelController.Queue<>(5 * MS, 100 * MS,
                () -> tick[0] * MS);
        for (int i = 0; i < 100; i++) {
            queue.offer(i);
            tick[0]++;   // 每毫秒一条——sojourn 恒 1ms < target
        }
        for (int i = 0; i < 100; i++) {
            assertThat(queue.poll()).isEqualTo(i);
        }
        assertThat(queue.dropped()).isZero();
        assertThat(queue.passed()).isEqualTo(100);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new CoDelController(0, 100 * MS, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CoDelController(5 * MS, 5 * MS, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CoDelController(5 * MS, 100 * MS, null))
                .isInstanceOf(IllegalArgumentException.class);
        CoDelController.Queue<String> queue = new CoDelController.Queue<>(5 * MS, 100 * MS,
                () -> 0L);
        assertThatThrownBy(() -> queue.offer(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
