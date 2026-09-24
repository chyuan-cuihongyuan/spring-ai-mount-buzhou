package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5031 / T6164：分层压实挑选合同——容量阶梯、评分挑层
 * （并列浅层优先）、最旧源表 + firstKey 序重叠目标、末层同层
 * 压实、完成出账、畸形 fail-fast。
 */
class LeveledCompactionTest {

    private static final int MAX_LEVEL = 4;

    private static final LeveledCompaction.LevelTable A = new LeveledCompaction.LevelTable("a", "k1", "k5");

    @Test
    void capacityLadderShouldFollowTriggerBaseRatio() {
        LeveledCompaction lc = new LeveledCompaction(MAX_LEVEL);
        assertThat(lc.capacityOf(0)).isEqualTo(4);
        assertThat(lc.capacityOf(1)).isEqualTo(2);
        assertThat(lc.capacityOf(2)).isEqualTo(20);
        assertThat(lc.capacityOf(3)).isEqualTo(200);
        assertThat(lc.capacityOf(4)).isEqualTo(2000);
    }

    @Test
    void planShouldReturnNullWhenNoLevelOverCapacity() {
        LeveledCompaction lc = new LeveledCompaction(MAX_LEVEL);
        lc.register(A, 0);
        assertThat(lc.plan()).isNull();
        assertThat(lc.scoreOf(0)).isEqualTo(0.25);
    }

    @Test
    void fullLevelShouldTriggerCompactionWithOldestSourceAndSortedTargets() {
        LeveledCompaction lc = new LeveledCompaction(MAX_LEVEL);
        lc.register(new LeveledCompaction.LevelTable("l0-1", "k0", "k9"), 0);
        lc.register(new LeveledCompaction.LevelTable("l0-2", "k2", "k2"), 0);
        lc.register(new LeveledCompaction.LevelTable("l0-3", "k3", "k3"), 0);
        lc.register(new LeveledCompaction.LevelTable("l0-4", "k4", "k4"), 0);
        lc.register(new LeveledCompaction.LevelTable("l1-a", "k4", "k5"), 1);
        lc.register(new LeveledCompaction.LevelTable("l1-b", "k6", "k7"), 1);

        assertThat(lc.scoreOf(0)).isEqualTo(1.0);
        assertThat(lc.scoreOf(1)).isEqualTo(1.0);
        LeveledCompaction.CompactionPlan plan = lc.plan();
        assertThat(plan).isNotNull();
        assertThat(plan.sourceLevel()).isZero();
        assertThat(plan.targetLevel()).isEqualTo(1);
        assertThat(plan.sourceTableId()).isEqualTo("l0-1");
        assertThat(plan.targetTableIds()).containsExactly("l1-a", "l1-b");
    }

    @Test
    void highestScoreLevelShouldWinAndTiesPreferShallower() {
        LeveledCompaction lc = new LeveledCompaction(MAX_LEVEL);
        for (int i = 0; i < 5; i++) {
            lc.register(new LeveledCompaction.LevelTable("l0-" + i, "k" + i, "k" + i), 0);
        }
        for (int i = 0; i < 3; i++) {
            lc.register(new LeveledCompaction.LevelTable("l1-" + i, "k" + (10 + i), "k" + (10 + i)), 1);
        }
        assertThat(lc.scoreOf(0)).isEqualTo(1.25);
        assertThat(lc.scoreOf(1)).isEqualTo(1.5);
        assertThat(lc.plan().sourceLevel()).isEqualTo(1);
    }

    @Test
    void equalScoresShouldPreferShallowerLevel() {
        LeveledCompaction lc = new LeveledCompaction(2);
        for (int i = 0; i < 5; i++) {
            lc.register(new LeveledCompaction.LevelTable("l0-" + i, "k" + i, "k" + i), 0);
        }
        for (int i = 0; i < 25; i++) {
            lc.register(new LeveledCompaction.LevelTable("l2-" + i, "k" + (100 + i), "k" + (100 + i)), 2);
        }
        assertThat(lc.scoreOf(0)).isEqualTo(1.25);
        assertThat(lc.scoreOf(2)).isEqualTo(1.25);
        assertThat(lc.plan().sourceLevel()).isZero();
    }

    @Test
    void maxLevelShouldCompactWithinSameLevel() {
        LeveledCompaction lc = new LeveledCompaction(1);
        lc.register(new LeveledCompaction.LevelTable("m1", "k1", "k5"), 1);
        lc.register(new LeveledCompaction.LevelTable("m2", "k4", "k8"), 1);
        lc.register(new LeveledCompaction.LevelTable("m3", "k6", "k9"), 1);
        LeveledCompaction.CompactionPlan plan = lc.plan();
        assertThat(plan.sourceLevel()).isEqualTo(1);
        assertThat(plan.targetLevel()).isEqualTo(1);
        assertThat(plan.sourceTableId()).isEqualTo("m1");
        assertThat(plan.targetTableIds()).containsExactly("m2");
        lc.complete(plan);
        assertThat(lc.tableCountOf(1)).isEqualTo(1);
    }

    @Test
    void completeShouldRemoveSourceAndTargetsAndAllowOutputReregister() {
        LeveledCompaction lc = new LeveledCompaction(MAX_LEVEL);
        lc.register(new LeveledCompaction.LevelTable("l0-1", "k1", "k9"), 0);
        lc.register(new LeveledCompaction.LevelTable("l0-2", "k2", "k2"), 0);
        lc.register(new LeveledCompaction.LevelTable("l0-3", "k3", "k3"), 0);
        lc.register(new LeveledCompaction.LevelTable("l0-4", "k4", "k4"), 0);
        lc.register(new LeveledCompaction.LevelTable("l1-a", "k5", "k6"), 1);
        LeveledCompaction.CompactionPlan plan = lc.plan();
        assertThat(plan.sourceTableId()).isEqualTo("l0-1");
        assertThat(plan.targetTableIds()).containsExactly("l1-a");
        lc.complete(plan);
        assertThat(lc.tableCountOf(0)).isEqualTo(3);
        assertThat(lc.tableCountOf(1)).isZero();
        lc.register(new LeveledCompaction.LevelTable("l1-merged", "k1", "k9"), 1);
        assertThat(lc.tableCountOf(1)).isEqualTo(1);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new LeveledCompaction(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeveledCompaction(10)).isInstanceOf(IllegalArgumentException.class);
        LeveledCompaction lc = new LeveledCompaction(MAX_LEVEL);
        assertThatThrownBy(() -> new LeveledCompaction.LevelTable("", "k", "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeveledCompaction.LevelTable("x", "k9", "k1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lc.register(A, 5)).isInstanceOf(IllegalArgumentException.class);
        lc.register(A, 0);
        assertThatThrownBy(() -> lc.register(A, 1)).isInstanceOf(IllegalArgumentException.class);
        lc.register(new LeveledCompaction.LevelTable("solo", "k", "k"), 1);
        assertThatThrownBy(() -> lc.complete(
                new LeveledCompaction.CompactionPlan(0, 1, "ghost", java.util.List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
