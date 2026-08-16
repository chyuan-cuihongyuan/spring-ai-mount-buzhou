package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort#15 语义缓存演示（spec 55 / T246 / impl-195；stub 嵌入口径——生产替换为真实
 * EmbeddingModel bean）：FAQ 型负载同义问法二问零模型调用——宿主视角「开语义缓存后
 * 看到什么」的可运行样例。
 *
 * <p>生产等价配置：{@code buzhou.resilience.semantic-cache.enabled=true}（默认关）+
 * 注册 Spring AI EmbeddingModel bean（如 OpenAI starter 自动装配）。
 * 成本口径：命中省一次模型调用、花一或两次嵌入调用（查询 + 写入）——模型贵/嵌入便宜
 * 的 FAQ 型负载最划算（runbook §语义缓存）。
 */
class Effort15SemanticCacheDemoTest {

    /** 演示 stub：常见问法映射到相近向量（真实部署换成 provider 判别力）。 */
    private static final Map<String, float[]> VECTORS = Map.of(
            "退货政策是什么\n", new float[]{1f, 0f},
            "退货规则是啥\n", new float[]{0.97f, 0.243f});

    @Test
    void demoFaqParaphraseServedSemanticallyWithZeroModelCalls() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("支持 7 天无理由退货");
        BuzhouStores stores = Buzhou.inMemoryStores();
        ResilienceProperties props = new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                null, null, null, null, null, null,
                new ResilienceProperties.SemanticCache(Boolean.TRUE, 0.9, 64, Duration.ofHours(1)));
        // 宿主开启语义缓存：编程式等价 buzhou.resilience.semantic-cache.enabled=true + EmbeddingModel bean
        var runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(props, "faq-model", new ResilienceStats(),
                        null, null, null, demoEmbedder()));

        // 用户 A 原问法（真调模型一次）
        try (var sessionA = runtime.spawn("app", "agent", "user-a")) {
            assertThat(sessionA.chat("退货政策是什么")).isEqualTo("支持 7 天无理由退货");
        }
        // 用户 B 换个问法（语义命中：零模型调用、零 token 成本——问法不同但语义相近）
        try (var sessionB = runtime.spawn("app", "agent", "user-b")) {
            assertThat(sessionB.chat("退货规则是啥")).isEqualTo("支持 7 天无理由退货");
        }
        // 模型全程只调一次（精确缓存做不到——问法不同键必 miss，这正是语义缓存的价值面）
        assertThat(model.seenPrompts).hasSize(1);
    }

    private static EmbeddingModel demoEmbedder() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<String> texts = request.getInstructions();
                return new EmbeddingResponse(java.util.stream.IntStream.range(0, texts.size())
                        .mapToObj(i -> new Embedding(VECTORS.getOrDefault(texts.get(i),
                                new float[]{0f, 1f}), i))
                        .toList());
            }

            @Override
            public float[] embed(Document document) {
                return VECTORS.getOrDefault(document.getText(), new float[]{0f, 1f});
            }
        };
    }
}
