package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetGateStatsTest {

    private static ModelCallContext modelCtx() {
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        return new ModelCallContext() {
            @Override
            public String sessionId() {
                return env.sessionId();
            }

            @Override
            public String agentName() {
                return env.agentName();
            }

            @Override
            public int turn() {
                return env.currentTurn();
            }

            @Override
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
                return env.stateHandle();
            }

            @Override
            public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) {
                env.emit(event);
            }

            @Override
            public org.springframework.ai.chat.client.ChatClientRequest request() {
                return null;
            }

            @Override
            public org.springframework.ai.chat.client.ChatClientResponse response() {
                return null;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public void replaceRequest(org.springframework.ai.chat.client.ChatClientRequest newRequest) {
            }

            @Override
            public void replaceResponse(org.springframework.ai.chat.client.ChatClientResponse newResponse) {
            }
        };
    }

    @Test
    void freshGateHasZeroCounts() {
        ModelBudgetGate gate = new ModelBudgetGate(Map.of("m", 1_000L), "m",
                ModelCostLedger.create());

        assertThat(gate.stats()).isEqualTo(new ModelBudgetGate.BudgetGateStats(0, 0, 0));
    }

    @Test
    void withinBudgetCountsAllowed() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ModelBudgetGate gate = new ModelBudgetGate(Map.of("m", 1_000L), "m", ledger);
        ledger.record("m", 500);

        assertThat(gate.beforeModel(modelCtx())).isSameAs(HookResult.CONTINUE);
        assertThat(gate.stats()).isEqualTo(new ModelBudgetGate.BudgetGateStats(1, 1, 0));
    }

    @Test
    void exhaustedBudgetCountsBlocked() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ModelBudgetGate gate = new ModelBudgetGate(Map.of("m", 1_000L), "m", ledger);
        ledger.record("m", 1_200);

        HookResult result = gate.beforeModel(modelCtx());

        assertThat(result).isNotSameAs(HookResult.CONTINUE);
        ModelBudgetGate.BudgetGateStats stats = gate.stats();
        assertThat(stats.checks()).isEqualTo(1);
        assertThat(stats.allowed()).isZero();
        assertThat(stats.blocked()).isEqualTo(1);
    }

    @Test
    void undeclaredModelAlwaysAllowedAndCounted() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ModelBudgetGate gate = new ModelBudgetGate(Map.of("m", 1_000L), "m", ledger);

        assertThat(gate.beforeModel(modelCtx())).isSameAs(HookResult.CONTINUE);
        assertThat(gate.stats().allowed()).isEqualTo(1);
    }

    @Test
    void conservationHoldsAcrossMixedCalls() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ModelBudgetGate gate = new ModelBudgetGate(Map.of("m", 1_000L), "m", ledger);
        ledger.record("m", 1_500);

        gate.beforeModel(modelCtx());
        gate.beforeModel(modelCtx());
        gate.beforeModel(modelCtx());

        ModelBudgetGate.BudgetGateStats stats = gate.stats();
        assertThat(stats.checks()).isEqualTo(3);
        assertThat(stats.allowed() + stats.blocked()).isEqualTo(stats.checks());
    }
}
