package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 危险工具 HITL 豁免征询测试（spec 1624 / T2399–T2400 / impl 1177）：
 * GuardExemptionRegistry 首个消费者——未过期豁免放行 + 审计事件；
 * 过期/撤销/无豁免恢复确认流程（block 语义零变化）。
 */
class DangerousToolExemptionTest {

    private static ToolCallContext toolCall(String tool) {
        return new ToolCallContext() {
            @Override
            public String sessionId() {
                return "s1";
            }

            @Override
            public String agentName() {
                return "agent";
            }

            @Override
            public int turn() {
                return 1;
            }

            @Override
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
                return null;
            }

            @Override
            public void emitEvent(SessionEvent event) {
            }

            @Override
            public String toolName() {
                return tool;
            }

            @Override
            public String toolCallId() {
                return "tc1";
            }

            @Override
            public Map<String, Object> arguments() {
                return Map.of();
            }

            @Override
            public Object result() {
                return null;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public void replaceArguments(Map<String, Object> newArguments) {
            }

            @Override
            public void replaceResult(Object newResult) {
            }
        };
    }

    private static io.github.chyuan_cuihongyuan.buzhou.guard.hook.DangerousToolGuardHook hookWith(
            GuardExemptionRegistry exemptions) {
        io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolConfig config =
                new io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolConfig(
                        true, io.github.chyuan_cuihongyuan.buzhou.guard.config.AuthTtl.ONCE,
                        List.of(new io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolEntry(
                                "run_command", "confirm_run_command", "即将执行命令", null)));
        SessionStateStore store = Buzhou.inMemoryStores().sessionStateStore();
        return new io.github.chyuan_cuihongyuan.buzhou.guard.hook.DangerousToolGuardHook(
                config, store, exemptions);
    }

    @Test
    void activeExemptionAdmitsWithoutConfirmation() {
        GuardExemptionRegistry exemptions = new GuardExemptionRegistry();
        exemptions.grant("dangerous-tool", "run_command",
                System.currentTimeMillis() + 60_000, "已人工核验");
        var hook = hookWith(exemptions);
        assertThat(hook.beforeTool(toolCall("run_command")))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Continue.class);
    }

    @Test
    void noOrExpiredExemptionStillBlocks() {
        var hook = hookWith(new GuardExemptionRegistry());
        assertThat(hook.beforeTool(toolCall("run_command")))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Block.class);

        GuardExemptionRegistry expired = new GuardExemptionRegistry();
        expired.grant("dangerous-tool", "run_command",
                System.currentTimeMillis() - 1_000, "过期豁免");
        var hook2 = hookWith(expired);
        assertThat(hook2.beforeTool(toolCall("run_command")))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Block.class);
    }

    @Test
    void revocationRestoresConfirmation() {
        GuardExemptionRegistry exemptions = new GuardExemptionRegistry();
        exemptions.grant("dangerous-tool", "run_command",
                System.currentTimeMillis() + 60_000, "临时");
        exemptions.revoke("dangerous-tool", "run_command");
        var hook = hookWith(exemptions);
        assertThat(hook.beforeTool(toolCall("run_command")))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Block.class);
    }

    @Test
    void moduleExposesRegistry() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).build();
        assertThat(module.exemptions()).isNotNull();
        assertThat(module.exemptions().snapshot(System.currentTimeMillis()).active()).isEmpty();
    }
}
