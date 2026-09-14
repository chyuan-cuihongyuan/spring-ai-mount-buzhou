package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1500 / T2251–T2252：SessionObserver 通知面异常隔离——单观察者回调抛
 * RuntimeException 不中断其余观察者、不向上传播破坏轮次/会话主流程
 * （Guava EventBus SubscriberExceptionHandler 思想；impl-30 隔离先例的
 * observer 回调面补全）。onOpen 在构造器尾部，未隔离时观测组件缺陷可炸掉
 * 整个会话构造——本测试四通道（open/start/error/cancel）逐面钉住。
 */
class ObserverNotifyIsolationTest {

    /** 全通道记录的健康观察者（排在抛异常观察者之后，验证「不跳过其余」）。 */
    private static final class RecordingObserver implements SessionObserver {
        final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public void onOpen() {
            events.add("open");
        }

        @Override
        public void onTurnStart(int turnSeq, String userInput) {
            events.add("start:" + turnSeq);
        }

        @Override
        public void onTurnEnd(int turnSeq, String finalReply) {
            events.add("end:" + turnSeq);
        }

        @Override
        public void onTurnError(int turnSeq, Throwable error) {
            events.add("error:" + turnSeq);
        }

        @Override
        public void onCancel() {
            events.add("cancel");
        }
    }

    /** 指定回调抛异常的缺陷观察者（其余通道静默——精确验证目标回调面的隔离）。 */
    private static SessionObserver throwingOn(String callback, Runnable assembly) {
        return new SessionObserver() {
            @Override
            public void onOpen() {
                if ("open".equals(callback)) {
                    assembly.run();
                    throw new IllegalStateException("缺陷观察者 onOpen 崩溃");
                }
            }

            @Override
            public void onTurnStart(int turnSeq, String userInput) {
                if ("start".equals(callback)) {
                    assembly.run();
                    throw new IllegalStateException("缺陷观察者 onTurnStart 崩溃");
                }
            }

            @Override
            public void onTurnError(int turnSeq, Throwable error) {
                if ("error".equals(callback)) {
                    assembly.run();
                    throw new IllegalStateException("缺陷观察者 onTurnError 崩溃");
                }
            }

            @Override
            public void onCancel() {
                if ("cancel".equals(callback)) {
                    assembly.run();
                    throw new IllegalStateException("缺陷观察者 onCancel 崩溃");
                }
            }
        };
    }

    /** beforeTurn 无条件 Block 的护栏 Hook（触发流式 onTurnError 通道的最小替身）。 */
    private static final class AlwaysBlockHook implements BuzhouHook {
        @Override
        public HookResult beforeTurn(TurnContext ctx) {
            return new HookResult.Block("护栏拦截：敏感输入");
        }
    }

    private static RuntimeConfig configWith(BuzhouHook hook, SessionObserver... observers) {
        List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer> customizers =
                java.util.Arrays.stream(observers)
                        .map(o -> (io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer)
                                ctx -> ctx.addObserver(o))
                        .toList();
        return new RuntimeConfig(hook == null ? List.of() : List.of(hook),
                java.util.Set.of(), java.util.Set.of(), null, List.of(), Map.of(), List.of(),
                customizers, null);
    }

    /** ① onOpen（构造器尾部）：缺陷观察者炸不掉会话构造，后续健康观察者仍收到 open。 */
    @Test
    void openFailureShouldNotBreakSessionConstruction() {
        RecordingObserver healthy = new RecordingObserver();
        RuntimeConfig config = configWith(null,
                throwingOn("open", () -> { }), healthy);
        try (var agent = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-iso-open")) {
            assertThat(healthy.events).containsExactly("open");
        }
    }

    /** ② onTurnStart：缺陷观察者不杀轮次——chat 返回模型回复，健康观察者 start+end 双收。 */
    @Test
    void turnStartFailureShouldNotKillTurn() {
        RecordingObserver healthy = new RecordingObserver();
        RuntimeConfig config = configWith(null,
                throwingOn("start", () -> { }), healthy);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok-reply");
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-iso-start")) {
            assertThat(agent.chat("正常输入")).isEqualTo("ok-reply");
            assertThat(healthy.events).containsExactly("open", "start:1", "end:1");
        }
    }

    /** ③ onTurnError（流式 guard-block 通道）：缺陷观察者不改写订阅者所见的原始 error，健康观察者仍收 error。 */
    @Test
    void turnErrorFailureShouldNotMaskOriginalStreamError() {
        RecordingObserver healthy = new RecordingObserver();
        RuntimeConfig config = configWith(new AlwaysBlockHook(),
                throwingOn("error", () -> { }), healthy);
        try (var agent = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-iso-err")) {
            assertThatThrownBy(() -> agent.stream("被拦截的输入").blockLast())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("护栏拦截");
            assertThat(healthy.events).containsExactly("open", "start:1", "error:1");
        }
    }

    /** ④ onCancel：缺陷观察者不炸 cancel()，健康观察者仍收 cancel。 */
    @Test
    void cancelFailureShouldNotBreakCancelPath() {
        RecordingObserver healthy = new RecordingObserver();
        RuntimeConfig config = configWith(null,
                throwingOn("cancel", () -> { }), healthy);
        try (var agent = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-iso-cancel")) {
            assertThatCode(agent::cancel).doesNotThrowAnyException();
            assertThat(healthy.events).containsExactly("open", "cancel");
        }
    }
}
