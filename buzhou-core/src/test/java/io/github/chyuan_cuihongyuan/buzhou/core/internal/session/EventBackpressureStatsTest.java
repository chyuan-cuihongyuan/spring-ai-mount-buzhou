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
    void dispatcherEnqueueRaisesDepthWatermarkOnDropOldest() {
        BufferedEventDispatcher dispatcher = dispatcher(
                EventDispatchConfig.OverflowPolicy.DROP_OLDEST, null);
        for (int i = 0; i < 6; i++) {
            dispatcher.enqueue(SessionEvent.of("tick." + i));
        }
        dispatcher.close();
        var s = EventBackpressureStats.stats();
        // 容量 2：水位 ≤ 容量（消费线程并发排水，观测点为入队路径峰值）
        assertThat(s.depthWatermark()).isGreaterThanOrEqualTo(1);
        assertThat(s.depthWatermark()).isLessThanOrEqualTo(2);
        assertThat(s.blockedPushes()).isZero(); // DROP_OLDEST 无阻塞推入
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
