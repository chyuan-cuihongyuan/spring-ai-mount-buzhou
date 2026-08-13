package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ToolCallSpec;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-10 / T35 事务性并行批——批提交语义（LangGraph superstep 修正版）：
 * FAILED_ONLY 策略下同伴失败时成功者结果只入事件日志（占位提示替代）、失败信号聚焦回喂；
 * 默认 ALL 全量回喂。整批 ToolResponse 同轮注入（状态层原子；副作用不谎称回滚）。
 */
class TransactionalBatchEndToEndTest {

    @Test
    void failedOnlyPolicyDefersSuccessfulResultsToEventLog() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.parallel(new ToolCallSpec("good_tool", "{}"),
                        new ToolCallSpec("bad_tool", "{}")),
                ScriptStep.text("按失败信号收尾"));
        List<String> executed = new CopyOnWriteArrayList<>();

        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), java.util.Map.of(), List.of(),
                List.of(ctx -> {
                    if (ctx.toolManager() != null) {
                        ctx.toolManager().setBatchFeedbackPolicy(
                                HarnessToolCallingManager.BatchFeedbackPolicy.FAILED_ONLY);
                    }
                }), null);

        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), config,
                fixedTool("good_tool", "非常长的成功结果：" + "s".repeat(500), executed),
                throwingTool("bad_tool", executed));
        AgentSession session = runtime.spawn("batch-app", "agent", "failed-only");
        String reply = session.chat("并行执行");
        session.close();

        assertThat(reply).isEqualTo("按失败信号收尾");
        // 两个工具都执行了（成功者结果已入事件日志——批记录暂存）
        assertThat(executed).containsExactlyInAnyOrder("good_tool", "bad_tool");
        // 模型只见失败反馈 + 成功者占位提示（含 toolCallId 回查指引），不见成功结果原文
        String feedback = model.seenPrompts.get(1).getInstructions().toString();
        assertThat(feedback).contains("[工具执行失败]");
        assertThat(feedback).contains("结果已入事件日志").contains("toolCallId=");
        assertThat(feedback).doesNotContain("非常长的成功结果");
    }

    @Test
    void defaultAllPolicyFeedsEveryResultBack() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.parallel(new ToolCallSpec("good_tool", "{}"),
                        new ToolCallSpec("bad_tool", "{}")),
                ScriptStep.text("全量收尾"));
        List<String> executed = new CopyOnWriteArrayList<>();

        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.defaults(),
                fixedTool("good_tool", "成功结果原文", executed),
                throwingTool("bad_tool", executed));
        AgentSession session = runtime.spawn("batch-app", "agent", "all-policy");
        String reply = session.chat("并行执行");
        session.close();

        assertThat(reply).isEqualTo("全量收尾");
        String feedback = model.seenPrompts.get(1).getInstructions().toString();
        assertThat(feedback).contains("成功结果原文").contains("[工具执行失败]");
    }

    private static ToolCallback fixedTool(String name, String result, List<String> sink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                sink.add(name);
                return result;
            }
        };
    }

    private static ToolCallback throwingTool(String name, List<String> sink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                sink.add(name);
                throw new IllegalStateException("disk-full");
            }
        };
    }
}
