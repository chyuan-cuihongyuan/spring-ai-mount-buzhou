package io.github.chyuan_cuihongyuan.buzhou.guard.leak;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨会话泄漏金丝雀 hook 测试（spec 1625 / T2401–T2402 / impl 1178）：
 * beforeTurn 种植同会话恒同令牌；afterModel 扫描输出——他会话令牌即泄漏事件；
 * 自会话令牌回显不算泄漏（spec 528 接线）。
 */
class SessionCanaryHookTest {

    private static final List<SessionEvent> EVENTS = new ArrayList<>();

    private static TurnContext turnOf(String sessionId) {
        return new TurnContext() {
            @Override public String sessionId() { return sessionId; }
            @Override public String agentName() { return "agent"; }
            @Override public int turn() { return 1; }
            @Override public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() { return null; }
            @Override public void emitEvent(SessionEvent event) { }
            @Override public String input() { return ""; }
            @Override public String response() { return ""; }
            @Override public void replaceInput(String newInput) { }
            @Override public void replaceResponse(String newResponse) { }
        };
    }

    private static ModelCallContext modelOf(String sessionId, String output) {
        ChatClientResponse response = new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage(output)))),
                Map.of());
        return new ModelCallContext() {
            @Override public String sessionId() { return sessionId; }
            @Override public String agentName() { return "agent"; }
            @Override public int turn() { return 1; }
            @Override public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() { return null; }
            @Override public void emitEvent(SessionEvent event) { EVENTS.add(event); }
            @Override public ChatClientRequest request() { return null; }
            @Override public void replaceRequest(ChatClientRequest newRequest) { }
            @Override public Throwable error() { return null; }
            @Override public ChatClientResponse response() { return response; }
            @Override public void replaceResponse(ChatClientResponse newResponse) { }
        };
    }

    @Test
    void foreignTokenInOutputRaisesLeakEvent() {
        EVENTS.clear();
        SessionCanaryRegistry registry = new SessionCanaryRegistry("test-salt");
        SessionCanaryHook hook = new SessionCanaryHook(registry);

        hook.beforeTurn(turnOf("session-a"));
        hook.beforeTurn(turnOf("session-b"));
        String tokenA = registry.plant("session-a");

        // B 会话的模型输出复现 A 的令牌——跨会话污染信号
        HookResult result = hook.afterModel(modelOf("session-b",
                "参考信息：" + SessionCanaryRegistry.TOKEN_PREFIX + tokenA.substring(SessionCanaryRegistry.TOKEN_PREFIX.length())));
        assertThat(result).isInstanceOf(HookResult.Continue.class);
        assertThat(EVENTS).hasSize(1);
        assertThat(EVENTS.get(0).type()).isEqualTo(SessionCanaryHook.EVENT_LEAK_DETECTED);

        // 自会话令牌回显不算泄漏
        EVENTS.clear();
        hook.afterModel(modelOf("session-a", "echo " + tokenA));
        assertThat(EVENTS).isEmpty();
        assertThat(registry.detectedCount()).isEqualTo(1);
    }

    @Test
    void plantingIsDeterministicPerSession() {
        SessionCanaryRegistry registry = new SessionCanaryRegistry("test-salt");
        SessionCanaryHook hook = new SessionCanaryHook(registry);
        hook.beforeTurn(turnOf("stable"));
        hook.beforeTurn(turnOf("stable"));
        assertThat(registry.size()).isEqualTo(1);
    }
}
