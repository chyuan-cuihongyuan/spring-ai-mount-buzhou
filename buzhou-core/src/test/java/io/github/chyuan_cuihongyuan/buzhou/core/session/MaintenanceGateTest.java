package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 205 / T578：维护门回归——关放行 / 开拦文案 / end 恢复 / 组合 drain
 * 维护序列 / 参数校验。
 */
class MaintenanceGateTest {

    @Test
    void closedGatePassesEverything() {
        MaintenanceGateHook hook = new MaintenanceGateHook(new MaintenanceGate());
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());

        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "q")))
                .isEqualTo(HookResult.CONTINUE); // 不维护零变化
        assertThat(hook.gate().isActive()).isFalse();
    }

    @Test
    void openGateBlocksWithReadableMessage() {
        MaintenanceGate gate = new MaintenanceGate();
        MaintenanceGateHook hook = new MaintenanceGateHook(gate);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        Instant back = Instant.now().plus(Duration.ofMinutes(30));

        gate.begin("数据库迁移", back);
        HookResult verdict = hook.beforeTurn(new DefaultTurnContext(env, "q"));

        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        String reason = ((HookResult.Block) verdict).reason();
        assertThat(reason).contains("数据库迁移").contains(back.toString()).contains("维护");
    }

    @Test
    void endRestoresTrafficImmediately() {
        MaintenanceGate gate = new MaintenanceGate();
        MaintenanceGateHook hook = new MaintenanceGateHook(gate);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());

        gate.begin("维护", null);
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "q")))
                .isInstanceOf(HookResult.Block.class);

        gate.end();
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "q")))
                .isEqualTo(HookResult.CONTINUE);
        assertThat(gate.window()).isEmpty();
    }

    @Test
    void unknownBackTimeDegradesGracefully() {
        MaintenanceGate gate = new MaintenanceGate();
        gate.begin("紧急修理", null); // 恢复时间未知

        assertThat(gate.refusalMessage()).contains("预计尽快恢复");
    }

    @Test
    void composesWithDrainCoordinatorAsMaintenanceSequence() throws Exception {
        MaintenanceGate gate = new MaintenanceGate();
        SessionDrainCoordinator drain = new SessionDrainCoordinator();
        SessionDrainCoordinator.Lease inFlight = drain.enter("s1");

        gate.begin("存储迁移", Instant.now().plus(Duration.ofMinutes(10)));
        drain.beginDrain("s1"); // 维护序列：拒新 + 排存量

        assertThat(drain.isDraining("s1")).isTrue();
        assertThat(drain.awaitDrained("s1", Duration.ofMillis(10))).isFalse(); // 存量在飞

        inFlight.close();
        assertThat(drain.awaitDrained("s1", Duration.ofSeconds(2))).isTrue(); // 排空可维护

        gate.end();
        assertThat(gate.isActive()).isFalse(); // 维护完成恢复
    }

    @Test
    void beginValidatesReason() {
        MaintenanceGate gate = new MaintenanceGate();
        assertThatThrownBy(() -> gate.begin(" ", null))
                .isInstanceOf(IllegalArgumentException.class);
        gate.begin("合法", null);
        gate.end();
        gate.end(); // 幂等
        assertThat(gate.isActive()).isFalse();
    }
}
