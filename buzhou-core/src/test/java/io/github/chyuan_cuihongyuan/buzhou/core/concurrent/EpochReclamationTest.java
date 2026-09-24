package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5036 / T6174：EBR 时代回收合同——守卫钉住时代、
 * 回收安全性（守卫未退不可回收）、时代粒度分拣、确定性
 * 回收序、双重关闭 fail-fast。
 */
class EpochReclamationTest {

    @Test
    void retireAtEpochShouldNotReclaimWhileGuardPinned() {
        EpochReclamation ebr = new EpochReclamation();
        EpochReclamation.Guard guard = ebr.enter();
        ebr.retire("n1");
        ebr.advanceEpoch();
        assertThat(ebr.tryReclaim()).isEmpty();
        guard.close();
        assertThat(ebr.tryReclaim()).containsExactly("n1");
    }

    @Test
    void retiredWithoutGuardsShouldReclaimAfterEpochAdvances() {
        EpochReclamation ebr = new EpochReclamation();
        ebr.retire("n1");
        ebr.advanceEpoch();
        assertThat(ebr.currentEpoch()).isEqualTo(1);
        assertThat(ebr.tryReclaim()).containsExactly("n1");
        assertThat(ebr.retiredCount()).isZero();
    }

    @Test
    void sameEpochRetireShouldNotReclaimWithoutAdvance() {
        EpochReclamation ebr = new EpochReclamation();
        ebr.retire("n1");
        assertThat(ebr.tryReclaim()).isEmpty();
        ebr.advanceEpoch();
        assertThat(ebr.tryReclaim()).containsExactly("n1");
    }

    @Test
    void olderEpochItemsShouldReclaimWhileNewerPinned() {
        EpochReclamation ebr = new EpochReclamation();
        ebr.retire("old");
        ebr.advanceEpoch();
        ebr.retire("newer");
        EpochReclamation.Guard guard = ebr.enter();
        assertThat(guard.epoch()).isEqualTo(1);
        ebr.advanceEpoch();
        assertThat(ebr.tryReclaim()).containsExactly("old");
        assertThat(ebr.retiredCount()).isEqualTo(1);
        guard.close();
        assertThat(ebr.tryReclaim()).containsExactly("newer");
    }

    @Test
    void reclaimsShouldBeDeterministicAcrossEpochsAndRetireOrder() {
        EpochReclamation ebr = new EpochReclamation();
        ebr.retire("a");
        ebr.retire("b");
        ebr.advanceEpoch();
        ebr.retire("c");
        EpochReclamation.Guard guard = ebr.enter();
        assertThat(guard.epoch()).isEqualTo(1);
        ebr.advanceEpoch();
        assertThat(ebr.tryReclaim()).containsExactly("a", "b");
        guard.close();
        assertThat(ebr.tryReclaim()).containsExactly("c");
    }

    @Test
    void activeGuardsReadoutShouldTrackEnterExit() {
        EpochReclamation ebr = new EpochReclamation();
        assertThat(ebr.activeGuards()).isZero();
        EpochReclamation.Guard g1 = ebr.enter();
        EpochReclamation.Guard g2 = ebr.enter();
        assertThat(ebr.activeGuards()).isEqualTo(2);
        g1.close();
        assertThat(ebr.activeGuards()).isEqualTo(1);
        g2.close();
        assertThat(ebr.activeGuards()).isZero();
    }

    @Test
    void invalidUsageShouldFailFast() {
        EpochReclamation ebr = new EpochReclamation();
        assertThatThrownBy(() -> ebr.retire(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ebr.retire("")).isInstanceOf(IllegalArgumentException.class);
        ebr.retire("dup");
        assertThatThrownBy(() -> ebr.retire("dup")).isInstanceOf(IllegalArgumentException.class);
        EpochReclamation.Guard guard = ebr.enter();
        guard.close();
        assertThatThrownBy(guard::close).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(guard::epoch).isInstanceOf(IllegalStateException.class);
    }
}
