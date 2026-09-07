package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 326 / impl-349：重复检测 hook 回归——observe-only 不 block/unstick
 * fire 回填解困指令/会话隔离/null 响应防御。
 */
class RepetitionDetectorHookTest {

    /** 最小 ModelCallContext 桩（afterModel 只读 sessionId + response）。 */
    private static final class Call implements ModelCallContext {
        private final HookEnvironment env;
        private final ChatClientResponse response;

        Call(HookEnvironment env, ChatClientResponse response) {
            this.env = env;
            this.response = response;
        }

        @Override public String sessionId() { return env.sessionId(); }
        @Override public String agentName() { return env.agentName(); }
        @Override public int turn() { return 1; }
        @Override public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle
        state() { return env.stateHandle(); }
        @Override public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) { }
        @Override public ChatClientRequest request() { return null; }
        @Override public ChatClientResponse response() { return response; }
        @Override public Throwable error() { return null; }
        @Override public void replaceRequest(ChatClientRequest newRequest) { }
        @Override public void replaceResponse(ChatClientResponse newResponse) { }
    }

    private static ChatClientResponse responseOf(String text) {
        return ChatClientResponse.builder()
                .chatResponse(new ChatResponse(java.util.List.of(
                        new Generation(new AssistantMessage(text)))))
                .build();
    }

    @Test
    void observeOnlyCountsWithoutBlocking() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(2, 80, false);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        hook.afterModel(new Call(env, responseOf("复读 复读")));
        HookResult result = hook.afterModel(new Call(env, responseOf("复读 复读")));
        assertThat(result)
                .as("默认 observe-only——fire 也不拦不换").isEqualTo(HookResult.CONTINUE);
        assertThat(hook.currentRun("s1")).isEqualTo(2);
    }

    @Test
    void unstickModeReplacesRepetitiveOutputWithInstruction() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(2, 80, true);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        hook.afterModel(new Call(env, responseOf("同一个 答案")));
        HookResult result = hook.afterModel(new Call(env, responseOf("同一个 答案")));
        assertThat(result).isInstanceOf(HookResult.Block.class);
        String reason = ((HookResult.Block) result).reason();
        assertThat(reason).startsWith("[重复检测]").contains("打转");
    }

    @Test
    void sessionsAreIndependent() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(2, 80, true);
        HookEnvironment s1 = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        HookEnvironment s2 = new HookEnvironment("s2", "a", new InMemorySessionStateStore());
        hook.afterModel(new Call(s1, responseOf("内容 一样")));
        assertThat(hook.afterModel(new Call(s2, responseOf("内容 一样"))))
                .as("s2 首条——不 fire（会话隔离）").isEqualTo(HookResult.CONTINUE);
        assertThat(hook.afterModel(new Call(s1, responseOf("内容 一样"))))
                .isInstanceOf(HookResult.Block.class);
    }

    @Test
    void nullResponseIsSafe() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(2, 80, true);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        assertThat(hook.afterModel(new Call(env, null)))
                .isEqualTo(HookResult.CONTINUE);
        assertThat(hook.afterModel(null)).isEqualTo(HookResult.CONTINUE);
    }
}
