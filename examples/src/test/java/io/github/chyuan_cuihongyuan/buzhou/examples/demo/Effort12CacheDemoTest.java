package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #12 缓存演示（spec 53 / T210 / impl-175）：同问二调零模型开销 + 计数可读——
 * 宿主视角「开缓存后看到什么」的可运行样例。
 */
class Effort12CacheDemoTest {

    @Test
    void demoRepeatedQuestionServedFromCache() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("标准答案");
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 宿主开启缓存：buzhou.resilience.response-cache.enabled=true（此处编程式等价）
        ResilienceProperties props = new ResilienceProperties(null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                new ResilienceProperties.ResponseCache(Boolean.TRUE, 64, Duration.ofHours(1)));
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(props, "demo-model", new ResilienceStats(), null, null));

        // 用户 A 问（真调模型，脚本消耗 1 次）
        try (var sessionA = runtime.spawn("app", "agent", "user-a")) {
            assertThat(sessionA.chat("退货政策是什么?")).isEqualTo("标准答案");
        }
        // 用户 B 同问（命中缓存：零模型调用、零 token 成本、毫秒级返回）
        try (var sessionB = runtime.spawn("app", "agent", "user-b")) {
            assertThat(sessionB.chat("退货政策是什么?")).isEqualTo("标准答案");
        }
        // 模型全程只调了一次（成本护栏可感：seenPrompts 即调用计数）
        assertThat(model.seenPrompts).hasSize(1);

        // 用户 C 换问法（不同 messages = miss，真调模型）
        model.enqueueText("另一个答案");
        try (var sessionC = runtime.spawn("app", "agent", "user-c")) {
            assertThat(sessionC.chat("退货政策是啥?")).isEqualTo("另一个答案");
        }
        assertThat(model.seenPrompts).hasSize(2);
    }
}
