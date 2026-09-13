package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 748 / T1098–T1099：执行策略汇总读数——默认全关形态/逐项设置回显。
 */
class EvalRunnerPolicyReadoutTest {

    @Test
    void policyReflectsCurrentConfiguration() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        AgentRuntime runtime = Buzhou.runtime(
                new ScriptedChatModel() {}, stores, RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());

        // 默认全关
        var policy = runner.executionPolicy();
        assertThat(policy.get("runBudgetChars")).isEqualTo(0L);
        assertThat(policy.get("errorRetryOnce")).isEqualTo(false);
        assertThat(policy.get("perItemTimeoutMs")).isNull();
        assertThat(policy.get("memoizationKey")).isNull();
        assertThat(policy.get("driftWindow")).isEqualTo(0);

        // 逐项设置后回显
        runner.setRunBudgetChars(1000);
        runner.setErrorRetryOnce(true);
        runner.setPerItemTimeout(Duration.ofSeconds(30));
        runner.setMemoizationKey("judge-v1");
        runner.setDriftBaseline(5, 0.1);
        policy = runner.executionPolicy();
        assertThat(policy.get("runBudgetChars")).isEqualTo(1000L);
        assertThat(policy.get("errorRetryOnce")).isEqualTo(true);
        assertThat(policy.get("perItemTimeoutMs")).isEqualTo(30_000L);
        assertThat(policy.get("memoizationKey")).isEqualTo("judge-v1");
        assertThat(policy.get("driftWindow")).isEqualTo(5);
        assertThat(policy.get("driftWarnShift")).isEqualTo(0.1);
    }
}
