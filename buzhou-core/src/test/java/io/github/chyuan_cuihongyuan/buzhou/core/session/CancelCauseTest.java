package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 取消原因传播测试（spec 606 / T862–T863 / impl 459，gRPC status codes 思想）：
 * 既有 cancel 路径默认 cause=USER、显式 cause 进事件 payload、停机排水带 SHUTDOWN_DRAIN。
 */
class CancelCauseTest {

    private static final class EventCaptureHook implements BuzhouHook {
        final List<SessionEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionEventContext ctx) {
            events.add(ctx.event());
        }
    }

    /** 既有两参/无参 cancel：事件 cause 默认 USER（零行为变化）。 */
    @Test
    void legacyCancelDefaultsToUserCause() {
        EventCaptureHook capture = new EventCaptureHook();
        AgentRuntime runtime = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(),
                RuntimeConfig.merge(RuntimeConfig.defaults(), RuntimeConfig.hooks(List.of(capture))));
        AgentSession session = runtime.spawn("app", "agent", "s1");

        session.cancel(CancelMode.IMMEDIATE);

        SessionEvent event = capture.events.stream()
                .filter(e -> "session.cancelled".equals(e.type())).findFirst().orElseThrow();
        assertThat(event.payload()).containsEntry("cancelMode", "IMMEDIATE");
        assertThat(event.payload()).containsEntry("cause", "USER");
        session.close();
    }

    /** 显式 cause：进入事件 payload（DEADLINE 例）。 */
    @Test
    void explicitCauseCarriedIntoEvent() {
        EventCaptureHook capture = new EventCaptureHook();
        AgentRuntime runtime = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(),
                RuntimeConfig.merge(RuntimeConfig.defaults(), RuntimeConfig.hooks(List.of(capture))));
        AgentSession session = runtime.spawn("app", "agent", "s1");

        session.cancel(CancelMode.AFTER_CURRENT_TOOLS, CancelCause.DEADLINE);

        SessionEvent event = capture.events.stream()
                .filter(e -> "session.cancelled".equals(e.type())).findFirst().orElseThrow();
        assertThat(event.payload()).containsEntry("cancelMode", "AFTER_CURRENT_TOOLS");
        assertThat(event.payload()).containsEntry("cause", "DEADLINE");
        session.close();
    }

    /** null cause 诚实缺省 USER（防御调用方）。 */
    @Test
    void nullCauseFallsBackToUser() {
        EventCaptureHook capture = new EventCaptureHook();
        AgentRuntime runtime = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(),
                RuntimeConfig.merge(RuntimeConfig.defaults(), RuntimeConfig.hooks(List.of(capture))));
        AgentSession session = runtime.spawn("app", "agent", "s1");

        session.cancel(CancelMode.IMMEDIATE, null);

        SessionEvent event = capture.events.stream()
                .filter(e -> "session.cancelled".equals(e.type())).findFirst().orElseThrow();
        assertThat(event.payload()).containsEntry("cause", "USER");
        session.close();
    }
}
