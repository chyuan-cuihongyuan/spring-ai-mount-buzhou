package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 423 §Testing / T737–T738：错误偏向采样——rate=100 全保（对照基础
 * 采样低率做不到）、rate=0 零入集、空/短输入过滤、onTurnEnd 清场、占位
 * 截断、fail-soft；yml 双开/error-only 装配；enqueueThrow E2E 错误轮入集。
 */
class TurnErrorSamplerTest {

    private static TurnErrorSampler sampler(EvalDatasetStore store, int ratePercent) {
        return new TurnErrorSampler(store, new TurnErrorSampler.Policy("err-pool", ratePercent, 0), "s1");
    }

    @Test
    void shouldKeepAllErrorTurnsAtFullRate_noneAtZero() {
        EvalDatasetStore store = new EvalDatasetStore(new InMemorySessionStateStore());
        store.createDataset("err-pool", "错误池");

        TurnErrorSampler full = sampler(store, 100);
        for (int t = 1; t <= 20; t++) {
            full.onTurnStart(t, "失败的问题 " + t);
            full.onTurnError(t, new IllegalStateException("模型炸了 " + t));
        }
        // 偏向性：20 个错误轮全保（基础采样同分布 rate=5 期望约 1 条）
        assertThat(store.items("err-pool")).hasSize(20);

        TurnErrorSampler zero = sampler(store, 0);
        zero.onTurnStart(99, "不会被采的问题");
        zero.onTurnError(99, new IllegalStateException("x"));
        assertThat(store.items("err-pool")).hasSize(20);
    }

    @Test
    void shouldFilterBlankAndShortInput_andClearAfterTurnEnd() {
        EvalDatasetStore store = new EvalDatasetStore(new InMemorySessionStateStore());
        store.createDataset("err-pool", "错误池");
        TurnErrorSampler sut = new TurnErrorSampler(store,
                new TurnErrorSampler.Policy("err-pool", 100, 5), "s1");

        sut.onTurnStart(1, "  "); // 空白输入
        sut.onTurnError(1, new IllegalStateException("x"));
        sut.onTurnStart(2, "短"); // 低于 minInputChars=5
        sut.onTurnError(2, new IllegalStateException("x"));
        assertThat(store.items("err-pool")).isEmpty();

        sut.onTurnStart(3, "足够长的问题");
        sut.onTurnEnd(3, "正常收尾"); // 清场——此后迟到 error 不采
        sut.onTurnError(3, new IllegalStateException("迟到"));
        assertThat(store.items("err-pool")).isEmpty();

        sut.onTurnStart(4, "足够长的问题");
        sut.onTurnError(4, new IllegalStateException("真错误"));
        assertThat(store.items("err-pool")).hasSize(1);
    }

    @Test
    void shouldPlaceholdWithErrorClassAndTruncatedMessage() {
        EvalDatasetStore store = new EvalDatasetStore(new InMemorySessionStateStore());
        store.createDataset("err-pool", "错误池");
        TurnErrorSampler sut = sampler(store, 100);

        sut.onTurnStart(1, "触发超长错误消息的问题");
        sut.onTurnError(1, new RuntimeException("x".repeat(500)));
        var item = store.items("err-pool").get(0);
        assertThat(item.input()).isEqualTo("触发超长错误消息的问题");
        assertThat(item.expected()).startsWith("[TURN-ERROR] RuntimeException: ");
        assertThat(item.expected()).hasSizeLessThanOrEqualTo("[TURN-ERROR] RuntimeException: ".length() + 200);

        // fail-soft：未建集不炸（计数暴露）
        TurnErrorSampler noDataset = new TurnErrorSampler(store,
                new TurnErrorSampler.Policy("missing", 100, 0), "s2");
        noDataset.onTurnStart(1, "问题");
        noDataset.onTurnError(1, new IllegalStateException("无集"));
    }

    @Test
    void shouldAssembleDualAndErrorOnly() {
        // 双开：store + 成功 hook RC + 错误采样 RC 全在
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.eval.sampling.enabled=true",
                        "buzhou.eval.sampling.dataset=prod-samples",
                        "buzhou.eval.error-sampling.enabled=true",
                        "buzhou.eval.error-sampling.dataset=err-pool")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouEvalDatasetStore");
                    assertThat(context).hasBean("buzhouEvalSamplingRuntimeConfig");
                    assertThat(context).hasBean("buzhouErrorSamplingRuntimeConfig");
                });
        // error-only：store + 错误 RC 在、成功 hook RC 不在（渐进路径）
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.eval.error-sampling.enabled=true",
                        "buzhou.eval.error-sampling.dataset=err-pool")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouEvalDatasetStore");
                    assertThat(context).hasBean("buzhouErrorSamplingRuntimeConfig");
                    assertThat(context).doesNotHaveBean("buzhouEvalSamplingRuntimeConfig");
                });
    }

    @Test
    void shouldSampleRealErrorTurn_endToEnd() {
        EvalDatasetStore store = new EvalDatasetStore(new InMemorySessionStateStore());
        store.createDataset("err-pool", "错误池");
        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), java.util.Map.of(), List.of(),
                List.of(ctx -> ctx.addObserver(new TurnErrorSampler(store,
                        new TurnErrorSampler.Policy("err-pool", 100, 0), ctx.sessionId()))),
                null);
        // 错误轮只在流式路径回调 onTurnError（非流式 chat() 异常不回调——观察者
        // 契约「如 stream 错误」）；覆写 stream 返回 Flux.error 走真错误信号
        ScriptedChatModel model = new ScriptedChatModel() {
            @Override
            public reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> stream(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                seenPrompts.add(prompt);
                return reactor.core.publisher.Flux.error(new IllegalStateException("流式模型故障"));
            }
        };
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-err")) {
            assertThatThrownBy(() -> agent.stream("会失败的问题").blockLast())
                    .isInstanceOf(Exception.class);
        }
        assertThat(store.items("err-pool")).hasSize(1);
        assertThat(store.items("err-pool").get(0).expected())
                .startsWith("[TURN-ERROR] IllegalStateException");
    }
}
