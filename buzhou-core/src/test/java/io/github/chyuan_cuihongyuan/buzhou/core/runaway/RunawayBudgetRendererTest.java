package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RunawayBudgetRenderer} 单元测试（spec「软退出通道」）。
 *
 * <p>renderer 在 memory advisor(+400) 运行、步数在 hook(+600) 递增——本步读到「上一步末」计数。
 * 此处直接驱动计数器模拟步数推进，断言注入文本随剩余预算正确变化（每步刷新）。
 */
class RunawayBudgetRendererTest {

    private static BuzhouRunawayProperties props(Integer maxSteps, Double ratio) {
        return new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(maxSteps, null, null),
                null, null, ratio, null, null);
    }

    @Test
    void injectsWhenRemainingBelowThreshold() {
        RunawayCounters counters = new RunawayCounters();
        counters.resetTurn("s");
        // maxSteps=10, ratio=0.2 → 剩余 < 2（即 steps > 8）时注入
        RunawayBudgetRenderer renderer = new RunawayBudgetRenderer(props(10, 0.2), counters);

        // 步数 0~8：剩余 10~2，2/10=0.2 不 < 0.2（边界严格小于），不注入
        for (int s = 0; s <= 8; s++) {
            setSteps(counters, "s", s);
            assertThat(renderer.render("s", 1)).as("steps=%d 不应注入", s).isEmpty();
        }
        // 步数 9：剩余 1，1/10=0.1 < 0.2，注入
        setSteps(counters, "s", 9);
        Optional<String> text = renderer.render("s", 1);
        assertThat(text).isPresent();
        assertThat(text.get()).contains("剩余步数预算：1/10").contains("收尾");
    }

    @Test
    void refreshesEveryStep() {
        RunawayCounters counters = new RunawayCounters();
        counters.resetTurn("s");
        // maxSteps=5, ratio=0.5 → 剩余 < 2.5（即 steps >= 3）时注入
        RunawayBudgetRenderer renderer = new RunawayBudgetRenderer(props(5, 0.5), counters);

        setSteps(counters, "s", 3);
        assertThat(renderer.render("s", 1)).get().asString().contains("剩余步数预算：2/5");
        setSteps(counters, "s", 4);
        assertThat(renderer.render("s", 1)).get().asString().contains("剩余步数预算：1/5");
    }

    @Test
    void noInjectionWithoutStepLimit() {
        RunawayCounters counters = new RunawayCounters();
        counters.resetTurn("s");
        // 无 max-steps：不注入（合法长任务不受影响）
        RunawayBudgetRenderer renderer = new RunawayBudgetRenderer(
                BuzhouRunawayProperties.defaults(), counters);
        setSteps(counters, "s", 100);
        assertThat(renderer.render("s", 1)).isEmpty();
    }

    @Test
    void noInjectionWhenBudgetExhausted() {
        RunawayCounters counters = new RunawayCounters();
        counters.resetTurn("s");
        RunawayBudgetRenderer renderer = new RunawayBudgetRenderer(props(5, 0.5), counters);
        // 剩余 0 或负：不注入
        setSteps(counters, "s", 5);
        assertThat(renderer.render("s", 1)).isEmpty();
    }

    @Test
    void disabledMechanismDoesNotInject() {
        RunawayCounters counters = new RunawayCounters();
        counters.resetTurn("s");
        BuzhouRunawayProperties disabled = new BuzhouRunawayProperties(false,
                new BuzhouRunawayProperties.PerTurn(5, null, null), null, null, 0.1, null, null);
        RunawayBudgetRenderer renderer = new RunawayBudgetRenderer(disabled, counters);
        setSteps(counters, "s", 4);
        assertThat(renderer.render("s", 1)).isEmpty();
    }

    // ---- 辅助：直接设置步数（模拟 hook 递增后的状态）----

    private static void setSteps(RunawayCounters counters, String sid, int target) {
        counters.turnState(sid).steps.set(target);
    }
}
