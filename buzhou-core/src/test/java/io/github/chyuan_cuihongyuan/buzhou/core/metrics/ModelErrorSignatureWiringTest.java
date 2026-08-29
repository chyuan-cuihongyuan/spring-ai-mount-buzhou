package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 104 §B / T384：模型失败签名接线红队——异常族/超时族各入 ErrorSignatures
 * model 前缀；正常调用零记录；既有抛出语义不变（异常类型照旧）。spec 83 fog 收口。
 */
class ModelErrorSignatureWiringTest {

    @AfterEach
    void cleanup() {
        ErrorSignatures.install(ErrorSignatures.create()); // 隔离全局并清空
    }

    @Test
    void modelExceptionRecordsModelSignature() {
        ErrorSignatures registry = ErrorSignatures.create();
        ErrorSignatures.install(registry);
        BuzhouStores stores = Buzhou.inMemoryStores();
        var runtime = Buzhou.runtime(new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                throw new IllegalStateException("upstream 503 after 1200ms");
            }
        }, stores, RuntimeConfig.defaults());

        try (var session = runtime.spawn("app", "agent", "model-err")) {
            assertThatThrownBy(() -> session.chat("hi"))
                    .isInstanceOf(IllegalStateException.class); // 既有类型语义不变
        }

        assertThat(registry.top(3)).extracting(Map.Entry::getKey)
                .anySatisfy(k -> assertThat(k).startsWith("model:IllegalStateException:")
                        .contains("upstream # after #ms"));
    }

    @Test
    void successfulChatLeavesSignaturesEmpty() {
        ErrorSignatures registry = ErrorSignatures.create();
        ErrorSignatures.install(registry);
        BuzhouStores stores = Buzhou.inMemoryStores();
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok");
        var runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        try (var session = runtime.spawn("app", "agent", "model-good")) {
            assertThat(session.chat("hi")).isEqualTo("ok");
        }

        assertThat(registry.distinct()).isZero(); // 成功路径零签名
    }

    @Test
    void timeoutFamilyAlsoRecordedViaSignaturesApi() {
        // 超时族经 record("model", timeout) 面入档（BuzhouException TIMEOUT 面直接验证
        // ——端到端超时需真实慢模型，此处验签名面通路）
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("model", new BuzhouException(
                io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.TIMEOUT,
                "模型调用超时：Turn 预算 5000ms（含 2s 收尾宽限）已耗尽"));
        assertThat(registry.top(1)).extracting(Map.Entry::getKey)
                .anySatisfy(k -> assertThat(k).startsWith("model:BuzhouException:")
                        .contains("模型调用超时"));
    }
}
