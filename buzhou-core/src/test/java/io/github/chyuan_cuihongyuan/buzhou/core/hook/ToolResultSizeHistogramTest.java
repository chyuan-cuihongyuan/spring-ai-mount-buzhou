package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1401 / T2104：工具结果字节直方分桶——E2E 经 HookedToolCallback 真实
 * 执行路径：幂次边界落桶、守恒式 successes=Σbuckets、失败不入字节分布、
 * reset 归零；hook 只读不裁决（回复语义不变）。
 */
class ToolResultSizeHistogramTest {

    static ToolCallback tool(final String name, final String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    static ToolCallback throwingTool(final String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("工具故意失败");
            }
        };
    }

    static AssistantMessage toolCall(String id, String name) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, "{}")))
                .build();
    }

    private AgentRuntime runtime(ScriptedChatModel model, ToolResultSizeHistogram histogram,
                                 ToolCallback... tools) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        return Buzhou.runtime(model, stores,
                new io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig(
                        List.of(histogram), Set.of(), Set.of(), null, List.of()),
                tools);
    }

    @Test
    void shouldBucketResultsByUtf8ByteBoundaries() {
        ScriptedChatModel model = new ScriptedChatModel();
        ToolResultSizeHistogram histogram = new ToolResultSizeHistogram();
        // 六个尺寸各落一桶：10B / 300B / 2KB / 5KB / 20KB / 100KB（ASCII 字节==字符）
        model.enqueue(toolCall("tc-1", "t0"));
        model.enqueue(new AssistantMessage("继续"));
        model.enqueue(toolCall("tc-2", "t1"));
        model.enqueue(new AssistantMessage("继续"));
        model.enqueue(toolCall("tc-3", "t2"));
        model.enqueue(new AssistantMessage("继续"));
        model.enqueue(toolCall("tc-4", "t3"));
        model.enqueue(new AssistantMessage("继续"));
        model.enqueue(toolCall("tc-5", "t4"));
        model.enqueue(new AssistantMessage("继续"));
        model.enqueue(toolCall("tc-6", "t5"));
        model.enqueue(new AssistantMessage("收尾"));
        AgentRuntime runtime = runtime(model, histogram,
                tool("t0", "x".repeat(10)),
                tool("t1", "x".repeat(300)),
                tool("t2", "x".repeat(2048)),
                tool("t3", "x".repeat(5000)),
                tool("t4", "x".repeat(20000)),
                tool("t5", "x".repeat(100000)));
        AgentSession session = runtime.spawn("app", "agent", "s-hist");
        for (int i = 0; i < 5; i++) {
            session.chat("调用第" + i + "个工具");
        }
        session.chat("收尾轮");
        session.close();

        ToolResultSizeHistogram.Snapshot s = histogram.stats();
        assertThat(s.b0()).isEqualTo(1);
        assertThat(s.b1()).isEqualTo(1);
        assertThat(s.b2()).isEqualTo(1);
        assertThat(s.b3()).isEqualTo(1);
        assertThat(s.b4()).isEqualTo(1);
        assertThat(s.overflow()).isEqualTo(1);
        assertThat(s.successes()).isEqualTo(6);
        assertThat(s.executed()).isEqualTo(6);
        assertThat(s.failed()).isZero();
        assertThat(s.totalBytes()).isEqualTo(10L + 300 + 2048 + 5000 + 20000 + 100000);
    }

    @Test
    void failedToolCallsMustNotEnterByteDistribution() {
        ScriptedChatModel model = new ScriptedChatModel();
        ToolResultSizeHistogram histogram = new ToolResultSizeHistogram();
        model.enqueue(toolCall("tc-1", "boom"));
        model.enqueue(new AssistantMessage("已兜底"));
        AgentRuntime runtime = runtime(model, histogram, throwingTool("boom"));
        AgentSession session = runtime.spawn("app", "agent", "s-hist-err");
        // 错误即反馈通道：Turn 不死，回复照常返回
        assertThat(session.chat("调用会失败的工具")).isEqualTo("已兜底");
        session.close();

        ToolResultSizeHistogram.Snapshot s = histogram.stats();
        assertThat(s.executed()).isEqualTo(1);
        assertThat(s.failed()).isEqualTo(1);
        assertThat(s.successes()).isZero();
        assertThat(s.totalBytes()).isZero();
        assertThat(s.executed()).isEqualTo(s.successes() + s.failed());
    }

    @Test
    void resetForTestShouldZeroAllCounters() {
        ToolResultSizeHistogram histogram = new ToolResultSizeHistogram();
        histogram.afterTool(new ToolCallContext() {
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
                return java.util.Map.of();
            }

            @Override
            public Object result() {
                return "abc";
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
                return "s-hist-reset";
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
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
                return null; // 直调 afterTool 的最小替身——histogram 不触 state
            }

            @Override
            public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) {
            }
        });
        assertThat(histogram.stats().executed()).isEqualTo(1);
        histogram.resetForTest();
        assertThat(histogram.stats()).isEqualTo(new ToolResultSizeHistogram.Snapshot(
                0, 0, 0, 0, 0, 0, 0, 0, 0));
    }
}
