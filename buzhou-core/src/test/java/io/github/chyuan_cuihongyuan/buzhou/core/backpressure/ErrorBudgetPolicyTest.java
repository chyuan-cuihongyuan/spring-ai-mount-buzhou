package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 335 / impl-358：错误预算政策回归——烧穿冻结抬地板 / 单轮清明不解冻
 * （防抖）/ 连续两轮清明解冻 / 冻结解冻计数留痕 / stop 解地板。
 *
 * <p>预算用真实 ErrorBudget 数值驱动（final 类不可继承）：SLO 50%、
 * burn 阈 1.0、minSamples 2、窗 1h（测试期不轮转）——2 败即烧穿
 * （1.0/0.5=2≥1），补 3 成功即清明（2/5/0.5=0.8&lt;1）。
 */
class ErrorBudgetPolicyTest {

    private static ErrorBudget budget() {
        return new ErrorBudget(new ErrorBudget.Config(50, 1.0, 10,
                Duration.ofHours(1), 2), Clock.systemDefaultZone());
    }

    private static void driveBreach(ErrorBudget budget) {
        budget.record("tools", false);
        budget.record("tools", false);
        assertThat(budget.anyBreaching()).isTrue(); // 驱动自证
    }

    private static void driveClear(ErrorBudget budget) {
        for (int i = 0; i < 3; i++) {
            budget.record("tools", true);
        }
        assertThat(budget.anyBreaching()).isFalse();
    }

    @Test
    void breachingFreezesFloor_leavesTrace() {
        ErrorBudget budget = budget();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        List<String> counters = new CopyOnWriteArrayList<>();
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(
                new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics() {
                    @Override
                    public void counter(String name, long delta, String... tagKeyValue) {
                        counters.add(name);
                    }

                    @Override
                    public void timer(String name, Duration duration, String... tagKeyValue) {
                    }
                });
        try {
            ErrorBudgetPolicy policy = new ErrorBudgetPolicy(budget, floor,
                    Duration.ofSeconds(15));
            driveBreach(budget);
            policy.evaluate();
            assertThat(policy.frozen()).isTrue();
            assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
            assertThat(counters).contains("buzhou.errorbudget.freeze");

            policy.evaluate(); // 持续烧穿——不重复计冻结
            assertThat(counters.stream()
                    .filter("buzhou.errorbudget.freeze"::equals).count()).isEqualTo(1);
        } finally {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
        }
    }

    @Test
    void singleClearRoundDoesNotUnfreeze_twoRoundsDo() {
        ErrorBudget budget = budget();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        ErrorBudgetPolicy policy = new ErrorBudgetPolicy(budget, floor, Duration.ofSeconds(15));

        driveBreach(budget);
        policy.evaluate();
        driveClear(budget);
        policy.evaluate(); // 第一轮清明——防抖不解冻
        assertThat(policy.frozen()).isTrue();
        assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
        policy.evaluate(); // 第二轮清明——解冻
        assertThat(policy.frozen()).isFalse();
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);
    }

    @Test
    void breachingResetsClearStreak() {
        ErrorBudget budget = budget();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        ErrorBudgetPolicy policy = new ErrorBudgetPolicy(budget, floor, Duration.ofSeconds(15));

        driveBreach(budget);            // 2/2 → burn 2
        policy.evaluate();              // 冻结
        driveClear(budget);             // 2/5 → burn 0.8
        policy.evaluate();              // 清明 1
        for (int i = 0; i < 4; i++) {
            budget.record("tools", false); // 6/9 → burn 1.33——又烧穿，streak 清零
        }
        policy.evaluate();
        for (int i = 0; i < 6; i++) {
            budget.record("tools", true);  // 6/15 → burn 0.8——清明
        }
        assertThat(budget.anyBreaching()).isFalse();
        policy.evaluate();              // 清明 1（重新计）
        assertThat(policy.frozen()).isTrue();
        policy.evaluate();              // 清明 2
        assertThat(policy.frozen()).isFalse();
    }

    @Test
    void stopLowersFloor_policyLeavesNoRaisedFloorBehind() {
        ErrorBudget budget = budget();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        ErrorBudgetPolicy policy = new ErrorBudgetPolicy(budget, floor, Duration.ofSeconds(15));
        policy.start();
        try {
            driveBreach(budget);
            policy.evaluate();
            assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
        } finally {
            policy.stop();
        }
        assertThat(policy.isRunning()).isFalse();
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW); // 政策离场即解冻
        assertThat(policy.view()).containsKeys("frozen", "floor", "evaluations");
    }

    @Test
    void cleanBudgetNeverFreezes() {
        ErrorBudget budget = budget();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        ErrorBudgetPolicy policy = new ErrorBudgetPolicy(budget, floor, Duration.ofSeconds(15));
        budget.record("tools", true);
        budget.record("tools", true);
        policy.evaluate();
        policy.evaluate();
        assertThat(policy.frozen()).isFalse();
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);
    }
}
