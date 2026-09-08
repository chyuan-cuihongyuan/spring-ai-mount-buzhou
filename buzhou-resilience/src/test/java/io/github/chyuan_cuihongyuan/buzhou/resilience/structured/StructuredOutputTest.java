package io.github.chyuan_cuihongyuan.buzhou.resilience.structured;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 402 §Testing / T695–T696：结构化输出执法——合法零修复；坏输出带
 * 错误反馈重调修复成功（模型两次调用+反馈含契约）；耗尽抛违规异常（0=纯
 * 执法）；围栏剥离；yml 装配 + enabled 无 schema fail-fast。
 */
class StructuredOutputTest {

    private static OutputSchema schema() {
        return new OutputSchema(List.of("name", "score"), Map.of("name", "string", "score", "integer"));
    }

    /** 内联链：第 N 次调用返回脚本第 N 条回复（nextCall 与终端直达同游标）。 */
    private static final class StubChain implements CallAdvisorChain {
        final List<String> replies;
        final List<ChatClientRequest> seen = new CopyOnWriteArrayList<>();
        int cursor;

        StubChain(List<String> replies) {
            this.replies = replies;
        }

        private ChatClientResponse consume(ChatClientRequest request) {
            seen.add(request);
            String reply = replies.get(Math.min(cursor, replies.size() - 1));
            cursor++;
            return new ChatClientResponse(
                    new org.springframework.ai.chat.model.ChatResponse(
                            List.of(new org.springframework.ai.chat.model.Generation(
                                    new AssistantMessage(reply)))),
                    request.context());
        }

        @Override
        public ChatClientResponse nextCall(ChatClientRequest request) {
            return consume(request);
        }

        @Override
        public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
            // 链尾 = 模型终端（advisor 修复路径直达此处）
            return List.of(new org.springframework.ai.chat.client.advisor.api.CallAdvisor() {
                @Override
                public String getName() {
                    return "stub-terminal";
                }

                @Override
                public int getOrder() {
                    return Integer.MAX_VALUE;
                }

                @Override
                public ChatClientResponse adviseCall(ChatClientRequest req, CallAdvisorChain chain) {
                    return consume(req);
                }
            });
        }

        @Override
        public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor advisor) {
            throw new UnsupportedOperationException();
        }
    }

    private static ChatClientRequest request(String userText) {
        return new ChatClientRequest(new Prompt(List.of(new UserMessage(userText))), Map.of());
    }

    @Test
    void shouldPassThroughWithoutRepair_whenValidJsonFirstTry() {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        StubChain chain = new StubChain(List.of("{\"name\":\"ok\",\"score\":3}"));
        ChatClientResponse out = new StructuredOutputAdvisor(schema(), 1, events::add)
                .adviseCall(request("问"), chain);
        assertThat(out.chatResponse().getResult().getOutput().getText())
                .isEqualTo("{\"name\":\"ok\",\"score\":3}");
        assertThat(chain.seen).hasSize(1); // 零修复
        assertThat(events).isEmpty();
    }

    @Test
    void shouldRepairWithStructuredFeedback_whenInvalidThenValid() {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        StubChain chain = new StubChain(List.of(
                "当然可以！这是我的回答：",       // 非法（散文）
                "{\"name\":\"修好\",\"score\":8}")); // 合法
        ChatClientResponse out = new StructuredOutputAdvisor(schema(), 1, events::add)
                .adviseCall(request("问"), chain);

        assertThat(out.chatResponse().getResult().getOutput().getText()).contains("修好");
        assertThat(chain.seen).hasSize(2); // 修复重调一次
        // 反馈消息在第二次请求里：错误清单 + 契约 + 上轮截断
        String second = chain.seen.get(1).prompt().getInstructions().toString();
        assertThat(second).contains("不是合法 JSON").contains("name").contains("当然可以");
        assertThat(events).extracting(SessionEvent::type)
                .containsExactly(StructuredOutputAdvisor.EVENT_REPAIR_ATTEMPTED,
                        StructuredOutputAdvisor.EVENT_REPAIRED);
    }

    @Test
    void shouldThrowViolation_whenExhausted() {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        StubChain chain = new StubChain(List.of("始终不合法"));
        // maxRepairAttempts=0：纯执法——首答违规即抛
        assertThatThrownBy(() -> new StructuredOutputAdvisor(schema(), 0, events::add)
                .adviseCall(request("问"), chain))
                .isInstanceOf(StructuredOutputViolationException.class)
                .hasMessageContaining("不是合法 JSON");
        assertThat(events).extracting(SessionEvent::type)
                .containsExactly(StructuredOutputAdvisor.EVENT_VIOLATION);
        assertThat(chain.seen).hasSize(1);
    }

    @Test
    void shouldStripCodeFence_whenModelWrapsJson() {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        StubChain chain = new StubChain(List.of("```json\n{\"name\":\"围栏\",\"score\":1}\n```"));
        ChatClientResponse out = new StructuredOutputAdvisor(schema(), 1, events::add)
                .adviseCall(request("问"), chain);
        assertThat(chain.seen).hasSize(1); // 围栏剥离后首答即合法
        assertThat(events).isEmpty();
    }

    @Test
    void shouldAssembleFromYml_andFailFastWithoutSchema() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.resilience.structured-output.enabled=true",
                        "buzhou.resilience.structured-output.schema.required[0]=name",
                        "buzhou.resilience.structured-output.schema.required[1]=score",
                        "buzhou.resilience.structured-output.schema.properties.score=integer")
                .run(context -> assertThat(context).hasNotFailed());

        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .withPropertyValues("buzhou.resilience.structured-output.enabled=true")
                .run(context -> assertThat(context).hasFailed());

        // 未 enabled：零装配（既有行为零变化）
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(RuntimeConfig.class))
                            .doesNotContainKey("structuredOutputRuntimeConfig");
                });
    }

    @Test
    void shouldEnforceEndToEnd_throughRuntime() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("我无法输出 JSON");            // 首答坏
        model.enqueueText("{\"name\":\"端到端\",\"score\":5}"); // 修复
        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.addAdvisor(new StructuredOutputAdvisor(schema(), 1, ctx::emitEvent))),
                null);
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-e2e")) {
            assertThat(agent.chat("生成")).contains("端到端");
        }
        assertThat(model.seenPrompts).hasSize(2); // 修复重调过模型
    }
}
