package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnConcurrencyTracker;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1400 / T2101–T2102：guard-block 轮观察者收口（实证缺陷修复）——
 * beforeTurn Block 的轮此前派发 onTurnStart 后无任何终结回调（TURN span 泄漏）；
 * 修复后非流式补 onTurnEnd（reason 即最终回复）、流式补 onTurnError（与订阅者
 * 所见 error 对称）；TurnConcurrencyTracker 守恒式端到端成立。
 */
class GuardBlockObserverClosureTest {

    /** beforeTurn 无条件 Block 的护栏 Hook（guard 拒绝语义的最小替身）。 */
    private static final class AlwaysBlockHook implements BuzhouHook {
        @Override
        public HookResult beforeTurn(TurnContext ctx) {
            return new HookResult.Block("护栏拦截：敏感输入");
        }
    }

    private static final class RecordingObserver implements SessionObserver {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<String> replies = new CopyOnWriteArrayList<>();
        final List<Throwable> errors = new CopyOnWriteArrayList<>();

        @Override
        public void onTurnStart(int turnSeq, String userInput) {
            events.add("start:" + turnSeq);
        }

        @Override
        public void onTurnEnd(int turnSeq, String finalReply) {
            events.add("end:" + turnSeq);
            replies.add(finalReply);
        }

        @Override
        public void onTurnError(int turnSeq, Throwable error) {
            events.add("error:" + turnSeq);
            errors.add(error);
        }
    }

    @Test
    void nonStreamGuardBlockShouldFireOnTurnEndWithReason() {
        RecordingObserver observer = new RecordingObserver();
        RuntimeConfig config = new RuntimeConfig(List.of(new AlwaysBlockHook()),
                java.util.Set.of(), java.util.Set.of(), null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.addObserver(observer)), null);
        ScriptedChatModel model = new ScriptedChatModel();
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-gb-ns")) {
            String reply = agent.chat("被拦截的输入");
            // 护栏拒绝语义不变：reason 文本原样作为回复
            assertThat(reply).isEqualTo("护栏拦截：敏感输入");
            // 修复点：start 后补派 end（此前泄漏），reason 即 finalReply
            assertThat(observer.events).containsExactly("start:1", "end:1");
            assertThat(observer.replies).containsExactly("护栏拦截：敏感输入");
            // 模型未被触达（guard 先于模型调用拦截）
            assertThat(model.seenPrompts).isEmpty();
        }
    }

    @Test
    void streamGuardBlockShouldFireOnTurnError() {
        RecordingObserver observer = new RecordingObserver();
        RuntimeConfig config = new RuntimeConfig(List.of(new AlwaysBlockHook()),
                java.util.Set.of(), java.util.Set.of(), null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.addObserver(observer)), null);
        ScriptedChatModel model = new ScriptedChatModel();
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-gb-st")) {
            assertThatThrownBy(() -> agent.stream("被拦截的输入").blockLast())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("护栏拦截");
            // 修复点：start 后补派 error（与订阅者所见 error 终结对称，此前泄漏）
            assertThat(observer.events).containsExactly("start:1", "error:1");
            assertThat(observer.errors.get(0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("护栏拦截");
            assertThat(model.seenPrompts).isEmpty();
        }
    }

    @Test
    void trackerConservationHoldsAcrossGuardBlockTurns() {
        TurnConcurrencyTracker tracker = new TurnConcurrencyTracker();
        RecordingObserver observer = new RecordingObserver();
        List<SessionAssemblyCustomizer> customizers = List.of(
                ctx -> ctx.addObserver(observer),
                ctx -> ctx.addObserver(tracker));
        RuntimeConfig config = new RuntimeConfig(List.of(new AlwaysBlockHook()),
                java.util.Set.of(), java.util.Set.of(), null, List.of(), Map.of(), List.of(),
                customizers, null);
        ScriptedChatModel model = new ScriptedChatModel();
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-gb-cc")) {
            // guard-block 轮（非流式）：终结落 okFinished 桶，守恒式闭合
            assertThat(agent.chat("拦截输入")).isEqualTo("护栏拦截：敏感输入");
            TurnConcurrencyTracker.Snapshot s = tracker.stats();
            assertThat(s.started()).isEqualTo(1);
            assertThat(s.okFinished()).isEqualTo(1);
            assertThat(s.active()).isZero();
            assertThat(s.started()).isEqualTo(s.okFinished() + s.failed() + s.active());
        }
    }
}
