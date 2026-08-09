package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 簇 3 · 护栏与 HITL（ticket 21 排障 demo）。
 *
 * <p>排障场景的改库/重启类操作必须有人工门禁：危险工具未获真实用户授权时框架层物理走不通。
 * <ul>
 *   <li>{@link #hitlRoundTripBlocksApprovesRunsAndEmitsEvent}：危险 run_command 阻断 → 确认事件经会话监听器透出 →
 *       用户授权 → 重发放行，完整 HITL 往返。</li>
 *   <li>{@link #authorizationPersistsAcrossInstances}：授权写入会话 state 持久化，全新实例凭同 sessionId 续跑即放行
 *      （多实例部署不重复要授权）。</li>
 * </ul>
 */
class GuardAndHitlDemoTest {

    @TempDir
    Path sandbox;

    @Test
    void hitlRoundTripBlocksApprovesRunsAndEmitsEvent() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        var events = new CopyOnWriteArrayList<SessionEvent>();

        ToolsModule tools = dangerousTools(stores);
        GuardModule guard = guardFor(tools, stores);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.merge(tools.configure(), guard.configure()));

        // 轮 1：重启网关 → run_command 未授权被 BLOCK
        model.enqueue(runCommand("echo restart-gateway"));
        model.enqueue(new AssistantMessage("等待用户确认"));
        AgentSession session = runtime.spawn("app", "agent", "sess-hitl");
        session.addEventListener(events::add);
        session.chat("重启网关");

        // 阻断：注入视图含等待确认；阻断事件经会话监听器透出
        assertThat(model.seenPrompts.get(1).getInstructions())
                .anyMatch(m -> m instanceof ToolResponseMessage
                        && ScriptedChatModel.contains(m, "等待人工确认"));
        assertThat(events).anyMatch(e -> "guard.tool.blocked".equals(e.type()));

        // 用户授权（业务侧 REST 等效调用）→ 重发同一操作 → 放行执行
        guard.authApi().approve("sess-hitl", "run_command",
                Map.of("command", "echo restart-gateway"), "approve", null);
        model.enqueue(runCommand("echo restart-gateway"));
        model.enqueue(new AssistantMessage("已重启"));
        session.chat("确认重启");
        session.close();

        // 放行：工具响应含执行结果
        assertThat(model.seenPrompts.get(3).getInstructions())
                .anyMatch(m -> ScriptedChatModel.contains(m, "restart-gateway"));
    }

    @Test
    void authorizationPersistsAcrossInstances() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();

        ToolsModule tools = dangerousTools(stores);
        GuardModule guard = guardFor(tools, stores);
        RuntimeConfig config = RuntimeConfig.merge(tools.configure(), guard.configure());

        // 实例 A：用户授权 run_command（授权写入会话 state）
        AgentRuntime runtimeA = Buzhou.runtime(model, stores, config);
        runtimeA.spawn("app", "agent", "sess-auth").close();
        guard.authApi().approve("sess-auth", "run_command",
                Map.of("command", "echo drain-node"), "approve", null);

        // 实例 B：全新 runtime、同 stores（state 持久），凭同 sessionId 续跑 → 已授权直接放行
        AgentRuntime runtimeB = Buzhou.runtime(model, stores, config);
        model.enqueue(runCommand("echo drain-node"));
        model.enqueue(new AssistantMessage("已执行"));
        AgentSession resumed = runtimeB.spawn("app", "agent", "sess-auth");
        resumed.chat("执行排空");
        resumed.close();

        // 无需再次确认即放行：工具响应含结果，无「等待人工确认」
        assertThat(model.seenPrompts.get(1).getInstructions())
                .anyMatch(m -> ScriptedChatModel.contains(m, "drain-node"));
        assertThat(model.seenPrompts.get(1).getInstructions().toString())
                .doesNotContain("等待人工确认");
    }

    /** opt-in run_command + 沙箱（两个 HITL 测试共用装配）。 */
    private ToolsModule dangerousTools(BuzhouStores stores) {
        return ToolsModule.builder(stores.sessionStateStore())
                .sandboxRoot(sandbox).runCommandEnabled(true).build();
    }

    /** 把 tools 暴露的危险工具名注册进 HITL 守卫（两个测试共用）。 */
    private GuardModule guardFor(ToolsModule tools, BuzhouStores stores) {
        GuardModule.Builder gb = GuardModule.builder(stores);
        tools.enabledDangerousToolNames().forEach(n ->
                gb.dangerousTool(n, "confirm_" + n, "即将执行 ${command}"));
        return gb.build();
    }

    private static AssistantMessage runCommand(String command) {
        return AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc", "function", "run_command",
                        "{\"command\":\"" + command + "\"}")))
                .build();
    }
}
