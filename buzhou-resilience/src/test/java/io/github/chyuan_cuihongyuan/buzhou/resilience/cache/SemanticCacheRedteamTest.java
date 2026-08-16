package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 55 §D/§E / T243/T244 / impl-193：语义缓存红队 + 端到端——
 * 否定对诚实命中（机制正确性 vs 嵌入质量分离钉住）/ 跨桶隔离 / enabled 无 bean
 * fail-fast / 带 toolCalls 不写 / 嵌入异常旁路降级 / call+stream 命中短路。
 * stub 嵌入模型向量手构（确定性）——真实嵌入判别力归 provider，不在此测。
 */
class SemanticCacheRedteamTest {

    /** 确定性 stub：文本 → 向量映射可注入（红队向量手工构造）。 */
    static final class StubEmbeddingModel implements EmbeddingModel {
        final Function<String, float[]> mapping;
        final Map<String, Integer> calls = new HashMap<>();

        StubEmbeddingModel(Function<String, float[]> mapping) {
            this.mapping = mapping;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> texts = request.getInstructions();
            float[][] vectors = texts.stream().map(t -> {
                calls.merge(t, 1, Integer::sum);
                float[] v = mapping.apply(t);
                if (v == null) {
                    throw new IllegalStateException("stub embed failure: " + t);
                }
                return v;
            }).toArray(float[][]::new);
            return new EmbeddingResponse(java.util.stream.IntStream.range(0, vectors.length)
                    .mapToObj(i -> new Embedding(vectors[i], i))
                    .toList());
        }

        @Override
        public float[] embed(Document document) {
            return mapping.apply(document.getText());
        }
    }

    private static ResilienceProperties semanticProps() {
        return new ResilienceProperties(true, 3, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, null, null, null, null, null, null, null,
                new ResilienceProperties.SemanticCache(Boolean.TRUE, 0.9, 16, Duration.ofHours(1)));
    }

    /** E2E：同义问法（stub 向量相近）第二问零模型调用命中。 */
    @Test
    void paraphrasedQuestionHitsSemanticallyWithZeroModelCalls() {
        // 一对相近但不相同的向量（cosine ≈ 0.97 ≥ 0.9）
        float[] q1 = {1f, 0f};
        float[] q2 = {0.97f, 0.243f};
        Map<String, float[]> vectors = Map.of(
                "退货政策是什么\n", q1,
                "退货规则是啥\n", q2);
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("标准答案");
        BuzhouStores stores = Buzhou.inMemoryStores();
        var runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(semanticProps(), "demo", new ResilienceStats(),
                        null, null, null, embedder));

        try (var a = runtime.spawn("app", "agent", "user-a")) {
            assertThat(a.chat("退货政策是什么")).isEqualTo("标准答案");
        }
        try (var b = runtime.spawn("app", "agent", "user-b")) {
            // 同义问法（非逐字相同——精确缓存必 miss）语义命中
            assertThat(b.chat("退货规则是啥")).isEqualTo("标准答案");
        }
        assertThat(model.seenPrompts).hasSize(1); // 第二问零模型调用
    }

    /**
     * 否定对（诚实机制钉住）：stub 把「X」与「不是 X」编为相近向量 → 框架按阈值命中——
     * 语义判别力归嵌入模型；框架保证阈值/分桶/边界正确（默认关闭 + runbook 残余风险声明）。
     */
    @Test
    void negationPairHitsWhenEmbeddingsAreCloseHonestMechanism() {
        float[] positive = {1f, 0f};
        float[] negated = {0.99f, 0.1f}; // cosine ≈ 0.995 —— 「差的嵌入」场景
        Map<String, float[]> vectors = new HashMap<>();
        vectors.put("支持退货\n", positive);
        vectors.put("不支持退货\n", negated);
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("支持");
        BuzhouStores stores = Buzhou.inMemoryStores();
        var runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(semanticProps(), "demo", new ResilienceStats(),
                        null, null, null, embedder));

        try (var a = runtime.spawn("app", "agent", "user-a")) {
            a.chat("支持退货");
        }
        try (var b = runtime.spawn("app", "agent", "user-b")) {
            // 诚实断言：相近嵌入下框架命中（错误答案重放）——这正是默认关闭的理由
            assertThat(b.chat("不支持退货")).isEqualTo("支持");
        }
        assertThat(model.seenPrompts).hasSize(1);
    }

    /** 不同问法向量正交（cosine 0 < 阈值）→ 不命中，正常打模型。 */
    @Test
    void dissimilarQuestionMissesAndCallsModel() {
        Map<String, float[]> vectors = Map.of(
                "天气如何\n", new float[]{1f, 0f},
                "退货政策是什么\n", new float[]{0f, 1f});
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("晴天");
        model.enqueueText("退货 7 天");
        var runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                ResilienceModule.configure(semanticProps(), "demo", new ResilienceStats(),
                        null, null, null, embedder));
        try (var a = runtime.spawn("app", "agent", "u1")) {
            assertThat(a.chat("天气如何")).isEqualTo("晴天");
        }
        try (var b = runtime.spawn("app", "agent", "u2")) {
            assertThat(b.chat("退货政策是什么")).isEqualTo("退货 7 天");
        }
        assertThat(model.seenPrompts).hasSize(2);
    }

    /** enabled=true 而无 EmbeddingModel → 启动 fail-fast 带修法（不静默不生效）。 */
    @Test
    void enabledWithoutEmbeddingModelFailsFastWithFix() {
        assertThatThrownBy(() -> ResilienceModule.configure(semanticProps(), "m",
                new ResilienceStats(), null, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("EmbeddingModel")
                .hasMessageContaining("semantic-cache.enabled");
    }

    /** 嵌入异常旁路降级：查询嵌入失败 → 主调用照常成功（bypass 计数可感，不阻断）。 */
    @Test
    void embedFailureBypassesWithoutBlockingMainCall() {
        Map<String, float[]> vectors = new HashMap<>();
        vectors.put("问题一\n", null); // stub 对该文本抛异常
        vectors.put("问题一", null);
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("主路径答案");
        var runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                ResilienceModule.configure(semanticProps(), "demo", new ResilienceStats(),
                        null, null, null, embedder));
        try (var a = runtime.spawn("app", "agent", "u1")) {
            assertThat(a.chat("问题一")).isEqualTo("主路径答案"); // 嵌入挂了主调用不受阻
        }
        assertThat(model.seenPrompts).hasSize(1);
    }

    /** stream 命中重放（T244）：同义问法二订阅零模型调用、内容等价。 */
    @Test
    void streamReplaysSemanticallyCachedResponse() {
        float[] q1 = {1f, 0f};
        float[] q2 = {0.97f, 0.243f};
        Map<String, float[]> vectors = Map.of(
                "流式问题一\n", q1,
                "流式问题一改个说法\n", q2);
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("流式答案");
        var runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                ResilienceModule.configure(semanticProps(), "demo", new ResilienceStats(),
                        null, null, null, embedder));
        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        try (var a = runtime.spawn("app", "agent", "u1")) {
            a.stream("流式问题一").doOnNext(r -> first.append(r.getResult().getOutput().getText()))
                    .blockLast();
        }
        try (var b = runtime.spawn("app", "agent", "u2")) {
            b.stream("流式问题一改个说法")
                    .doOnNext(r -> second.append(r.getResult().getOutput().getText()))
                    .blockLast();
        }
        assertThat(second.toString()).isEqualTo(first.toString()).isEqualTo("流式答案");
        assertThat(model.seenPrompts).hasSize(1); // 二订阅零模型调用
    }
}
