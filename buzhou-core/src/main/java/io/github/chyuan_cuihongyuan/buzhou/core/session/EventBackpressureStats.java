package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件总线积压水位读数（spec 1415 / T2131 / impl 1068）——Kafka consumer
 * lag（积压水位先于丢数显形）思想：{@link EventBusStats} 的 queueDepth 是
 * 瞬时值，「队列最深到过多少、发生过多少次限时阻塞推入」无历史面——
 * 水位逼近容量 = 丢弃将至的预警信号，容量规划（EventDispatchConfig.capacity）
 * 从拍脑袋变水位推导。
 *
 * <p>进程级静态读面（ToolArgsValidator.validationStats / StructuredOutputStats
 * 同款先例）：埋点在 internal 事件分发器的入队路径（只增记账），读面归本公共类；
 * {@link #resetForTest()} 为归零注入点。多会话共享一个进程级水位（同一 JVM 的
 * 总量治理视角——按会话分桶属基数红线）。
 */
public final class EventBackpressureStats {

    private static final AtomicInteger DEPTH_WATERMARK = new AtomicInteger();
    private static final AtomicLong BLOCKED_PUSHES = new AtomicLong();

    private EventBackpressureStats() {
    }

    /** 分发器埋点：队列深度采样（每次入队路径变更后调用）。 */
    public static void recordDepth(int depth) {
        if (depth < 0) {
            return;
        }
        DEPTH_WATERMARK.accumulateAndGet(depth, Math::max);
    }

    /** 分发器埋点：BLOCK 策略进入限时等待（首次 offer 失败即记一次）。 */
    public static void recordBlockedPush() {
        BLOCKED_PUSHES.incrementAndGet();
    }

    /** 只读快照：深度水位 + 限时阻塞推入累计。 */
    public static Snapshot stats() {
        return new Snapshot(DEPTH_WATERMARK.get(), BLOCKED_PUSHES.get());
    }

    /** 测试归零口：静态读数的 reset 注入点。 */
    public static void resetForTest() {
        DEPTH_WATERMARK.set(0);
        BLOCKED_PUSHES.set(0);
    }

    /**
     * @param depthWatermark 队列深度历史峰值（跨会话进程级；0 = 从未积压）
     * @param blockedPushes  BLOCK 策略限时等待推入累计（>0 即容量曾被打满）
     */
    public record Snapshot(int depthWatermark, long blockedPushes) {
    }
}
