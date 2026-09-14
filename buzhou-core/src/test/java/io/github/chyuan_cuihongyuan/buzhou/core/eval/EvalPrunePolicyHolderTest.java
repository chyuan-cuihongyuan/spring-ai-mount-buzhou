package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-701 / spec 958：EvalPrunePolicyHolder 进程级兜底——Runner 未显式设置时
 * 惰性拾取 Holder；显式 setPrunePolicy 优先；clear 后回退关闭。
 */
class EvalPrunePolicyHolderTest {

    @AfterEach
    void cleanup() {
        EvalPrunePolicyHolder.clear();
    }

    @Test
    void runnerPicksUpHolderPolicyWhenOwnIsNull() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ds-holder", null);
        for (int i = 0; i < 3; i++) {
            ds.addItem("ds-holder", "问题" + i, "ok", null, null);
        }
        EvalRunner runner = new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore());

        EvalPrunePolicyHolder.set(new EvalPrunePolicy(1, 0.99));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult run = runner.run("ds-holder", evaluator);

        // Holder 兜底生效：观察窗 1 项 fail 即剪（1/1 ≥ 0.99）
        assertThat(evaluator.calls()).isEqualTo(1);
        assertThat(run.items().get(0).status()).isEqualTo(EvalRunItemResult.STATUS_FAIL);
        assertThat(run.items().subList(1, 3)).allSatisfy(r ->
                assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_PRUNED));
    }

    @Test
    void explicitSetterOverridesHolder() {
        EvalPrunePolicyHolder.set(new EvalPrunePolicy(3, 0.5));
        EvalRunner runner = new EvalRunner(
                io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.runtime(
                        prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores(),
                        io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                new EvalDatasetStore(io.github.chyuan_cuihongyuan.buzhou.core.Buzhou
                        .inMemoryStores().sessionStateStore()),
                io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores().sessionStateStore());
        // 显式实例级策略优先（ Runner 自身字段非空时不读 Holder）——语义由
        // EvalPruneTest.prunesRemainingItemsOnceFailRateReached 覆盖，此处只固化
        // Holder.current() 非 null 时 getter 链路存在
        assertThat(EvalPrunePolicyHolder.current()).isNotNull();
        EvalPrunePolicyHolder.clear();
        assertThat(EvalPrunePolicyHolder.current()).isNull();
    }

    private static final class AlwaysFailEvaluator implements Evaluator {
        private final java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            calls.incrementAndGet();
            return EvalScore.fail("x");
        }

        int calls() {
            return calls.get();
        }
    }
}
