package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 322 / impl-345：工具混沌注入回归——确定性概率源：延迟耗时可测/故障
 * block 结构化标记/零概率不袭/include 外不袭/运行时开关/计数/构造校验。
 */
class ChaosMonkeyHookTest {

    private static final HookEnvironment ENV =
            new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    /** 恒命中概率源（0.0×100=0 < 任何正 percent）。 */
    private static final DoubleSupplier ALWAYS = () -> 0.0;

    /** 恒不命中概率源（0.99×100=99 ≥ percent≤99 才不命中……用 100 档必须 0 命中测试显式 0%）。 */
    private static final DoubleSupplier NEVER = () -> 0.999999;

    private static DefaultToolCallContext ctx(String tool) {
        return new DefaultToolCallContext(ENV, "tc-" + tool, tool, Map.of());
    }

    @Test
    void latencyInjectionSleepsThenContinues() {
        ChaosMonkeyHook hook = new ChaosMonkeyHook(100, 60, 100, Set.of(), true, ALWAYS);
        long start = System.nanoTime();
        HookResult result = hook.beforeTool(ctx("slow"));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertThat(result).isEqualTo(HookResult.CONTINUE); // 慢完照常执行
        assertThat(elapsedMillis).as("延迟注入 ≥ 60ms").isGreaterThanOrEqualTo(60);
        assertThat(hook.latencyInjectedCount()).isEqualTo(1);
        assertThat(hook.faultInjectedCount())
                .as("一次调用至多一种袭击——延迟命中后不再掷故障").isZero();
    }

    @Test
    void faultInjectionBlocksWithStructuredMarker() {
        ChaosMonkeyHook hook = new ChaosMonkeyHook(0, 0, 100, Set.of(), true, ALWAYS);
        HookResult result = hook.beforeTool(ctx("flaky"));
        assertThat(result).isInstanceOf(HookResult.Block.class);
        String reason = ((HookResult.Block) result).reason();
        assertThat(ToolFeedbackType.isErrorFeedback(reason))
                .as("131/321 同标记语义——熔断/错误预算把它当真故障").isTrue();
        assertThat(reason).contains("混沌注入");
        assertThat(hook.faultInjectedCount()).isEqualTo(1);
    }

    @Test
    void zeroPercentNeverAssaults() {
        ChaosMonkeyHook hook = new ChaosMonkeyHook(0, 100, 0, Set.of(), true, ALWAYS);
        assertThat(hook.beforeTool(ctx("any"))).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.latencyInjectedCount()).isZero();
        assertThat(hook.faultInjectedCount()).isZero();
    }

    @Test
    void percentGateMissesWhenRandomHigh() {
        ChaosMonkeyHook hook = new ChaosMonkeyHook(50, 10, 50, Set.of(), true, NEVER);
        assertThat(hook.beforeTool(ctx("any")))
                .as("0.99×100=99 ≥ 50——两档都不命中").isEqualTo(HookResult.CONTINUE);
        assertThat(hook.latencyInjectedCount()).isZero();
        assertThat(hook.faultInjectedCount()).isZero();
    }

    @Test
    void includeListSkipsOtherTools() {
        ChaosMonkeyHook hook = new ChaosMonkeyHook(100, 1, 100, Set.of("victim"), true, ALWAYS);
        assertThat(hook.beforeTool(ctx("innocent"))).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.latencyInjectedCount()).isZero();
        assertThat(hook.beforeTool(ctx("victim"))).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.latencyInjectedCount()).isEqualTo(1); // 清单内才袭
    }

    @Test
    void runtimeToggleDisablesAndEnables() {
        ChaosMonkeyHook hook = new ChaosMonkeyHook(0, 0, 100, Set.of(), false, ALWAYS);
        assertThat(hook.beforeTool(ctx("any"))).isEqualTo(HookResult.CONTINUE);
        hook.setEnabled(true); // 演练窗口开
        assertThat(hook.beforeTool(ctx("any"))).isInstanceOf(HookResult.Block.class);
    }

    @Test
    void constructorValidates() {
        assertThatThrownBy(() -> new ChaosMonkeyHook(101, 1, 0, Set.of(), true, ALWAYS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChaosMonkeyHook(0, 1, 101, Set.of(), true, ALWAYS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChaosMonkeyHook(0, -1, 0, Set.of(), true, ALWAYS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChaosMonkeyHook(0, 1, 0, Set.of(), true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void afterToolNeverTouchesResult() {
        ChaosMonkeyHook hook = new ChaosMonkeyHook(100, 1, 100, Set.of(), true, ALWAYS);
        DefaultToolCallContext executed = ctx("any");
        executed.markExecuted("真实结果", null);
        assertThat(hook.afterTool(executed)).isEqualTo(HookResult.CONTINUE);
        assertThat(executed.result())
                .as("混沌不篡改真实结果（结果篡改另立项）").isEqualTo("真实结果");
    }
}
