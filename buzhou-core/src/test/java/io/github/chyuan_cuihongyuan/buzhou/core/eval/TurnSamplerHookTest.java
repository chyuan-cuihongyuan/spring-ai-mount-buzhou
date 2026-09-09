package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 407 §Testing / T705–T706：在线采样——率边界/确定性；空白与短问过滤；
 * fail-soft 未建集不炸轮 + 预建后入集含 provenance；yml 装配默认关。
 */
class TurnSamplerHookTest {

    private static HookEnvironment env(String sessionId) {
        return new HookEnvironment(sessionId, "agent", new InMemorySessionStateStore());
    }

    private static DefaultTurnContext turn(String sessionId, String input, String response) {
        // HookEnvironment 的 turn 计数由会话推进——测试用固定 1（确定性采样只依赖键）
        return new DefaultTurnContext(env(sessionId), input) {
            @Override
            public String response() {
                return response;
            }
        };
    }

    @Test
    void shouldSampleAllAtFullRate_noneAtZero_andStayDeterministic() {
        SessionStateStore state = new InMemorySessionStateStore();
        EvalDatasetStore store = new EvalDatasetStore(state);
        store.createDataset("prod-samples", "生产采样");

        TurnSamplerHook full = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy("prod-samples", 100, 0));
        full.afterTurn(turn("s1", "问题一", "回答一"));
        full.afterTurn(turn("s2", "问题二", "回答二"));
        assertThat(store.items("prod-samples")).hasSize(2);

        TurnSamplerHook zero = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy("prod-samples", 0, 0));
        zero.afterTurn(turn("s3", "问题三", "回答三"));
        assertThat(store.items("prod-samples")).hasSize(2);

        // 中间率确定性：同键同判（重复调用决定不变）
        TurnSamplerHook mid = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy("prod-samples", 50, 0));
        for (int t = 1; t <= 20; t++) {
            boolean first = mid.sampled("det", t);
            for (int repeat = 0; repeat < 3; repeat++) {
                assertThat(mid.sampled("det", t)).as("turn=" + t).isEqualTo(first);
            }
        }
    }

    @Test
    void shouldSkipBlankAndShortInput() {
        SessionStateStore state = new InMemorySessionStateStore();
        EvalDatasetStore store = new EvalDatasetStore(state);
        store.createDataset("d", "x");
        TurnSamplerHook hook = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy("d", 100, 10));

        hook.afterTurn(turn("s", "  ", "回答"));      // 空问
        hook.afterTurn(turn("s", "问题", null));       // 空答
        hook.afterTurn(turn("s", "短", "回答"));       // 短问（<10）
        assertThat(store.items("d")).isEmpty();

        hook.afterTurn(turn("s", "这是一个足够长的问题", "回答"));
        assertThat(store.items("d")).hasSize(1);
        assertThat(store.items("d").get(0).input()).isEqualTo("这是一个足够长的问题");
    }

    @Test
    void shouldFailSoftWhenDatasetMissing_andRecordProvenanceWhenPresent() {
        SessionStateStore state = new InMemorySessionStateStore();
        EvalDatasetStore store = new EvalDatasetStore(state);
        // 未建集：fail-soft 轮不炸（无异常冒出即验证）
        TurnSamplerHook hook = new TurnSamplerHook(store,
                new TurnSamplerHook.Policy("missing", 100, 0));
        hook.afterTurn(turn("s", "正常长度的问题", "回答"));

        store.createDataset("built", "x");
        new TurnSamplerHook(store, new TurnSamplerHook.Policy("built", 100, 0))
                .afterTurn(turn("prov-session", "另一个正常长度的问题", "回答二"));
        var items = store.items("built");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).sourceSessionId()).isEqualTo("prov-session");
    }

    @Test
    void shouldAssembleFromYml_onlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.eval.sampling.enabled=true",
                        "buzhou.eval.sampling.dataset=prod-samples",
                        "buzhou.eval.sampling.rate-percent=5")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouEvalSamplingRuntimeConfig");
                    assertThat(context).hasBean("buzhouEvalDatasetStore");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouEvalSamplingRuntimeConfig");
                });
    }
}
