package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #10 新能力演示（spec 51 续 / T184 / impl-153）：TTFT 观测、rateTurn 反馈、
 * 金丝雀权重分流、shadow 对照——「宿主视角能拿到什么」的可运行样例（对齐 effort #9 演示口径）。
 */
class Effort10CapabilitiesDemoTest {

    /** 演示①：TTFT 观测——流式消费后 span 上可读首字时延与均摊吐字。 */
    @Test
    void demoTtftObservability() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(
                new DemoStreamingModel(),
                stores,
                io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityModule.configureSync(
                        stores,
                        io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig
                                .testDefaults(),
                        "demo-ttft"));
        AgentSession session = runtime.spawn("app", "demo", "demo-ttft");
        session.stream("流式问一句").blockLast();
        session.close();

        var modelCalls = stores.observabilityStore().spansOfSession("demo-ttft").stream()
                .filter(s -> io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind.MODEL_CALL
                        .equals(s.kind()) && "OK".equals(s.status()))
                .toList();
        assertThat(modelCalls).isNotEmpty();
        // 宿主视角：ttft.ms / tpot.ms 就在 span 属性里（dashboard 回放同源）
        assertThat(modelCalls.getFirst().attributes()).containsKeys("ttft.ms", "tpot.ms");
    }

    /** 演示②：rateTurn 反馈——一轮对话后点赞/点踩进事件与导出面。 */
    @Test
    void demoTurnFeedbackCapture() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("这是答案"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        AgentSession session = runtime.spawn("app", "demo", "demo-fb");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);

        String reply = session.chat("问个问题");
        session.rateTurn(1, "boolean", "true", "有帮助", null);

        assertThat(reply).isEqualTo("这是答案");
        assertThat(events).anyMatch(e -> "turn.feedback".equals(e.type())
                && "true".equals(e.payload().get("value")));
        // 导出面：反馈随 SessionExport 携带（core.feedback 段）
        ((io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime) runtime)
                .setExportExtensions(List.of(
                        new io.github.chyuan_cuihongyuan.buzhou.core.session.FeedbackExporter(
                                stores.sessionStateStore())));
        var export = runtime.exportSession("demo-fb");
        assertThat(export.extensions()).containsKey("core.feedback");
        session.close();
    }

    /** 演示③：金丝雀权重——同会话粘住、跨会话分流、事件可见首选落点。 */
    @Test
    void demoCanaryWeightedSplit() {
        int secondary = 0;
        for (int i = 0; i < 20; i++) {
            ScriptedChatModel primary = new ScriptedChatModel();
            primary.enqueue(new AssistantMessage("P"));
            primary.enqueue(new AssistantMessage("P"));
            ScriptedChatModel sec = new ScriptedChatModel();
            sec.enqueue(new AssistantMessage("S"));
            sec.enqueue(new AssistantMessage("S"));
            BuzhouStores stores = Buzhou.inMemoryStores();
            AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                    new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                            2.0, 0.0, null, Duration.ofSeconds(5), null, null,
                            new ResilienceProperties.Fallback(null, null, Boolean.TRUE,
                                    Map.of("secondary", 3)),
                            null, null),
                    "primary", new ResilienceStats(),
                    List.of(new NamedFallbackModel("secondary", sec))));
            AgentSession session = runtime.spawn("app", "demo", "demo-canary-" + i);
            List<SessionEvent> events = new CopyOnWriteArrayList<>();
            session.addEventListener(events::add);

            String a = session.chat("q1");
            String b = session.chat("q2");
            assertThat(b).isEqualTo(a); // 粘住
            assertThat(events).anyMatch(e -> FallbackChain.EVENT_CANARY_SELECTED.equals(e.type()));
            if ("S".equals(a)) {
                secondary++;
            }
            session.close();
        }
        assertThat(secondary).isBetween(5, 20);
    }

    /** 演示④：shadow 对照——用户照常拿到主模型回复，对照事件异步到达。 */
    @Test
    void demoShadowComparison() throws Exception {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("主模型答案"));
        ScriptedChatModel shadow = new ScriptedChatModel();
        shadow.enqueue(new AssistantMessage("shadow 答案"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                        2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null,
                        new ResilienceProperties.Shadow(Boolean.TRUE, null, null, null)),
                "primary", new ResilienceStats(), null,
                List.of(new NamedFallbackModel("shadow-model", shadow))));
        AgentSession session = runtime.spawn("app", "demo", "demo-shadow");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);

        assertThat(session.chat("问")).isEqualTo("主模型答案");
        long deadline = System.currentTimeMillis() + 5000;
        while (events.stream().noneMatch(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type()))
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertThat(events).anyMatch(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type())
                && e.payload().containsKey("deltaMs"));
        session.close();
    }

    /** 三块流式模型（首块空转延迟省略——演示不打 exact 时序）。 */
    static class DemoStreamingModel implements org.springframework.ai.chat.model.ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            return new org.springframework.ai.chat.model.ChatResponse(List.of(
                    new org.springframework.ai.chat.model.Generation(new AssistantMessage("c"))),
                    org.springframework.ai.chat.metadata.ChatResponseMetadata.builder()
                            .usage(new org.springframework.ai.chat.metadata.DefaultUsage(10, 3))
                            .build());
        }

        @Override
        public reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> stream(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            return reactor.core.publisher.Flux.just(
                    new org.springframework.ai.chat.model.ChatResponse(List.of(
                            new org.springframework.ai.chat.model.Generation(new AssistantMessage("a")))),
                    new org.springframework.ai.chat.model.ChatResponse(List.of(
                            new org.springframework.ai.chat.model.Generation(new AssistantMessage("b")))),
                    call(prompt));
        }
    }
}
