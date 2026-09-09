package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalDatasetStore;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.TurnErrorSampler;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 427 §Testing / T745–T746：非流式轮次错误回调对称化——enqueueThrow
 * 非流式 chat → 观察者 onTurnError 收原异常、onTurnEnd 不发生、后续成功轮
 * 照常；TurnErrorSampler（423 扩散）非流式 E2E 错误轮入集。
 */
class NonStreamTurnErrorCallbackTest {

    private static final class RecordingObserver implements SessionObserver {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<Throwable> errors = new CopyOnWriteArrayList<>();

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
            errors.add(error);
        }
    }

    @Test
    void shouldCallbackObserversOnNonStreamTurnError() {
        RecordingObserver observer = new RecordingObserver();
        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.addObserver(observer)), null);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(new IllegalStateException("非流式模型故障"));
        model.enqueueText("恢复后的回复");
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-cb")) {
            assertThatThrownBy(() -> agent.chat("会失败的问题")).isInstanceOf(Exception.class);

            // 失败轮：start 后 error 收原异常、无 end
            assertThat(observer.events).containsExactly("start:1", "error:1");
            assertThat(observer.errors.get(0)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("非流式模型故障");

            // 同会话后续成功轮照常（观察者状态未被破坏）
            assertThat(agent.chat("恢复问题")).isEqualTo("恢复后的回复");
            assertThat(observer.events).containsExactly("start:1", "error:1", "start:2", "end:2");
        }
    }

    @Test
    void shouldSampleNonStreamErrorTurn_turnErrorSampler() {
        EvalDatasetStore store = new EvalDatasetStore(new InMemorySessionStateStore());
        store.createDataset("err-pool", "错误池");
        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.addObserver(new TurnErrorSampler(store,
                        new TurnErrorSampler.Policy("err-pool", 100, 0), ctx.sessionId()))),
                null);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(new IllegalStateException("非流式故障"));
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-samp")) {
            assertThatThrownBy(() -> agent.chat("非流式失败问题")).isInstanceOf(Exception.class);
        }
        // 423 扩散兑现：非流式错误轮也进候选池（R24 时是流式专属）
        assertThat(store.items("err-pool")).hasSize(1);
        assertThat(store.items("err-pool").get(0).expected())
                .startsWith("[TURN-ERROR] IllegalStateException");
    }
}
