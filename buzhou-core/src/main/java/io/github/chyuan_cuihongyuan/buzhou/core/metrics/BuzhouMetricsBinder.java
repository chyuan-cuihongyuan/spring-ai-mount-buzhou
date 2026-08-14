package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;

/**
 * 标准指标集预注册（impl-41 / spec 13 §T66）：装 micrometer 时经
 * {@code @ConditionalOnClass(MeterRegistry.class)} 装配本 binder，指标在
 * {@code /metrics} 即刻可见（零值起步），而非首次记录才出现——
 * 运维面板从启动起就有完整序列。
 *
 * <p>标准集（tag 值有界枚举）：
 * {@code buzhou.turn.duration}(timer, outcome) / {@code buzhou.tool.calls}(counter, outcome) /
 * {@code buzhou.eventbus.dropped} / {@code buzhou.compaction}(outcome) /
 * {@code buzhou.spill.requests}(outcome) / {@code buzhou.guard.checks}(outcome) /
 * {@code buzhou.store.write.failures}(policy)。
 */
public final class BuzhouMetricsBinder implements MeterBinder {

    private static final List<String> OUTCOMES_TURN = List.of("ok", "degraded", "failed");
    private static final List<String> OUTCOMES_CALLS = List.of("ok", "failed");
    private static final List<String> OUTCOMES_COMPACTION = List.of("ok", "failed");
    private static final List<String> OUTCOMES_SPILL = List.of("spilled", "degraded", "failed");
    private static final List<String> OUTCOMES_GUARD = List.of("allowed", "blocked", "escalated");
    private static final List<String> STORE_WRITE_POLICIES = List.of("degrade", "fail");

    @Override
    public void bindTo(MeterRegistry registry) {
        OUTCOMES_TURN.forEach(outcome ->
                io.micrometer.core.instrument.Timer.builder("buzhou.turn.duration")
                        .tag("outcome", outcome).register(registry));
        OUTCOMES_CALLS.forEach(outcome ->
                registry.counter("buzhou.tool.calls", "outcome", outcome));
        registry.counter("buzhou.eventbus.dropped");
        OUTCOMES_COMPACTION.forEach(outcome ->
                registry.counter("buzhou.compaction", "outcome", outcome));
        OUTCOMES_SPILL.forEach(outcome ->
                registry.counter("buzhou.spill.requests", "outcome", outcome));
        OUTCOMES_GUARD.forEach(outcome ->
                registry.counter("buzhou.guard.checks", "outcome", outcome));
        STORE_WRITE_POLICIES.forEach(policy ->
                registry.counter("buzhou.store.write.failures", "policy", policy));
        // 韧性族（impl-44 / spec 14 §A）：重试 / 耗尽 / 限流拒绝 / 模型超时 / 内容拒绝
        // （category tag 五类有界；model tag 由记录侧截断，此处预注册无 tag 基型）
        registry.counter("buzhou.resilience.retries");
        registry.counter("buzhou.resilience.retry-exhausted");
        registry.counter("buzhou.resilience.rate-limit-rejected");
        registry.counter("buzhou.resilience.model-timeouts");
        registry.counter("buzhou.resilience.content-refusals");
        // 失控检测与容量闸（impl-45 / spec 14 §A；reason tag 有界枚举）
        registry.counter("buzhou.runaway.hard-stops");
        registry.counter("buzhou.backpressure.spawn-rejected");
    }
}
