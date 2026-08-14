package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-30 / spec 13 §core-1：close() 与事件分发的异常隔离——单个 observer/listener 异常
 * 不跳过其余清理（既有实现里 dispatchEvent 抛出会跳过 listeners.clear()；observer.onClose
 * 抛出会跳过资源注册表关闭——本片补齐「清理优先、异常收集」）。
 */
class DefaultAgentSessionCloseIsolationTest {

    @Test
    void shouldCompleteCleanupAndIsolateFailures_whenObserverAndListenersThrow() {
        AtomicBoolean probeResourceClosed = new AtomicBoolean();
        List<String> eventsSeenByHealthyListener = new CopyOnWriteArrayList<>();
        SessionObserver throwingObserver = new SessionObserver() {
            @Override
            public void onClose() {
                throw new IllegalStateException("observer 关闭通知炸了");
            }
        };
        // 探针资源（验证 registry.closeAll 仍被调用）+ 抛异常 observer 经装配注入
        SessionResourceCustomizer probeResource = (registry, appId, agentName, sessionId) ->
                registry.register("close-probe", () -> probeResourceClosed.set(true));
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("回复"));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig
                .merge(RuntimeConfig.defaults(),
                        RuntimeConfig.sessionCustomizers(List.of(probeResource)),
                        RuntimeConfig.assemblyCustomizers(List.of(
                                ctx -> ctx.addObserver(throwingObserver)))));
        AgentSession session = runtime.spawn("app", "agent", "close-isolation");

        SessionEventListener throwingListener = event -> {
            throw new IllegalStateException("listener 分发炸了");
        };
        session.addEventListener(throwingListener);
        session.addEventListener(event -> eventsSeenByHealthyListener.add(event.type()));

        // observer.onClose 抛异常 → 清理仍全部执行、收集后上抛（清理优先、异常不吞）
        org.assertj.core.api.Assertions.assertThatThrownBy(session::close)
                .hasMessageContaining("observer 关闭通知炸了");

        // 健康 listener 不被坏 listener 阻断：session.closed 事件仍送达
        assertThat(eventsSeenByHealthyListener).contains("session.closed");
        // 资源注册表关闭未被跳过（探针资源已关闭）
        assertThat(probeResourceClosed.get()).isTrue();
        // 会话已进入关闭态（listeners.clear 等清理执行完毕的既有可观测面）
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> session.chat("已关闭"))
                .hasMessageContaining("already closed");
    }

    @Test
    void shouldIsolateListenerFailure_whenDispatchingNonCloseEvents() {
        List<String> eventsSeenByHealthyListener = new CopyOnWriteArrayList<>();
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("回复"));
        AgentRuntime runtime = Buzhou.runtime(model);
        AgentSession session = runtime.spawn("app", "agent", "dispatch-isolation");

        session.addEventListener(event -> {
            throw new IllegalStateException("坏 listener");
        });
        session.addEventListener(event -> eventsSeenByHealthyListener.add(event.type()));

        // cancel() 的 session.cancelled 事件分发：坏 listener 不外溢、不阻断健康 listener
        session.cancel();

        assertThat(eventsSeenByHealthyListener).contains("session.cancelled");
        session.close();
        assertThat(eventsSeenByHealthyListener).contains("session.closed");
    }
}
