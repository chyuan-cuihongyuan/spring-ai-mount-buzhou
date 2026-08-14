package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局指标实例持有者（impl-41 / spec 13 §T66）：默认 {@link BuzhouMetrics#noop()}；
 * 装配层（有 micrometer 时）经 {@link #install} 安装实现。
 *
 * <p><b>为什么全局单点</b>：关键路径（事件分发器 / 会话工具执行 / 存储写路径）构造面广，
 * 逐点注入构造器会侵入每个机制的装配链；Netty {@code ResourceLeakDetector.setLevel}
 * 同款全局旋钮模式——库内默认 no-op 零开销，应用侧装配一次全局生效。
 * 测试可 {@link #install} 后 {@link #reset()}（@AfterEach 清理纪律）。
 */
public final class BuzhouMetricsHolder {

    private static final AtomicReference<BuzhouMetrics> INSTANCE =
            new AtomicReference<>(BuzhouMetrics.noop());

    private BuzhouMetricsHolder() {
    }

    public static BuzhouMetrics metrics() {
        return INSTANCE.get();
    }

    public static void install(BuzhouMetrics metrics) {
        INSTANCE.set(metrics == null ? BuzhouMetrics.noop() : metrics);
    }

    /** 恢复 no-op 默认（测试清理用）。 */
    public static void reset() {
        INSTANCE.set(BuzhouMetrics.noop());
    }
}
