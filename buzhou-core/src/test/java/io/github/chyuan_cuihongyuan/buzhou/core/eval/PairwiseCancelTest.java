package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1506 / T2263–T2264：A/B 对比 run 宿主取消（spec 1505 扩散）——第 2 项
 * judge 触发 requestCancel() 后剩余项进 skipped 不再执行、summary.hostCancelled
 * 区分宿主叫停与 SPRT 达界停、残留标记不污染下一次 compare。
 */
class PairwiseCancelTest {

    private static AgentRuntime runtime() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("reply");
        return Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults());
    }

    /** 第 2 项触发取消：前 2 项有裁决、剩余进 skipped、hostCancelled=true；残留清零。 */
    @Test
    void cancelAtSecondItemShouldSkipRemainderWithHostCancelledFlag() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ds-ab-cancel", null);
        for (int i = 0; i < 5; i++) {
            ds.addItem("ds-ab-cancel", "问题" + i, "ok", null, null);
        }
        PairwiseEvalRunner[] holder = new PairwiseEvalRunner[1];
        AtomicInteger judgeCalls = new AtomicInteger();
        ChatModel judgeModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                // PairwiseJudge 双向裁定：每项调 judge 2 次——恰第 4 次（第 2 项收尾）
                // 触发；用 == 而非 >= 保证第二次 compare 不因计数残留重复触发
                if (judgeCalls.incrementAndGet() == 4) {
                    holder[0].requestCancel();
                }
                return new ChatResponse(List.of(new Generation(new AssistantMessage(
                        "WINNER_A 内容更完整"))));
            }
        };
        holder[0] = new PairwiseEvalRunner(ds, new PairwiseJudge(judgeModel));
        PairwiseEvalRunner runner = holder[0];

        PairwiseEvalRunner.PairwiseEvalResult result = runner.compare(
                "ds-ab-cancel", runtime(), runtime(), 1);

        assertThat(judgeCalls.get()).isEqualTo(4); // 未起项不再执行（judge 不被触达）
        assertThat(result.items()).hasSize(5);
        assertThat(result.items().get(0)).isNotNull();
        assertThat(result.items().get(1)).isNotNull();
        assertThat(result.items().subList(2, 5)).containsOnlyNulls();
        assertThat(result.summary().skipped()).isEqualTo(3);
        // spec 1535 / T2321：进度读面——终态快照 done=5（含 3 skipped 占位）total=5
        assertThat(runner.progress().total()).isEqualTo(5);
        assertThat(runner.progress().done()).isEqualTo(5);
        assertThat(runner.progress().hostCancelled()).isTrue();
        assertThat(result.summary().hostCancelled()).isTrue();

        // 残留清零：同一 runner 的下一次 compare 完整执行、hostCancelled=false
        // （judge 触发条件为 == 4，第二次 run 计数越过不再触发）
        PairwiseEvalRunner.PairwiseEvalResult second = runner.compare(
                "ds-ab-cancel", runtime(), runtime(), 1);
        assertThat(second.items()).allMatch(java.util.Objects::nonNull);
        assertThat(second.summary().hostCancelled()).isFalse();
        assertThat(second.summary().skipped()).isZero();
    }
}
