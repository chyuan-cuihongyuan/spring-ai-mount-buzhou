package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 337 / impl-360：行李传播回归——工具读到 buzhou.baggage 快照 /
 * 空行李零注入 / 构造后 put 下次调用可见（活面快照语义）。
 * DeadlinePropagationTest 同手法。
 */
class BaggagePropagationTest {

    /** 记录所见行李的探针工具（ABSENT 哨兵——队列不收 null）。 */
    private ToolCallback probingTool(String name, ConcurrentLinkedQueue<Object> seen) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput, org.springframework.ai.chat.model.ToolContext ctx) {
                Object baggage = ctx.getContext().get(ToolBaggage.KEY);
                seen.add(baggage == null ? ABSENT : baggage);
                return name + ":ok";
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("本测试走 context 路径");
            }
        };
    }

    private static final String ABSENT = "__absent__";

    private ToolExecutionResult dispatch(HarnessToolCallingManager manager, ToolCallback tool) {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(List.of(
                        new AssistantMessage.ToolCall("probe", "function", "probe", "{}")))
                .build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        return manager.executeToolCalls(new Prompt(List.of(),
                ToolCallingChatOptions.builder().toolCallbacks(List.of(tool)).build()), response);
    }

    private HarnessToolCallingManager manager() {
        return new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(),
                8, Duration.ofSeconds(5), Map.of());
    }

    @Test
    void toolSeesBaggageSnapshot() {
        HarnessToolCallingManager manager = manager();
        ToolBaggage baggage = new ToolBaggage(Map.of("tenant", "acme", "env", "prod"));
        manager.setToolBaggage(baggage);
        ConcurrentLinkedQueue<Object> seen = new ConcurrentLinkedQueue<>();

        dispatch(manager, probingTool("probe", seen));

        assertThat(seen).hasSize(1);
        assertThat(seen.peek()).isInstanceOf(Map.class);
        assertThat(cast(seen.peek())).containsEntry("tenant", "acme").containsEntry("env", "prod");
    }

    @Test
    void emptyBaggageInjectsNothing() {
        HarnessToolCallingManager manager = manager();
        manager.setToolBaggage(new ToolBaggage()); // 空——零注入
        ConcurrentLinkedQueue<Object> seen = new ConcurrentLinkedQueue<>();

        dispatch(manager, probingTool("probe", seen));

        assertThat(seen).hasSize(1);
        assertThat(seen.peek()).isEqualTo(ABSENT); // 键都不在——零开销
    }

    @Test
    void runtimePutVisibleOnNextCall_noBaggageSetMeansNothing() {
        HarnessToolCallingManager manager = manager(); // 未接行李——键不在
        ConcurrentLinkedQueue<Object> seen = new ConcurrentLinkedQueue<>();
        ToolCallback probe = probingTool("probe", seen);

        dispatch(manager, probe);
        assertThat(seen.poll()).isEqualTo(ABSENT); // 未接 = 零注入

        ToolBaggage baggage = new ToolBaggage();
        manager.setToolBaggage(baggage); // 装配后接上
        baggage.put("correlation", "order-42"); // 运行时动态挂
        dispatch(manager, probe);
        assertThat(cast(seen.peek()))
                .containsEntry("correlation", "order-42"); // 下次调用即见
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> cast(Object seen) {
        return (Map<String, String>) seen;
    }
}
