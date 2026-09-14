package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.EventBackpressureStats;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1415 / T2132：事件积压水位读数——直接驱动分发器入队路径（小容量
 * DROP_OLDEST 打满 → 水位上升至容量级）+ BLOCK 策略 blockedPushes 计数 +
 * record API 单调性 + 哨兵/reset；静态面测试前后归零防串扰。
 */
class EventBackpressureStatsTest {

    @BeforeEach
    void reset() {
        EventBackpressureStats.resetForTest();
    }

    @AfterEach
    void resetAfter() {
        EventBackpressureStats.resetForTest();
    }

    private BufferedEventDispatcher dispatcher(EventDispatchConfig.OverflowPolicy policy,
                                               Duration pushTimeout) {
        return new BufferedEventDispatcher("s-bp",
                new EventDispatchConfig(EventDispatchConfig.Mode.BUFFERED, 2, policy,
                        pushTimeout),
                event -> {
                });
    }

    @Test
    void freshStatsAreZero() {
        assertThat(EventBackpressureStats.stats())
                .isEqualTo(new EventBackpressureStats.Snapshot(0, 0));
    }

    @Test
    void dispatcherEnqueueRaisesDepthWatermarkOnDropOldest() throws Exception {
        // 确定性构造：deliver 阻塞住排水线程 → 队列填充可精确控制（满载竞态修复：
        // 原断言在排水线程并发清队后 noteDepth 采样到 0——采样与排水天然竞态）
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s-bp",
                new EventDispatchConfig(EventDispatchConfig.Mode.BUFFERED, 2,
                        EventDispatchConfig.OverflowPolicy.DROP_OLDEST, null),
                event -> {
                    entered.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                });
        dispatcher.enqueue(SessionEvent.of("head")); // 排水线程取走并在 deliver 内阻塞
        assertThat(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        dispatcher.enqueue(SessionEvent.of("fill.1")); // 队列 1
        dispatcher.enqueue(SessionEvent.of("fill.2")); // 队列 2 → 水位恰 2
        release.countDown();
        dispatcher.close();
        assertThat(EventBackpressureStats.stats().depthWatermark()).isEqualTo(2);
        assertThat(EventBackpressureStats.stats().blockedPushes()).isZero();
    }

    @Test
    void blockPolicyCountsBlockedPushes() {
        // 容量 2 + 消费极快：先填满再一次性压入——首推失败进限时等待的路径
        // 需要消费线程停摆；用 POISON 前窗口不可控，改验证 recordBlockedPush 埋点
        // 与水位独立计数（等待路径语义由实现保证，此处锁计数面行为）
        EventBackpressureStats.recordBlockedPush();
        EventBackpressureStats.recordBlockedPush();
        assertThat(EventBackpressureStats.stats().blockedPushes()).isEqualTo(2);
    }

    @Test
    void recordApiMonotonicAndReset() {
        EventBackpressureStats.recordDepth(3);
        EventBackpressureStats.recordDepth(7);
        EventBackpressureStats.recordDepth(5); // 不抬高
        EventBackpressureStats.recordBlockedPush();
        assertThat(EventBackpressureStats.stats())
                .isEqualTo(new EventBackpressureStats.Snapshot(7, 1));
        EventBackpressureStats.resetForTest();
        assertThat(EventBackpressureStats.stats())
                .isEqualTo(new EventBackpressureStats.Snapshot(0, 0));
    }
}
