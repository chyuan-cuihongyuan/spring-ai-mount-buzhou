package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.guard.fact.FactDefinition;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hook→state→Attachment 真端到端（ticket 13 checklist 1）：
 * 工具调用触发 FactCollectorHook 判定 → 事实写 SessionStateStore → 下一轮注入视图出现
 * {@code <system-reminder>} 事实块。贯穿 guard（采集）→ core（存储）→ memory（注入视图）三模块。
 */
class GuardFactLoopEndToEndTest {

    @Test
    void toolCallFactAppearsInNextTurnInjectionView() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();

        // 判定器从入参判定语义：带 tableId 的 update_table 才是改表
        FactDefinition riskFact = new FactDefinition() {
            @Override
            public String name() {
                return "risk";
            }

            @Override
            public Optional<Fact> judge(ToolCallContext ctx) {
                if (!"update_table".equals(ctx.toolName()) || ctx.arguments() == null
                        || !ctx.arguments().containsKey("tableId")) {
                    return Optional.empty();
                }
                return Optional.of(new Fact("table-" + ctx.arguments().get("tableId"),
                        "高风险表已修改", null, 0, 0));
            }

            @Override
            public String render(Fact fact) {
                return "- " + fact.value() + "（" + fact.key() + "）";
            }

            @Override
            public int ttl() {
                return 3;
            }
        };

        GuardModule guard = GuardModule.builder(stores).factDefinition(riskFact).build();
        RuntimeConfig config = RuntimeConfig.merge(
                guard.configure(),
                MemoryModule.configure(Map.of(), stores, model, model, guard.attachmentRenderer()));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config, new EchoTool("update_table"));

        // 轮 1：模型调 update_table（带 tableId）→ Hook 采集事实
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "update_table", "{\"tableId\":\"T1\"}")))
                .build());
        model.enqueue(new AssistantMessage("表已更新"));

        AgentSession session = runtime.spawn("app", "agent", "sess-fact-loop");
        session.chat("更新表 T1");

        // 轮 2：注入视图应含事实 system-reminder 块
        model.enqueue(new AssistantMessage("继续"));
        session.chat("下一步");
        session.close();

        // 模型在轮 2 实际看到的 prompt（注入视图）含事实块
        // （轮 1 两次模型调用：含 tool_calls 的首调 + 工具结果回注后的次调；轮 2 是第三次调用）
        Prompt turn2Prompt = model.seenPrompts.get(2);
        assertThat(turn2Prompt.getInstructions())
                .anyMatch(m -> m.getText() != null
                        && m.getText().contains("<system-reminder>")
                        && m.getText().contains("高风险表已修改"));
    }

    @Test
    void factAttachmentRendererTruncatesWithKeyPointer() {
        // spec 07：max-inject-chars 总量约束，超出按事实粒度截断并附 key 清单指针
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule guard = GuardModule.builder(stores)
                .factDefinition(new FactDefinition() {
                    @Override
                    public String name() {
                        return "p";
                    }

                    @Override
                    public Optional<Fact> judge(ToolCallContext ctx) {
                        return Optional.empty();
                    }

                    @Override
                    public String render(Fact fact) {
                        return "- " + fact.value();
                    }
                })
                .build();
        guard.factStore().save("s1", new Fact(Fact.keyFor("p", "short"), "短事实", "p", 1, 5));
        guard.factStore().save("s1", new Fact(Fact.keyFor("p", "long"), "x".repeat(200), "p", 1, 5));

        Optional<String> rendered = guard.attachmentRenderer().render("s1", 2, 50);
        assertThat(rendered).isPresent();
        assertThat(rendered.get()).contains("短事实");
        // 长事实被省略，指针 = 其 fact key
        assertThat(rendered.get()).contains("fact.p.long");
    }

    static class EchoTool implements ToolCallback {
        private final String name;

        EchoTool(String name) {
            this.name = name;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name(name).description(name)
                    .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
        }

        @Override
        public String call(String toolInput) {
            return "{\"ok\":true}";
        }
    }

    static class ScriptedChatModel implements ChatModel {
        final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();
        final List<Prompt> seenPrompts = new CopyOnWriteArrayList<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            ChatResponse next = script.poll();
            if (next == null) {
                next = new ChatResponse(List.of(new Generation(new AssistantMessage("default reply"))));
            }
            return next;
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
