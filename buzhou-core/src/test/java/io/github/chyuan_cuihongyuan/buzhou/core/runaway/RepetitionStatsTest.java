package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RepetitionStatsTest {

    private static final HookEnvironment ENV =
            new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    /** 可变响应 shim（afterModel 只读 response）。 */
    private record Shim(HookEnvironment env, ChatClientResponse response)
            implements io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext {
        @Override
        public String sessionId() {
            return env.sessionId();
        }

        @Override
        public String agentName() {
            return env.agentName();
        }

        @Override
        public int turn() {
            return env.currentTurn();
        }

        @Override
        public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
            return env.stateHandle();
        }

        @Override
        public void emitEvent(SessionEvent event) {
            env.emit(event);
        }

        @Override
        public ChatClientResponse request() {
            return null;
        }

        @Override
        public ChatClientResponse response() {
            return response;
        }

        @Override
        public Throwable error() {
            return null;
        }

        @Override
        public void replaceRequest(org.springframework.ai.chat.client.ChatClientRequest r) {
        }

        @Override
        public void replaceResponse(org.springframework.ai.chat.client.ChatClientResponse r) {
        }
    }

    private static io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext ctx(
            String text) {
        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage(text))));
        return new Shim(ENV, new ChatClientResponse(chatResponse, Map.of()));
    }

    @Test
    void observeOnlyModeCountsFiresWithoutBlocking() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(3, 90, false);

        for (int i = 0; i < 3; i++) {
            HookResult result = hook.afterModel(ctx("same output text"));
            assertThat(result).isSameAs(HookResult.CONTINUE);
        }

        RepetitionDetectorHook.RepetitionStats stats = hook.stats();
        assertThat(stats.fires()).isEqualTo(1);
        assertThat(stats.blocks()).isZero();
        assertThat(stats.maxRunSeen()).isEqualTo(3);
    }

    @Test
    void unstickModeCountsBlocks() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(2, 90, true);

        HookResult first = hook.afterModel(ctx("same output"));
        assertThat(first).isSameAs(HookResult.CONTINUE);
        HookResult second = hook.afterModel(ctx("same output"));
        assertThat(second).isNotSameAs(HookResult.CONTINUE);

        RepetitionDetectorHook.RepetitionStats stats = hook.stats();
        assertThat(stats.fires()).isEqualTo(1);
        assertThat(stats.blocks()).isEqualTo(1);
        assertThat(stats.maxRunSeen()).isEqualTo(2);
    }

    @Test
    void dissimilarOutputsNeverFire() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(2, 90, false);

        hook.afterModel(ctx("alpha beta gamma"));
        hook.afterModel(ctx("delta epsilon zeta"));
        hook.afterModel(ctx("eta theta iota"));

        RepetitionDetectorHook.RepetitionStats stats = hook.stats();
        assertThat(stats.fires()).isZero();
        assertThat(stats.blocks()).isZero();
        assertThat(stats.maxRunSeen()).isZero();
    }

    @Test
    void maxRunSeenKeepsPeakAfterFire() {
        RepetitionDetectorHook hook = new RepetitionDetectorHook(2, 90, false);

        hook.afterModel(ctx("loop text"));
        hook.afterModel(ctx("loop text"));
        hook.afterModel(ctx("loop text"));
        hook.afterModel(ctx("loop text"));

        RepetitionDetectorHook.RepetitionStats stats = hook.stats();
        assertThat(stats.fires()).isEqualTo(1);
        assertThat(stats.maxRunSeen()).isEqualTo(2);
    }
}
