package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 530 / T811：per-model 预算闸——记账达预算 block（告示含模型名/
 * 已记账/预算/修法）、未达预算放行、未声明模型放行、空表/非正预算
 * fail-fast、yml 装配缺席。
 */
class ModelBudgetGateTest {

    private static ModelCallContext ctx() {
        return new ModelCallContext() {
            @Override public String sessionId() { return "s"; }
            @Override public String agentName() { return "a"; }
            @Override public int turn() { return 1; }
            @Override public SessionStateHandle state() {
                throw new UnsupportedOperationException();
            }
            @Override public void emitEvent(SessionEvent event) { }
            @Override public ChatClientRequest request() {
                return new ChatClientRequest(new Prompt(java.util.List.of()), Map.of());
            }
            @Override public ChatClientResponse response() { return null; }
            @Override public Throwable error() { return null; }
            @Override public void replaceRequest(ChatClientRequest newRequest) { }
            @Override public void replaceResponse(ChatClientResponse newResponse) { }
        };
    }

    @Test
    void exhaustedModelIsBlockedWithStructuredNotice() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-4o", 9_000);
        ledger.record("gpt-4o", 500);
        ModelBudgetGate gate = new ModelBudgetGate(
                Map.of("gpt-4o", 9_000L), "gpt-4o", ledger);

        HookResult result = gate.beforeModel(ctx());
        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) result).reason())
                .contains("gpt-4o").contains("9,500".replace(",", ""))
                .contains("9,000".replace(",", ""))
                .contains("model-budget");
    }

    @Test
    void underBudgetAndUndeclaredModelsPassThrough() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-4o", 5_000);
        ModelBudgetGate gate = new ModelBudgetGate(
                Map.of("gpt-4o", 9_000L), "gpt-4o", ledger);
        assertThat(gate.beforeModel(ctx())).isEqualTo(HookResult.CONTINUE);
        // 未声明模型（ledger 有账但无预算）放行
        ModelBudgetGate narrow = new ModelBudgetGate(
                Map.of("other", 1L), "gpt-4o", ledger);
        assertThat(narrow.beforeModel(ctx())).isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void invalidBudgetsFailFast() {
        assertThatThrownBy(() -> new ModelBudgetGate(
                Map.of(), "m", ModelCostLedger.create()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModelBudgetGate(
                Map.of("m", 0L), "m", ModelCostLedger.create()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ymlAssemblyOnlyWhenBudgetsDeclared() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        () -> io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.budget.model-budget.gpt-4o=1000000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouModelBudgetRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        () -> io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouModelBudgetRuntimeConfig");
                });
    }
}
