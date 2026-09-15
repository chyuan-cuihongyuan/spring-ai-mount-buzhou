package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1820 / T2842：重启错峰——id 稳定槽、界内延迟、碰撞账。 */
class RestartSpreadPlanTest {

    /** 同 id 永远同延迟（确定性——无随机数可回放）。 */
    @Test
    void delayIsStablePerId() {
        long first = RestartSpreadPlan.delayFor("instance-a", 10, 1000L);
        long second = RestartSpreadPlan.delayFor("instance-a", 10, 1000L);
        assertThat(first).isEqualTo(second);
        assertThat(first).isGreaterThanOrEqualTo(0L).isLessThan(1000L);
    }

    /** 槽位穷尽性：cohort 大于等于实例数时，延迟多样性可观（非全同槽）。 */
    @Test
    void cohortSpreadsAcrossSlots() {
        RestartSpreadPlan.CohortReport report = RestartSpreadPlan.cohort(16, 1600L,
                List.of("i1", "i2", "i3", "i4", "i5", "i6", "i7", "i8"));
        assertThat(report.instances()).isEqualTo(8);
        assertThat(report.maxDelay()).isLessThan(1600L);
        // 8 实例 16 槽：全撞同槽的概率可忽略，至少 4 个不同延迟
        assertThat(report.delays().values().stream().distinct().count())
                .isGreaterThanOrEqualTo(4);
    }

    /** 碰撞账：实例数 > 槽位数必有碰撞（鸽笼），读数诚实。 */
    @Test
    void collisionsAccountedWhenPigeonhole() {
        RestartSpreadPlan.CohortReport report = RestartSpreadPlan.cohort(2, 200L,
                List.of("a", "b", "c", "d", "e"));
        assertThat(report.instances()).isEqualTo(5);
        // 5 实例 2 槽：至少 3 次碰撞
        assertThat(report.collisions()).isGreaterThanOrEqualTo(3);
        assertThat(report.collisionRatio()).isGreaterThan(0.5d);
    }

    /** 空批哨兵：零实例、碰撞率 -1。 */
    @Test
    void emptyCohortYieldsSentinel() {
        for (RestartSpreadPlan.CohortReport report : List.of(
                RestartSpreadPlan.cohort(4, 400L, List.of()),
                RestartSpreadPlan.cohort(4, 400L, null))) {
            assertThat(report.instances()).isZero();
            assertThat(report.collisionRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形入参 fail-fast：空白 id、cohort < 1、窗 < 1。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> RestartSpreadPlan.delayFor(" ", 4, 400L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RestartSpreadPlan.delayFor("id", 0, 400L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cohortSize 不能小于 1");
        assertThatThrownBy(() -> RestartSpreadPlan.delayFor("id", 4, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spreadWindowMillis 不能小于 1");
    }
}
