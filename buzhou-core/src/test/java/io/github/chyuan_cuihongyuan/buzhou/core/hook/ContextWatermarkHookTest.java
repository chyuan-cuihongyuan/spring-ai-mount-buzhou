package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 181 / T554：上下文余量水位回归——未配静默 / 低水位事件翻转制 /
 * 恢复跨线 / gauge 读数 / 会话隔离 / 参数校验。
 */
class ContextWatermarkHookTest {

    /** 最小 ModelCallContext 桩（beforeModel 读 request/sessionId/emitEvent）。 */
    private static final class StubModelCallContext implements ModelCallContext {
        private final HookEnvironment env;
        private final ChatClientRequest request;
        final List<SessionEvent> emitted = new CopyOnWriteArrayList<>();

        StubModelCallContext(HookEnvironment env, ChatClientRequest request) {
            this.env = env;
            this.request = request;
        }

        @Override public String sessionId() { return env.sessionId(); }
        @Override public String agentName() { return env.agentName(); }
        @Override public int turn() { return 1; }
        @Override public SessionStateHandle state() { return env.stateHandle(); }
        @Override public void emitEvent(SessionEvent event) { emitted.add(event); }
        @Override public ChatClientRequest request() { return request; }
        @Override public org.springframework.ai.chat.client.ChatClientResponse response() { return null; }
        @Override public Throwable error() { return null; }
        @Override public void replaceRequest(ChatClientRequest newRequest) { }
        @Override public void replaceResponse(
                org.springframework.ai.chat.client.ChatClientResponse newResponse) { }
    }

    private static ChatClientRequest requestOf(String... texts) {
        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        for (String text : texts) {
            messages.add(new UserMessage(text));
        }
        return ChatClientRequest.builder().prompt(new Prompt(messages)).build();
    }

    @Test
    void unconfiguredCapacityStaysSilent() {
        ContextWatermarkHook hook = new ContextWatermarkHook(); // 默认 disabled
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        StubModelCallContext ctx = new StubModelCallContext(env,
                requestOf("很长".repeat(1000)));

        assertThat(hook.beforeModel(ctx)).isEqualTo(HookResult.CONTINUE);
        assertThat(ctx.emitted).isEmpty();
        assertThat(hook.lastUtilization()).isEqualTo(-1); // 未估算
    }

    @Test
    void lowWatermarkEventFiresOncePerCrossing() {
        // 容量 1000 字符，低水位 0.8：800+ 触发
        ContextWatermarkHook hook = new ContextWatermarkHook(
                new ContextWatermarkHook.Config(1000, 0.8));
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        StubModelCallContext heavy = new StubModelCallContext(env, requestOf("字".repeat(900)));

        hook.beforeModel(heavy);
        assertThat(heavy.emitted).hasSize(1);
        SessionEvent event = heavy.emitted.getFirst();
        assertThat(event.type()).isEqualTo(ContextWatermarkHook.EVENT_LOW_WATERMARK);
        assertThat(event.payload()).containsEntry("chars", 900L)
                .containsEntry("remainingChars", 100L);
        assertThat(hook.lastUtilization()).isEqualTo(0.9);

        hook.beforeModel(heavy); // 仍低水位——翻转制不刷屏
        assertThat(heavy.emitted).hasSize(1);
    }

    @Test
    void dippingBelowAndBackFiresAgain() {
        ContextWatermarkHook hook = new ContextWatermarkHook(
                new ContextWatermarkHook.Config(1000, 0.8));
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        StubModelCallContext heavy = new StubModelCallContext(env, requestOf("字".repeat(850)));
        StubModelCallContext light = new StubModelCallContext(env, requestOf("字".repeat(100)));

        hook.beforeModel(heavy);
        hook.beforeModel(light); // 退出低水位（静默恢复）

        assertThat(heavy.emitted).hasSize(1);
        assertThat(light.emitted).isEmpty();

        hook.beforeModel(heavy); // 再次进入——再发一次
        assertThat(heavy.emitted).hasSize(2);
        assertThat(hook.beforeModel(heavy)).isEqualTo(HookResult.CONTINUE); // 仍低水位不再发
        assertThat(heavy.emitted).hasSize(2);
    }

    @Test
    void sessionsTrackWatermarkIndependently() {
        ContextWatermarkHook hook = new ContextWatermarkHook(
                new ContextWatermarkHook.Config(1000, 0.8));
        HookEnvironment one = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        HookEnvironment two = new HookEnvironment("s2", "a", new InMemorySessionStateStore());
        StubModelCallContext heavyOne = new StubModelCallContext(one, requestOf("字".repeat(900)));
        StubModelCallContext heavyTwo = new StubModelCallContext(two, requestOf("字".repeat(900)));

        hook.beforeModel(heavyOne);
        hook.beforeModel(heavyTwo); // 各自首跨线——各发一次

        assertThat(heavyOne.emitted).hasSize(1);
        assertThat(heavyTwo.emitted).hasSize(1);
    }

    @Test
    void utilizationCappedAtOneAndNullSafeText() {
        ContextWatermarkHook hook = new ContextWatermarkHook(
                new ContextWatermarkHook.Config(100, 0.99));
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        Prompt prompt = new Prompt(List.of(new AssistantMessage(null))); // null 文本安全
        StubModelCallContext ctx = new StubModelCallContext(env,
                ChatClientRequest.builder().prompt(prompt).build());

        hook.beforeModel(ctx);
        assertThat(hook.lastChars()).isZero();
        assertThat(hook.lastUtilization()).isZero();
    }

    @Test
    void configValidated() {
        assertThatThrownBy(() -> new ContextWatermarkHook.Config(-1, 0.8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContextWatermarkHook.Config(100, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
