package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.AuthTtl;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.ConfirmOption;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.Confirmation;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.DangerousToolGuardHook;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.GuardAuthApi;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HITL 危险守卫端到端（接缝 = AgentSession + 内存 SPI + ScriptedChatModel，对齐 spec 测试决策）。
 * 验收 checklist（ticket 12）四项 + 通配/指纹粒度在此覆盖。
 */
class HitlGuardEndToEndTest {

    @Test
    void unauthorizedDangerousCallBlockedAndConfirmationEventEmitted() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RecordingTool tool = new RecordingTool("run_command");
        GuardModule guard = GuardModule.builder(stores)
                .dangerousTool("run_command", "confirm_run_command", "即将执行：${command}",
                        List.of(new ConfirmOption("approve", "允许", "approve", false, "", "text"),
                                new ConfirmOption("approval", "提交审批", "approval", true, "审批人", "text"),
                                new ConfirmOption("reject", "拒绝", "reject", false, "", "text")))
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores, guard.configure(), tool);

        // 第一轮：模型调危险工具 run_command → 守卫 BLOCK，工具结果回注"等待人工确认"
        model.enqueue(toolCall("tc-1", "run_command", "{\"command\":\"deploy --env=prod\"}"));
        // 第二轮：模型看到"等待人工确认"后回复"请您确认"
        model.enqueue(new AssistantMessage("需要您确认后才能执行"));

        AgentSession session = runtime.spawn("app", "agent", "sess-block");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);

        String reply = session.chat("执行部署");

        assertThat(reply).isEqualTo("需要您确认后才能执行");
        // 工具未实际执行
        assertThat(tool.callCount.get()).isEqualTo(0);
        // 确认请求事件经 listener 透出
        assertThat(events).anyMatch(e -> DangerousToolGuardHook.EVENT_CONFIRMATION_REQUESTED.equals(e.type()));
        SessionEvent request = events.stream()
                .filter(e -> DangerousToolGuardHook.EVENT_CONFIRMATION_REQUESTED.equals(e.type()))
                .findFirst().orElseThrow();
        Map<String, Object> payload = request.payload();
        assertThat(payload).containsEntry("toolName", "run_command");
        assertThat(payload).containsEntry("requiredState", "confirm_run_command");
        assertThat(payload).containsEntry("hint", "即将执行：deploy --env=prod");
        // confirmation schema 含多选项 + 单输入控件
        @SuppressWarnings("unchecked")
        Map<String, Object> confirmation = (Map<String, Object>) payload.get("confirmation");
        assertThat(confirmation).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) confirmation.get("options");
        assertThat(options).hasSize(3);
        assertThat(options).anyMatch(o -> "approval".equals(o.get("id")) && Boolean.TRUE.equals(o.get("hasInput")));
        // 阻断审计事件
        assertThat(events).anyMatch(e -> DangerousToolGuardHook.EVENT_GUARD_BLOCKED.equals(e.type()));
        session.close();
    }

    @Test
    void approveThenResendReleasesOnceAndConsumesAuth() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RecordingTool tool = new RecordingTool("run_command");
        GuardModule guard = GuardModule.builder(stores)
                .dangerousTool("run_command", "confirm_run", "即将执行")
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores, guard.configure(), tool);
        Map<String, Object> args = Map.of("command", "deploy");

        // 第一轮：BLOCK
        model.enqueue(toolCall("tc-1", "run_command", "{\"command\":\"deploy\"}"));
        model.enqueue(new AssistantMessage("等待确认"));
        AgentSession session = runtime.spawn("app", "agent", "sess-once");
        session.chat("部署");
        assertThat(tool.callCount.get()).isEqualTo(0);

        // 业务写回授权
        guard.authApi().approve("sess-once", "run_command", args);

        // 第二轮：重发同一输入 → 守卫命中指纹 → 放行（一次性消费）
        model.enqueue(toolCall("tc-2", "run_command", "{\"command\":\"deploy\"}"));
        model.enqueue(new AssistantMessage("部署完成"));
        String reply = session.chat("部署");
        assertThat(reply).isEqualTo("部署完成");
        assertThat(tool.callCount.get()).isEqualTo(1); // 工具实际执行

        // auth key 已被消费删除
        assertThat(guard.authApi().isAuthorized("sess-once", "run_command", args)).isFalse();

        // 第三轮：同输入再调 → 再次 BLOCK（一次性已消费）
        model.enqueue(toolCall("tc-3", "run_command", "{\"command\":\"deploy\"}"));
        model.enqueue(new AssistantMessage("再次等待确认"));
        session.chat("再部署");
        assertThat(tool.callCount.get()).isEqualTo(1); // 仍未执行（第二次因消费后重新阻断）
        session.close();
    }

    @Test
    void sessionLongAuthReleasesMultipleTimesWithoutConsuming() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RecordingTool tool = new RecordingTool("run_command");
        GuardModule guard = GuardModule.builder(stores)
                .authTtl(AuthTtl.SESSION)
                .dangerousTool("run_command", "confirm_run", "即将执行")
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores, guard.configure(), tool);
        Map<String, Object> args = Map.of("command", "deploy");

        guard.authApi().approve("sess-long", "run_command", args);
        AgentSession session = runtime.spawn("app", "agent", "sess-long");

        // 连续三轮同指纹调用均放行
        for (int i = 1; i <= 3; i++) {
            model.enqueue(toolCall("tc-" + i, "run_command", "{\"command\":\"deploy\"}"));
            model.enqueue(new AssistantMessage("第" + i + "次完成"));
            session.chat("第" + i + "次部署");
        }
        assertThat(tool.callCount.get()).isEqualTo(3);
        // 长效授权仍保留
        assertThat(guard.authApi().isAuthorized("sess-long", "run_command", args)).isTrue();
        session.close();
    }

    @Test
    void crossInstanceAuthorizationReleasesOnSharedStore() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores(); // 共享 store
        RecordingTool tool = new RecordingTool("run_command");
        GuardModule guard = GuardModule.builder(stores)
                .dangerousTool("run_command", "confirm_run", "即将执行")
                .build();
        Map<String, Object> args = Map.of("command", "deploy");

        // 实例 A 写回授权
        guard.authApi().approve("sess-cross", "run_command", args);

        // 实例 B（同 sessionId 续跑）放行
        AgentRuntime runtimeB = Buzhou.runtime(model, stores, guard.configure(), tool);
        model.enqueue(toolCall("tc-1", "run_command", "{\"command\":\"deploy\"}"));
        model.enqueue(new AssistantMessage("跨实例执行完成"));
        AgentSession sessionB = runtimeB.spawn("app", "agent", "sess-cross");
        String reply = sessionB.chat("部署");
        assertThat(reply).isEqualTo("跨实例执行完成");
        assertThat(tool.callCount.get()).isEqualTo(1);
        sessionB.close();
    }

    @Test
    void globPatternMatchesByPrefix() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RecordingTool prodTool = new RecordingTool("mcp:prod_deploy");
        RecordingTool devTool = new RecordingTool("mcp:dev_query");
        GuardModule guard = GuardModule.builder(stores)
                .dangerousTool("mcp:prod_*", "confirm_prod", "生产操作")
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores, guard.configure(), prodTool, devTool);
        guard.authApi().approve("sess-glob", "mcp:prod_deploy", Map.of());

        // mcp:prod_deploy 命中通配 → 已授权 → 放行
        model.enqueue(toolCall("tc-1", "mcp:prod_deploy", "{}"));
        model.enqueue(new AssistantMessage("prod 完成"));
        AgentSession session = runtime.spawn("app", "agent", "sess-glob");
        session.chat("prod 部署");
        assertThat(prodTool.callCount.get()).isEqualTo(1);

        // mcp:dev_query 不命中通配 → 不拦截 → 直接执行
        model.enqueue(toolCall("tc-2", "mcp:dev_query", "{}"));
        model.enqueue(new AssistantMessage("dev 完成"));
        session.chat("dev 查询");
        assertThat(devTool.callCount.get()).isEqualTo(1);
        session.close();
    }

    @Test
    void fingerprintGranularityArgsADoesNotCoverArgsB() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RecordingTool tool = new RecordingTool("run_command");
        GuardModule guard = GuardModule.builder(stores)
                .dangerousTool("run_command", "confirm_run", "即将执行")
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores, guard.configure(), tool);

        // 只授权 deploy 命令
        guard.authApi().approve("sess-fp", "run_command", Map.of("command", "deploy"));
        AgentSession session = runtime.spawn("app", "agent", "sess-fp");

        // deploy 放行
        model.enqueue(toolCall("tc-1", "run_command", "{\"command\":\"deploy\"}"));
        model.enqueue(new AssistantMessage("deploy 完成"));
        session.chat("deploy");
        assertThat(tool.callCount.get()).isEqualTo(1);

        // rollback 不同参数 → 不同指纹 → 未授权 → BLOCK
        model.enqueue(toolCall("tc-2", "run_command", "{\"command\":\"rollback\"}"));
        model.enqueue(new AssistantMessage("rollback 需确认"));
        session.chat("rollback");
        assertThat(tool.callCount.get()).isEqualTo(1); // 仍未执行 rollback
        session.close();
    }

    // --- helpers ---

    static AssistantMessage toolCall(String id, String name, String argsJson) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, argsJson)))
                .build();
    }

    static class RecordingTool implements ToolCallback {
        final String name;
        final AtomicInteger callCount = new AtomicInteger();

        RecordingTool(String name) {
            this.name = name;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name(name).description(name)
                    .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
        }

        @Override
        public String call(String toolInput) {
            callCount.incrementAndGet();
            return "{\"ok\":true}";
        }
    }

    static class ScriptedChatModel implements ChatModel {
        final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
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
