package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 评估项级超时测试（spec 609 / T868–T869 / impl 462，pytest-timeout 借鉴）：
 * 挂死项收敛 error 不断批、run 必完成；默认不设零变化；非正预算拒绝。
 */
class EvalItemTimeoutTest {

    /** 永久挂死的模型（ latch 永不开——模拟 provider 挂死）。 */
    private static final class HangingChatModel extends ScriptedChatModel {
        final CountDownLatch never = new CountDownLatch(1);

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            try {
                never.await(); // 可中断阻塞：超时中断即刻中止
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("挂死项被中断（预期）");
            }
            return super.call(prompt);
        }
    }

    /** 挂死项 + 100ms 预算：该条 error（detail 带预算）、健康项照跑、run 完成。 */
    @Test
    void hungItemTimesOutToErrorWithoutKillingRun() {
        AtomicInteger calls = new AtomicInteger();
        HangingChatModel model = new HangingChatModel();
        ScriptedChatModel fast = new ScriptedChatModel();
        // 第一项挂死、第二项正常：按调用计数分派
        ScriptedChatModel routed = new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                return calls.incrementAndGet() == 1
                        ? model.call(prompt) : fast.call(prompt);
            }
        };
        fast.enqueueText("a2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(routed, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("suite", null);
        datasetStore.addItem("suite", "q1", "a1", null, null); // 挂死项
        datasetStore.addItem("suite", "q2", "a2", null, null); // 健康项

        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setPerItemTimeout(Duration.ofMillis(100));
        EvalRunResult result = runner.run("suite", BuiltInEvaluators.EXACT);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.errored()).isEqualTo(1);
        assertThat(result.passed()).isEqualTo(1);
        assertThat(result.items().get(0).status()).isEqualTo("error");
        assertThat(result.items().get(0).detail()).contains("项超时").contains("预算");
        assertThat(result.items().get(1).status()).isEqualTo("pass");
    }

    /** 并行路径同样生效：两挂死项并行，整跑在预算内完成且双 error。 */
    @Test
    void parallelPathHonorsTimeout() {
        HangingChatModel model = new HangingChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("suite", null);
        datasetStore.addItem("suite", "q1", "a1", null, null);
        datasetStore.addItem("suite", "q2", "a2", null, null);

        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setPerItemTimeout(Duration.ofMillis(150));
        EvalRunResult result = runner.run("suite", BuiltInEvaluators.EXACT, 2);

        assertThat(result.errored()).isEqualTo(2);
        assertThat(result.items()).allSatisfy(item ->
                assertThat(item.detail()).contains("项超时"));
    }

    /** 预算校验：零/负拒绝；null = 关闭（默认零行为变化）。 */
    @Test
    void timeoutValidation() {
        EvalRunner runner = new EvalRunner(null, null, null);
        assertThatThrownBy(() -> runner.setPerItemTimeout(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> runner.setPerItemTimeout(Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        runner.setPerItemTimeout(null); // 关闭合法
    }
}
