package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1449 兄弟票 / T2200：采样漏斗静态读数——empty/short/rate/written 四桶
 * （ratePercent=100 全采、0% 不进漏斗）、写失败 fail-soft 分桶、reset 归零。
 */
class TurnSamplerHookStatsTest {

    private static final String DATASET = "ds-funnel";
    private EvalDatasetStore store;

    private static DefaultTurnContext turn(String sessionId, String input, String response) {
        var ctx = new DefaultTurnContext(new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment(
                sessionId, "ag", new InMemorySessionStateStore()), input);
        ctx.markResponded(response);
        return ctx;
    }

    @BeforeEach
    void reset() {
        TurnSamplerHook.resetSamplerStatsForTest();
        store = new EvalDatasetStore(new InMemorySessionStateStore());
        store.createDataset(DATASET, "采样池");
    }

    @AfterEach
    void resetAfter() {
        TurnSamplerHook.resetSamplerStatsForTest();
    }

    @Test
    void funnelBucketsAtHundredPercent() {
        TurnSamplerHook hook = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy(DATASET, 100, 5));
        hook.afterTurn(turn("s-f", "这是一个足够长的合法输入", "合法回答一"));
        hook.afterTurn(turn("s-f", "短问", "回答")); // 短问过滤
        hook.afterTurn(turn("s-f", "", "缺问"));      // 空输入跳过
        var s = TurnSamplerHook.samplerStats();
        assertThat(s.turnsSeen()).isEqualTo(2); // 空输入不进漏斗
        assertThat(s.emptySkipped()).isEqualTo(1);
        assertThat(s.shortSkipped()).isEqualTo(1);
        assertThat(s.rateSkipped()).isZero();
        assertThat(s.written()).isEqualTo(1);
        assertThat(store.items(DATASET)).hasSize(1);
    }

    @Test
    void zeroRateSkipsAllAtRateGate() {
        TurnSamplerHook hook = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy(DATASET, 0, 5));
        hook.afterTurn(turn("s-f0", "这是一个足够长的合法输入", "合法回答"));
        var s = TurnSamplerHook.samplerStats();
        // ratePercent=0 早退——不入漏斗（既有语义：关闭即零开销）
        assertThat(s.turnsSeen()).isZero();
        assertThat(store.items(DATASET)).isEmpty();
    }

    @Test
    void resetForTestClearsFunnel() {
        TurnSamplerHook hook = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy(DATASET, 100, 5));
        hook.afterTurn(turn("s-fr", "这是一个足够长的合法输入", "回答"));
        assertThat(TurnSamplerHook.samplerStats().written()).isEqualTo(1);
        TurnSamplerHook.resetSamplerStatsForTest();
        assertThat(TurnSamplerHook.samplerStats().written()).isZero();
    }
}
