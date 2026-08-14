package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.time.Duration;

/**
 * 内部指标记录面（impl-41 / spec 13 §T66，命名约定 Micrometer 风格
 * {@code buzhou.<mech>.<测量>}、tag 值有界枚举、<b>严禁 sessionId 进 tag</b>）：
 * 关键路径（事件分发 / 工具调用 / Turn 时长 / 压缩 / spill / guard 裁决 / 存储写失败）
 * 经 {@link BuzhouMetricsHolder} 全局默认实例记录；未装 micrometer 时为 {@link #noop()}
 * （零开销 no-op，库不强制可观测依赖）。
 */
public interface BuzhouMetrics {

    /** 计数 +1（tag 值有界枚举；name 形如 {@code buzhou.eventbus.dropped}）。 */
    default void counter(String name, String... tagKeyValue) {
        counter(name, 1, tagKeyValue);
    }

    /** 计数 +delta。 */
    void counter(String name, long delta, String... tagKeyValue);

    /** 时长记录（timer 语义；tag 值有界枚举，如 outcome=ok|degraded|failed）。 */
    void timer(String name, Duration duration, String... tagKeyValue);

    /** 单值水位（gauge 语义；引用失效自动注销由实现负责）。 */
    default void gauge(String name, java.util.function.Supplier<Number> value,
                       String... tagKeyValue) {
        // no-op 默认：gauge 是可选增强面
    }

    /** no-op 实现（未装 micrometer / 显式关闭时）。 */
    static BuzhouMetrics noop() {
        return NoopBuzhouMetrics.INSTANCE;
    }

    /** 聚合多路（如全局 + 模块内）。 */
    static BuzhouMetrics compose(BuzhouMetrics... metrics) {
        return new CompositeBuzhouMetrics(java.util.List.of(metrics));
    }
}

final class NoopBuzhouMetrics implements BuzhouMetrics {

    static final NoopBuzhouMetrics INSTANCE = new NoopBuzhouMetrics();

    private NoopBuzhouMetrics() {
    }

    @Override
    public void counter(String name, long delta, String... tagKeyValue) {
    }

    @Override
    public void timer(String name, Duration duration, String... tagKeyValue) {
    }
}

record CompositeBuzhouMetrics(java.util.List<BuzhouMetrics> delegates) implements BuzhouMetrics {

    CompositeBuzhouMetrics {
        delegates = java.util.List.copyOf(delegates);
    }

    @Override
    public void counter(String name, long delta, String... tagKeyValue) {
        delegates.forEach(m -> m.counter(name, delta, tagKeyValue));
    }

    @Override
    public void timer(String name, Duration duration, String... tagKeyValue) {
        delegates.forEach(m -> m.timer(name, duration, tagKeyValue));
    }

    @Override
    public void gauge(String name, java.util.function.Supplier<Number> value,
                      String... tagKeyValue) {
        delegates.forEach(m -> m.gauge(name, value, tagKeyValue));
    }
}
