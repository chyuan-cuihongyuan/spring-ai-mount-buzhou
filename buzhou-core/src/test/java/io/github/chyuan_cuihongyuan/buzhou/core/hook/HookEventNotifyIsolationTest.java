package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1501 / T2253–T2254：HookChain 事件通知面逐 hook 异常隔离——
 * 首位 hook 的 onEvent 抛异常不吞掉后续 hook 的事件消费；计时仍入账；
 * 裁决面（beforeTurn 等）fail-fast 语义零变化。
 */
class HookEventNotifyIsolationTest {

    /** onEvent 记录所见事件类型；可选抛异常（缺陷 hook 替身）。 */
    private static final class EventHook implements BuzhouHook {
        final String name;
        final List<String> seen;
        final boolean throwOnEvent;

        EventHook(String name, List<String> seen, boolean throwOnEvent) {
            this.name = name;
            this.seen = seen;
            this.throwOnEvent = throwOnEvent;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void onEvent(SessionEventContext ctx) {
            if (throwOnEvent) {
                throw new IllegalStateException("缺陷 hook onEvent 崩溃：" + name);
            }
            seen.add(name + ":" + ctx.event().type());
        }
    }

    /** 最小事件上下文 stub（事件类型固定 turn.completed，HookContext 五方法空实现）。 */
    private static SessionEventContext stubEventCtx() {
        SessionEvent event = new SessionEvent("turn.completed", Map.of(), Instant.now());
        return new SessionEventContext() {
            @Override
            public SessionEvent event() {
                return event;
            }

            @Override
            public String sessionId() {
                return "s-evt";
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
            public SessionStateHandle state() {
                return null;
            }

            @Override
            public void emitEvent(SessionEvent e) {
            }
        };
    }

    /** ① 首位 hook 抛异常：后续 hook 仍收到同一事件（通知面逐 hook 隔离）。 */
    @Test
    void firstHookFailureShouldNotSwallowRemainingHooksEvent() {
        List<String> seen = new CopyOnWriteArrayList<>();
        HookChain chain = HookChain.of(List.of(
                new EventHook("broken", seen, true),
                new EventHook("healthy", seen, false)));

        chain.fireEvent(stubEventCtx());

        assertThat(seen).containsExactly("healthy:turn.completed");
    }

    /** ② 抛异常调用的 onEvent 耗时仍入 stats（计时不丢账）。 */
    @Test
    void failedCallTimingShouldStillBeRecorded() {
        List<String> seen = new CopyOnWriteArrayList<>();
        HookChain chain = HookChain.of(List.of(
                new EventHook("broken", seen, true)));

        chain.fireEvent(stubEventCtx());

        HookChain.HookTiming timing = chain.stats().get("broken");
        assertThat(timing).isNotNull();
        assertThat(timing.count()).isEqualTo(1);
    }

    /** ③ 裁决面 fail-fast 语义零变化：beforeTurn 抛异常仍向上传播（治理点异常必须可见）。 */
    @Test
    void verdictFaceMustStayFailFast() {
        BuzhouHook brokenVerdict = new BuzhouHook() {
            @Override
            public String name() {
                return "broken-verdict";
            }

            @Override
            public HookResult beforeTurn(TurnContext ctx) {
                throw new IllegalStateException("治理点 beforeTurn 崩溃");
            }
        };
        HookChain chain = HookChain.of(List.of(brokenVerdict));
        SessionEventContext evt = stubEventCtx();

        assertThatThrownBy(() -> chain.beforeTurn(new TurnContext() {
            @Override
            public String input() {
                return "in";
            }

            @Override
            public String response() {
                return null;
            }

            @Override
            public void replaceInput(String newInput) {
            }

            @Override
            public void replaceResponse(String newResponse) {
            }

            @Override
            public String sessionId() {
                return evt.sessionId();
            }

            @Override
            public String agentName() {
                return evt.agentName();
            }

            @Override
            public int turn() {
                return evt.turn();
            }

            @Override
            public SessionStateHandle state() {
                return null;
            }

            @Override
            public void emitEvent(SessionEvent event) {
            }
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("治理点 beforeTurn 崩溃");
    }
}
