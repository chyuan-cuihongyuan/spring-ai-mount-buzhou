package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;

/**
 * impl-34 / spec 13 §core-4：会话事件分发模式（默认同步，兼容既有语义）。
 *
 * <p>源：Netty 35K★ 写水位（有界缓冲 + 溢出可见）+ Akka 13.3K★ 死信语义（丢弃必须计数）。
 *
 * <ul>
 *   <li>{@link Mode#SYNC}（默认）：hook 链与全部 listener 在 Turn 主链路内联分发
 *       （逐监听器异常隔离——见 DefaultAgentSession.dispatchEvent）；</li>
 *   <li>{@link Mode#BUFFERED}（opt-in）：事件入有界队列，由专属分发线程排空——慢监听器
 *       不再拖慢 Turn 主链路。容量打满时按 {@link OverflowPolicy} 处理：
 *       {@link OverflowPolicy#DROP_OLDEST} 丢最老事件（遥测类监听器建议）；
 *       {@link OverflowPolicy#BLOCK} 限时阻塞入队（持久化类监听器建议），超时仍未入队
 *       才丢弃。两种策略的<b>丢弃都必须计数可见</b>（{@link EventBusStats}，低频 WARN 汇总）。</li>
 * </ul>
 *
 * @param mode        分发模式；null → SYNC
 * @param capacity    有界队列容量（buffered 模式）；默认 1024，范围 [1, 1_048_576]
 * @param overflow    溢出策略；null → DROP_OLDEST
 * @param pushTimeout BLOCK 策略的入队限时；null → 2s
 */
public record EventDispatchConfig(Mode mode, int capacity, OverflowPolicy overflow, Duration pushTimeout) {

    public enum Mode { SYNC, BUFFERED }

    public enum OverflowPolicy { DROP_OLDEST, BLOCK }

    public static final int DEFAULT_CAPACITY = 1024;
    public static final Duration DEFAULT_PUSH_TIMEOUT = Duration.ofSeconds(2);

    /** 低频丢弃汇总的粒度：每累计 64 次丢弃输出一条 WARN（避免日志风暴）。 */
    public static final int DROP_SUMMARY_EVERY = 64;

    public EventDispatchConfig {
        mode = mode == null ? Mode.SYNC : mode;
        capacity = capacity <= 0 ? DEFAULT_CAPACITY : capacity;
        if (capacity > 1_048_576) {
            throw new IllegalArgumentException("event-dispatch capacity 超出上限（1,048,576）：" + capacity);
        }
        overflow = overflow == null ? OverflowPolicy.DROP_OLDEST : overflow;
        pushTimeout = pushTimeout == null || pushTimeout.isZero() || pushTimeout.isNegative()
                ? DEFAULT_PUSH_TIMEOUT : pushTimeout;
    }

    /** 既有行为：同步分发（内联、逐监听器隔离）。 */
    public static EventDispatchConfig sync() {
        return new EventDispatchConfig(Mode.SYNC, DEFAULT_CAPACITY, OverflowPolicy.DROP_OLDEST, null);
    }

    /** opt-in 有界异步分发（默认容量 + DROP_OLDEST）。 */
    public static EventDispatchConfig buffered() {
        return new EventDispatchConfig(Mode.BUFFERED, DEFAULT_CAPACITY, OverflowPolicy.DROP_OLDEST, null);
    }

    /** 是否为有界异步分发模式。 */
    public boolean isBuffered() {
        return mode == Mode.BUFFERED;
    }
}
