package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1412 / T2126：工具入参字节直方——E2E 经 HookedToolCallback 真实调用
 * 路径（脚本模型驱动 toolCall）：空参/小参/大参落桶、executed 守恒、totalBytes
 * 精确、beforeTool 只读零裁决（工具照常执行）。
 */
class ToolInputSizeHistogramTest {

    static ToolCallback tool(final String name, final String result) {
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return org.springframework.ai.tool.definition.ToolDefinition.builder()
                        .name(name).description("d").inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    static AssistantMessage toolCall(String id, String name, String argsJson) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, argsJson)))
                .build();
    }

    private AgentRuntime runtime(ScriptedChatModel model, ToolInputSizeHistogram histogram,
                                 ToolCallback... tools) {
        return Buzhou.runtime(model, Buzhou.inMemoryStores(),
                new io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig(
                        List.of(histogram), java.util.Set.of(), java.util.Set.of(),
                        null, List.of()),
                tools);
    }

    @Test
    void shouldBucketInputSizesWithConservation() {
        ScriptedChatModel model = new ScriptedChatModel();
        ToolInputSizeHistogram histogram = new ToolInputSizeHistogram();
        model.enqueue(toolCall("tc-1", "t0", "{}"));
        model.enqueue(new AssistantMessage("继续"));
        model.enqueue(toolCall("tc-2", "t1", "{\"path\":\"a.txt\"}"));
        model.enqueue(new AssistantMessage("继续"));
        String big = "{\"text\":\"" + "x".repeat(5000) + "\"}";
        model.enqueue(toolCall("tc-3", "t2", big));
        model.enqueue(new AssistantMessage("收尾"));
        AgentRuntime runtime = runtime(model, histogram,
                tool("t0", "r0"), tool("t1", "r1"), tool("t2", "r2"));
        AgentSession session = runtime.spawn("app", "agent", "s-inh");
        session.chat("第一轮");
        session.chat("第二轮");
        session.chat("收尾轮");
        session.close();

        var s = histogram.stats();
        assertThat(s.executed()).isEqualTo(3);
        assertThat(s.bucketSum()).isEqualTo(3);
        assertThat(s.b0()).isEqualTo(2); // {} 与 {"path":"a.txt"} 均 <256B
        assertThat(s.b2()).isZero();
        // 5KB+ 引号键 ≈ 5013 字节 → b3 (<16KB)
        assertThat(s.b3()).isEqualTo(1);
        assertThat(s.totalBytes()).isGreaterThan(5000);
    }

    @Test
    void directCallWithEmptyArgumentsCountsTwoBytes() {
        ToolInputSizeHistogram histogram = new ToolInputSizeHistogram();
        histogram.beforeTool(minimalContext(java.util.Map.of()));
        var s = histogram.stats();
        assertThat(s.executed()).isEqualTo(1);
        assertThat(s.totalBytes()).isEqualTo(2); // "{}"
        assertThat(s.b0()).isEqualTo(1);
    }

    @Test
    void resetForTestClearsAllBuckets() {
        ToolInputSizeHistogram histogram = new ToolInputSizeHistogram();
        histogram.beforeTool(minimalContext(java.util.Map.of("k", "v")));
        assertThat(histogram.stats().executed()).isEqualTo(1);
        histogram.resetForTest();
        assertThat(histogram.stats()).isEqualTo(new ToolInputSizeHistogram.Snapshot(
                0, 0, 0, 0, 0, 0, 0, 0));
    }

    /** afterTool 直调最小替身（不触链）。 */
    private ToolCallContext minimalContext(java.util.Map<String, Object> arguments) {
        return new ToolCallContext() {
            @Override
            public String toolCallId() {
                return "tc-x";
            }

            @Override
            public String toolName() {
                return "t";
            }

            @Override
            public java.util.Map<String, Object> arguments() {
                return arguments;
            }

            @Override
            public Object result() {
                return null;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public void replaceArguments(java.util.Map<String, Object> newArguments) {
            }

            @Override
            public void replaceResult(Object newResult) {
            }

            @Override
            public String sessionId() {
                return "s-inh-reset";
            }

            @Override
            public String agentName() {
                return "ag";
            }

            @Override
            public int turn() {
                return 1;
            }

            @Override
            public SessionStateHandle state() {
                return null;
            }

            @Override
            public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) {
            }
        };
    }
}
