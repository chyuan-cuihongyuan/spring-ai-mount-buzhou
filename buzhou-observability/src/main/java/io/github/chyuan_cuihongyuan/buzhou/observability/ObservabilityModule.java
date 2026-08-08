package io.github.chyuan_cuihongyuan.buzhou.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.observability.advisor.ObservabilityAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.observability.advisor.ObservabilitySessionState;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.AsyncObservabilityPipeline;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.BaseSpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.SynchronousObservabilityPipeline;
import io.github.chyuan_cuihongyuan.buzhou.observability.thinking.ThinkingChainExtractor;
import io.github.chyuan_cuihongyuan.buzhou.observability.tool.ObservableToolCallback;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.Optional;

/**
 * 可观测采集模块入口（spec 03）。经 {@link #configure} 返回 {@link RuntimeConfig}，由
 * {@code RuntimeConfig.merge} 与其他机制模块组合，挂进 {@code HarnessAssembler} 的装配链。
 *
 * <p>{@link SpanRecorder} 选择：生产用 {@link AsyncObservabilityPipeline}（异步批量，背压不丢）；
 * 测试用 {@link #configureSync}（同步直写，即时断言）。
 */
public final class ObservabilityModule {

    private ObservabilityModule() {
    }

    /** 生产配置：异步管线 + 可选 Micrometer 双写。modelName 由调用方从 yml 解析。 */
    public static RuntimeConfig configure(BuzhouStores stores, ObservabilityConfig config,
                                          MeterRegistry meterRegistry, String modelName) {
        return configure(stores, config, meterRegistry, modelName, false, List.of());
    }

    /**
     * 生产配置（带旁路 sink）：异步管线 + 可选 Micrometer 双写 + 旁路 {@link PipelineSink}
     * （OTel 导出桥等）。{@code buzhou-observe-otel} 模块产出 sink 后经此接入。
     */
    public static RuntimeConfig configure(BuzhouStores stores, ObservabilityConfig config,
                                          MeterRegistry meterRegistry, String modelName,
                                          List<PipelineSink> sinks) {
        return configure(stores, config, meterRegistry, modelName, false, sinks);
    }

    /** 测试配置：同步管线，便于即时断言。 */
    public static RuntimeConfig configureSync(BuzhouStores stores, ObservabilityConfig config,
                                              String modelName) {
        return configure(stores, config, null, modelName, true, List.of());
    }

    /** 测试配置（带旁路 sink）：同步管线 + 旁路 {@link PipelineSink}，便于即时断言。 */
    public static RuntimeConfig configureSync(BuzhouStores stores, ObservabilityConfig config,
                                              String modelName, List<PipelineSink> sinks) {
        return configure(stores, config, null, modelName, true, sinks);
    }

    private static RuntimeConfig configure(BuzhouStores stores, ObservabilityConfig config,
                                           MeterRegistry meterRegistry, String modelName,
                                           boolean synchronous, List<PipelineSink> sinks) {
        if (!config.enabled()) {
            return RuntimeConfig.defaults();
        }
        MicrometerDualWriter meters = config.micrometerEnabled() && meterRegistry != null
                ? new MicrometerDualWriter(meterRegistry) : MicrometerDualWriter.NOOP;
        BaseSpanRecorder recorder = synchronous
                ? new SynchronousObservabilityPipeline(stores.observabilityStore(), config, meters, sinks)
                : new AsyncObservabilityPipeline(stores.observabilityStore(), config, meters, sinks);
        TokenEstimator tokenEstimator = new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator();
        ThinkingChainExtractor thinkingExtractor = new ThinkingChainExtractor(
                config.thinkingExtraKeys(), config.thinkingMaxChars());
        return new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(), null, List.of(),
                java.util.Map.of(), List.of(),
                List.of(new ObservabilityAssemblyCustomizer(
                        recorder, config, thinkingExtractor, tokenEstimator, modelName)));
    }

    /** 装配定制器：注入 advisor + 工具包装 + 会话 observer（开/关 SESSION span、flush）。 */
    static class ObservabilityAssemblyCustomizer implements SessionAssemblyCustomizer {

        private final BaseSpanRecorder recorder;
        private final ObservabilityConfig config;
        private final ThinkingChainExtractor thinkingExtractor;
        private final TokenEstimator tokenEstimator;
        private final String modelName;

        ObservabilityAssemblyCustomizer(BaseSpanRecorder recorder, ObservabilityConfig config,
                                        ThinkingChainExtractor thinkingExtractor,
                                        TokenEstimator tokenEstimator, String modelName) {
            this.recorder = recorder;
            this.config = config;
            this.thinkingExtractor = thinkingExtractor;
            this.tokenEstimator = tokenEstimator;
            this.modelName = modelName;
        }

        @Override
        public void customize(SessionAssemblyContext ctx) {
            SpanContextCarrier carrier = ctx.spanContextCarrier();
            ObservabilitySessionState state = new ObservabilitySessionState(
                    recorder, carrier, ctx.sessionId(), ctx.agentName(), ctx.appId(), modelName);
            ObservabilityAdvisor advisor = new ObservabilityAdvisor(
                    recorder, config, thinkingExtractor, tokenEstimator, state, modelName);
            ctx.addAdvisor(advisor);
            ctx.wrapToolCallbacks(tool -> new ObservableToolCallback(tool, recorder, state, carrier));
            ctx.addObserver(state);
        }
    }
}
