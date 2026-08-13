package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.CancellationToken;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-05 / T31（docs/spec/12 §core-3）端到端：CancelMode 三档语义——
 * IMMEDIATE 丢在飞结果、AFTER_CURRENT_TOOLS 当前批完成后停止递归、
 * AFTER_CURRENT_TURN 本轮完整收尾；取消令牌随 ToolContext 贯穿工具链（协作式取消）。
 */
class CancelModeEndToEndTest {

    @Test
    void afterCurrentToolsLetsBatchFinishThenCutsRecursion() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("kick_off", "{}"),
                ScriptStep.toolCall("another_round", "{}"), // 批完成后模型再要工具 → 被护栏截断
                ScriptStep.text("不应到达"));
        FakeModelGuard.requireTestDouble(model);

        AtomicReference<AgentSession> sessionRef = new AtomicReference<>();
        List<String> executed = new CopyOnWriteArrayList<>();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults(),
                toolThatCancels("kick_off", sessionRef, CancelMode.AFTER_CURRENT_TOOLS, executed),
                fixedTool("another_round", executed));
        AgentSession session = runtime.spawn("cancel-app", "agent", "after-tools");
        sessionRef.set(session);
        String reply = session.chat("开始");
        session.close();

        // 当前批完成（kick_off 已执行并回喂一次）；下一次工具递归被优雅截断
        assertThat(executed).containsExactly("kick_off");
        assertThat(reply).contains("当前工具批后").contains("取消");
        assertThat(model.callCount()).isEqualTo(2); // 第二次调用的工具请求被替换为取消收尾
    }

    @Test
    void afterCurrentTurnDoesNotDisturbTheTurn() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("kick_off", "{}"),
                ScriptStep.text("本轮正常完成"));
        FakeModelGuard.requireTestDouble(model);

        AtomicReference<AgentSession> sessionRef = new AtomicReference<>();
        List<String> executed = new CopyOnWriteArrayList<>();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults(),
                toolThatCancels("kick_off", sessionRef, CancelMode.AFTER_CURRENT_TURN, executed));
        AgentSession session = runtime.spawn("cancel-app", "agent", "after-turn");
        sessionRef.set(session);
        String reply = session.chat("开始");
        session.close();

        // Turn 完整收尾：模型自然产出最终回复，取消仅为标记
        assertThat(executed).containsExactly("kick_off");
        assertThat(reply).isEqualTo("本轮正常完成");
    }

    @Test
    void immediateCutDropsFurtherRoundsAndTokenSignalsCancellation() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("self_cancel", "{}"),
                ScriptStep.toolCall("another_round", "{}"),
                ScriptStep.text("不应到达"));
        FakeModelGuard.requireTestDouble(model);

        AtomicReference<AgentSession> sessionRef = new AtomicReference<>();
        List<String> cancelTokenSeen = new CopyOnWriteArrayList<>();
        List<String> executed = new CopyOnWriteArrayList<>();
        // self_cancel：协作式取消——工具内先轮询令牌（尚未取消=false）、执行中自请 IMMEDIATE 取消
        ToolCallback selfCancel = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("self_cancel").description("self_cancel")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return call(toolInput, null);
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                boolean before = CancellationToken.from(toolContext).isCancelled();
                sessionRef.get().cancel(CancelMode.IMMEDIATE);
                boolean after = CancellationToken.from(toolContext).isCancelled();
                cancelTokenSeen.add("before=" + before + ",after=" + after);
                executed.add("self_cancel");
                return "cancelled-self";
            }
        };
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults(),
                selfCancel, fixedTool("another_round", executed));
        AgentSession session = runtime.spawn("cancel-app", "agent", "immediate");
        sessionRef.set(session);
        String reply = session.chat("开始");
        session.close();

        // 立即档：当前工具已完成（结果回喂一次），后续递归被截断为优雅取消收尾
        assertThat(executed).containsExactly("self_cancel");
        assertThat(reply).contains("立即").contains("取消");
        // 取消令牌贯穿：取消前后令牌状态翻转（协作式取消可观测）
        assertThat(cancelTokenSeen).containsExactly("before=false,after=true");
    }

    private static ToolCallback toolThatCancels(String name, AtomicReference<AgentSession> sessionRef,
                                                CancelMode mode, List<String> sink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                sink.add(name);
                sessionRef.get().cancel(mode);
                return name + "-done";
            }
        };
    }

    private static ToolCallback fixedTool(String name, List<String> sink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                sink.add(name);
                return name + "-done";
            }
        };
    }
}
