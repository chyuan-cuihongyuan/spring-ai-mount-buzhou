package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.transaction.TwoPhaseCoordinator.Phase;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5016 / T6134：2PC 合同——全票提交、一票否决、PREPARING
 * 直接 abort、非法迁移 fail-fast、确定性回放。
 */
class TwoPhaseCoordinatorTest {

    private static final Set<String> PARTICIPANTS = Set.of("db", "queue", "cache");

    @Test
    void unanimousYesShouldReachCommitted() {
        TwoPhaseCoordinator coordinator = new TwoPhaseCoordinator();
        coordinator.begin("t1", PARTICIPANTS);
        assertThat(coordinator.phaseOf("t1")).isEqualTo(Phase.PREPARING);
        for (String participant : PARTICIPANTS) {
            coordinator.votePrepare("t1", participant, true);
        }
        assertThat(coordinator.phaseOf("t1")).isEqualTo(Phase.PREPARED);
        assertThat(coordinator.commit("t1")).isEqualTo(Phase.COMMITTED);
    }

    @Test
    void singleNoShouldAbortVeto() {
        TwoPhaseCoordinator coordinator = new TwoPhaseCoordinator();
        coordinator.begin("t1", PARTICIPANTS);
        coordinator.votePrepare("t1", "db", true);
        assertThat(coordinator.votePrepare("t1", "queue", false)).isEqualTo(Phase.ABORTED);
        assertThatThrownBy(() -> coordinator.commit("t1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PREPARED");
        assertThat(coordinator.votedOf("t1")).containsExactlyInAnyOrder("db", "queue");
    }

    @Test
    void preparingShouldAbortDirectly() {
        TwoPhaseCoordinator coordinator = new TwoPhaseCoordinator();
        coordinator.begin("t1", PARTICIPANTS);
        assertThat(coordinator.abort("t1")).isEqualTo(Phase.ABORTED);
        assertThatThrownBy(() -> coordinator.votePrepare("t1", "db", true))
                .isInstanceOf(IllegalArgumentException.class);   // 非投票期
    }

    @Test
    void duplicateVotesAndUnknownParticipantsShouldFailFast() {
        TwoPhaseCoordinator coordinator = new TwoPhaseCoordinator();
        coordinator.begin("t1", PARTICIPANTS);
        coordinator.votePrepare("t1", "db", true);
        assertThatThrownBy(() -> coordinator.votePrepare("t1", "db", true))
                .isInstanceOf(IllegalArgumentException.class);   // 重复投票
        assertThatThrownBy(() -> coordinator.votePrepare("t1", "ghost", true))
                .isInstanceOf(IllegalArgumentException.class);   // 未知参与者
        assertThatThrownBy(() -> coordinator.begin("t1", PARTICIPANTS))
                .isInstanceOf(IllegalArgumentException.class);   // 重复事务
    }

    @Test
    void illegalTransitionsShouldFailFast() {
        TwoPhaseCoordinator coordinator = new TwoPhaseCoordinator();
        coordinator.begin("t1", PARTICIPANTS);
        coordinator.votePrepare("t1", "db", true);
        coordinator.votePrepare("t1", "queue", true);
        coordinator.votePrepare("t1", "cache", true);
        coordinator.commit("t1");
        assertThatThrownBy(() -> coordinator.abort("t1"))
                .isInstanceOf(IllegalArgumentException.class);   // COMMITTED 后 abort
        assertThatThrownBy(() -> coordinator.commit("t2"))
                .isInstanceOf(IllegalArgumentException.class);   // 未知事务
    }
}
