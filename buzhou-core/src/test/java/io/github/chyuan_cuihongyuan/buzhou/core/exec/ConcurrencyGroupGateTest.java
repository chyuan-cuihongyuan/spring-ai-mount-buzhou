package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ConcurrencyGroupGate.GroupOutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2028 / T3158：并发组闸合同——互斥/重入幂等/取代语义/占用拒/
 * 属主栅栏（迟到完成不误伤）/四计数/畸形 fail-fast。
 */
class ConcurrencyGroupGateTest {

    @Test
    void idleGroupShouldGrantAndTrackOwner() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(false);
        assertThat(gate.tryEnter("deploy", "run-1")).isEqualTo(GroupOutcome.GRANTED);
        assertThat(gate.ownerOf("deploy")).isEqualTo("run-1");
    }

    @Test
    void sameTaskReentryShouldBeIdempotent() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(false);
        gate.tryEnter("g", "t1");
        assertThat(gate.tryEnter("g", "t1")).isEqualTo(GroupOutcome.GRANTED); // 重入幂等
        assertThat(gate.stats().busyRejections()).isZero();
    }

    @Test
    void busyGroupWithoutCancelInProgressShouldReject() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(false);
        gate.tryEnter("g", "t1");
        assertThat(gate.tryEnter("g", "t2")).isEqualTo(GroupOutcome.BUSY_REJECTED);
        assertThat(gate.ownerOf("g")).isEqualTo("t1"); // 在跑者优先
        assertThat(gate.stats().busyRejections()).isEqualTo(1L);
    }

    @Test
    void cancelInProgressShouldSupersedeOldOwner() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(true);
        gate.tryEnter("g", "old");
        assertThat(gate.tryEnter("g", "new")).isEqualTo(GroupOutcome.SUPERSEDED);
        assertThat(gate.ownerOf("g")).isEqualTo("new"); // 新者接管
        assertThat(gate.stats().supersessions()).isEqualTo(1L);
        // 被取代者的迟到完成——栅栏拦下，不误伤新属主
        assertThat(gate.complete("g", "old")).isFalse();
        assertThat(gate.ownerOf("g")).isEqualTo("new");
        assertThat(gate.stats().fencedCompletions()).isEqualTo(1L);
    }

    @Test
    void completionShouldReleaseGroupForNextEnter() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(false);
        gate.tryEnter("g", "t1");
        assertThat(gate.complete("g", "t1")).isTrue();
        assertThat(gate.ownerOf("g")).isNull();
        assertThat(gate.tryEnter("g", "t2")).isEqualTo(GroupOutcome.GRANTED); // 释放后可入
        assertThat(gate.stats().completions()).isEqualTo(1L);
    }

    @Test
    void completingIdleGroupShouldReturnFalse() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(false);
        assertThat(gate.complete("g", "t1")).isFalse(); // 组已空闲
        assertThat(gate.stats().completions()).isZero();
    }

    @Test
    void independentGroupsShouldNotInterfere() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(false);
        gate.tryEnter("a", "t1");
        assertThat(gate.tryEnter("b", "t2")).isEqualTo(GroupOutcome.GRANTED); // 组间独立
        assertThat(gate.ownerOf("a")).isEqualTo("t1");
        assertThat(gate.ownerOf("b")).isEqualTo("t2");
    }

    @Test
    void malformedInputsShouldFailFast() {
        ConcurrencyGroupGate gate = new ConcurrencyGroupGate(false);
        assertThatThrownBy(() -> gate.tryEnter(null, "t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.tryEnter(" ", "t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.tryEnter("g", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.complete(null, "t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.complete("g", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.ownerOf(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
