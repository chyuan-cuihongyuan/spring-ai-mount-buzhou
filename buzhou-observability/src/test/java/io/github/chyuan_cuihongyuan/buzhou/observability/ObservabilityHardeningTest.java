package io.github.chyuan_cuihongyuan.buzhou.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-46 / spec 14 §B 加固面测试：流取消 span 终态、配置 fail-fast、
 * 指标家族单口径（经 core holder，无平行计数器）、同步管线失败隔离。
 */
class ObservabilityHardeningTest {

    @AfterEach
    void resetMetricsHolder() {
        BuzhouMetricsHolder.reset();
    }

    /** 流被订阅者取消：MODEL_CALL span 落 CANCELLED 终态，store 无 RUNNING 孤儿。 */
    @Test
    void streamCancelFinalizesSpanAsCancelled() {
        ScriptedChatModel model = new ScriptedChatModel() {
            @Override
            public Flux<org.springframework.ai.chat.model.ChatResponse> stream(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                // 慢流：首帧后永不完成——订阅者取消时流仍在途（doOnCancel 必触发）
                return Flux.concat(
                        Flux.just(call(prompt)).delayElements(Duration.ofMillis(50)),
                        Flux.never());
            }
        };
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ObservabilityModule.configureSync(stores, ObservabilityConfig.testDefaults(), "m"));
        AgentSession session = runtime.spawn("app", "agent", "sess-cancel");

        var subscription = session.stream("hi").subscribe();
        // 等流建立后取消（doOnCancel 触发）
        sleep(150);
        subscription.dispose();
        session.close();

        List<SpanRecord> spans = stores.observabilityStore().spansOfSession("sess-cancel");
        // store 语义 = RUNNING 开启行 + 终态行（同 spanId 两行）——修复前取消路径无终态行（RUNNING 孤儿）
        assertThat(spans).anyMatch(s -> "MODEL_CALL".equals(s.kind()) && "CANCELLED".equals(s.status())
                && s.endedAt() != null);
    }

    /** 配置 fail-fast：非法 batch-size / 负 flush-interval 启动即失败（带修法）。 */
    @Test
    void invalidConfigFailsFast() {
        assertThatThrownBy(() -> new ObservabilityConfig(true, 0, Duration.ofSeconds(1),
                Duration.ofSeconds(5), 10000, true, null, 32768, true, true, true))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("batch-size");
        assertThatThrownBy(() -> new ObservabilityConfig(true, 200, Duration.ZERO,
                Duration.ofSeconds(5), 10000, true, null, 32768, true, true, true))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("flush-interval");
        assertThatThrownBy(() -> new ObservabilityConfig(true, 200, Duration.ofSeconds(1),
                Duration.ofSeconds(5), 0, true, null, 32768, true, true, true))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("queue-capacity");
    }

    /** 指标单口径：DualWriter 经 core holder 记录，落库失败计入 store.write.failures(degrade) 而非平行计数器。 */
    @Test
    void metricsRecordedThroughCoreHolderSingleFamily() {
        List<String> recorded = new CopyOnWriteArrayList<>();
        BuzhouMetricsHolder.install(new BuzhouMetrics() {
            @Override
            public void counter(String name, long delta, String... tagKeyValue) {
                recorded.add(name + "|" + String.join("=", tagKeyValue));
            }

            @Override
            public void timer(String name, Duration duration, String... tagKeyValue) {
                recorded.add(name);
            }
        });
        MicrometerDualWriter writer = new MicrometerDualWriter();

        writer.recordPersistError();
        writer.recordQueueWait(5);
        writer.recordTokens("gpt-x", "prompt", 42);

        assertThat(recorded).contains("buzhou.store.write.failures|policy=degrade");
        assertThat(recorded).contains("buzhou.observability.queue.wait");
        assertThat(recorded).anyMatch(r -> r.startsWith("buzhou.tokens|kind=prompt"));
        // 收敛承诺：不再存在平行计数器 buzhou.observability.persist.errors
        assertThat(recorded).noneMatch(r -> r.startsWith("buzhou.observability.persist.errors"));

        // NOOP 哨兵：micrometer-enabled=false 时零记录
        recorded.clear();
        MicrometerDualWriter.NOOP.recordPersistError();
        MicrometerDualWriter.NOOP.recordTokens("m", "prompt", 1);
        assertThat(recorded).isEmpty();
    }

    /** 同步管线失败隔离：store 抛异常不打断主链路（计数 + 吞）。 */
    @Test
    void syncPipelineStoreFailureIsolated() {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore broken =
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore() {
                    @Override
                    public void saveSpans(List<SpanRecord> spans) {
                        throw new IllegalStateException("disk full");
                    }

                    @Override
                    public void saveEvents(List<io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord> events) {
                        throw new IllegalStateException("disk full");
                    }

                    @Override
                    public List<SpanRecord> spansOfSession(String sessionId) {
                        return List.of();
                    }

                    @Override
                    public List<io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord> eventsOfSession(
                            String sessionId) {
                        return List.of();
                    }

                    @Override
                    public void saveInjectionSnapshot(
                            io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot snapshot) {
                        throw new IllegalStateException("disk full");
                    }

                    @Override
                    public java.util.Optional<io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot> injectionSnapshot(
                            String sessionId, int turnSeq) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary> listSessionSummaries(
                            String cursor, int size) {
                        return List.of();
                    }

                    @Override
                    public List<io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord> eventsOfSpan(String spanId) {
                        return List.of();
                    }
                };
        io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.SynchronousObservabilityPipeline pipeline =
                new io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.SynchronousObservabilityPipeline(
                        broken, ObservabilityConfig.testDefaults(), new MicrometerDualWriter());
        // 不抛即通过（失败被吞 + 计数；此前同步路径异常直接打断业务调用）
        pipeline.flush();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
