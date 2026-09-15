package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 影子镜像旁路端到端补测（K 会话 R31 / spec 1229+ / T1875——R29 全仓扫描最大单方法缺口
 * shadowMirrorIfSampled 21 missed）：主路成功后采样对照首个备模型（Istio mirror 思想）——
 * 镜像调用发生、镜像失败全吞不影响主路、守卫分支（候选空/首候选即主模型）不镜像。
 * 先例：FallbackChainEndToEndTest（多模型 runtime harness）。
 */
class ShadowMirrorEndToEndTest {

    private static ResilienceProperties.Fallback fallback(List<String> models, int shadowProbePercent) {
        return new ResilienceProperties.Fallback(models, null, null, null, null, shadowProbePercent);
    }

    private static ResilienceProperties props(ResilienceProperties.Fallback fallback) {
        ResilienceProperties.Circuit circuit = new ResilienceProperties.Circuit(
                null, 10, 2, 0.5, Duration.ofSeconds(60), null);
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, circuit, fallback, null, null, null, null, null, null);
    }

    private static AgentSession newRuntime(ScriptedChatModel primary, ScriptedChatModel secondary,
            ResilienceProperties.Fallback fallback) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props(fallback), "primary", new ResilienceStats(),
                List.of(new NamedFallbackModel("secondary", secondary))));
        return runtime.spawn("app", "agent", "sess-" + System.nanoTime());
    }

    @Test
    void primarySuccessShadowMirrorsToFirstFallbackModel() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("primary-reply"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("shadow-reply"));

        AgentSession session = newRuntime(primary, secondary,
                fallback(List.of("secondary"), 100));

        assertThat(session.chat("hi")).isEqualTo("primary-reply");
        // 主路成功后采样对照：备模型被旁路调用一次（用户无感）
        assertThat(secondary.seenPrompts).hasSize(1);
        session.close();
    }

    @Test
    void shadowFailureIsSwallowedAndPrimaryReplyUnaffected() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("primary-reply"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueueThrow(new UncheckedIOException(new java.io.IOException("shadow down")));
        secondary.enqueueThrow(new UncheckedIOException(new java.io.IOException("shadow down 2")));

        AgentSession session = newRuntime(primary, secondary,
                fallback(List.of("secondary"), 100));

        assertThat(session.chat("hi")).isEqualTo("primary-reply");
        session.close();
    }

    @Test
    void emptyFallbackChainMeansNoMirror() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("primary-reply"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("never"));

        // 模块级候选列表空：候选空守卫直返（probe 非空但候选空）——不镜像
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props(fallback(List.of("secondary"), 100)), "primary", new ResilienceStats(),
                List.of()));
        AgentSession session = runtime.spawn("app", "agent", "sess-" + System.nanoTime());

        assertThat(session.chat("hi")).isEqualTo("primary-reply");
        assertThat(secondary.seenPrompts).isEmpty();
        session.close();
    }

    @Test
    void zeroProbePercentMeansNoMirror() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("primary-reply"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("never"));

        // shadowProbePercent=0 → 探针不构建（装配守卫），零旁路调用
        AgentSession session = newRuntime(primary, secondary, fallback(List.of("secondary"), 0));

        assertThat(session.chat("hi")).isEqualTo("primary-reply");
        assertThat(secondary.seenPrompts).isEmpty();
        session.close();
    }
}
