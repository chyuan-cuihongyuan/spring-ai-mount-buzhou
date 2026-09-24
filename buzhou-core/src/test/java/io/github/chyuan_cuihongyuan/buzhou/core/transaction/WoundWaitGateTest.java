package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5030 / T6162：wait-die/wound-wait 死锁预防合同——
 * 两模式四裁决面、年长接管/等待分叉、释放交接年长等待者、
 * 收尾清理、fail-fast。
 */
class WoundWaitGateTest {

    private static final long OLD_TS = 100L;

    private static final long YOUNG_TS = 200L;

    private static final String RESOURCE = "row-42";

    @Test
    void freeResourceShouldGrantImmediately() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WOUND_WAIT);
        gate.begin("t1", OLD_TS);
        assertThat(gate.acquire("t1", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThat(gate.holdersOf(RESOURCE)).containsExactly("t1");
        assertThat(gate.abortedCount()).isZero();
    }

    @Test
    void woundWaitOlderShouldWoundYoungerHolder() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WOUND_WAIT);
        gate.begin("young", YOUNG_TS);
        gate.begin("old", OLD_TS);
        assertThat(gate.acquire("young", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThat(gate.acquire("old", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.ABORT_HOLDER);
        assertThat(gate.holdersOf(RESOURCE)).containsExactly("old");
        assertThat(gate.transactionCount()).isEqualTo(1);
        assertThat(gate.abortedCount()).isEqualTo(1);
    }

    @Test
    void woundWaitYoungerShouldWaitBehindOlderHolder() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WOUND_WAIT);
        gate.begin("old", OLD_TS);
        gate.begin("young", YOUNG_TS);
        assertThat(gate.acquire("old", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThat(gate.acquire("young", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.WAIT);
        assertThat(gate.waitersOf(RESOURCE)).containsExactly("young");
        assertThat(gate.holdersOf(RESOURCE)).containsExactly("old");
    }

    @Test
    void waitDieYoungerShouldDieOnOlderHolder() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WAIT_DIE);
        gate.begin("old", OLD_TS);
        gate.begin("young", YOUNG_TS);
        assertThat(gate.acquire("old", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThat(gate.acquire("young", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.ABORT_REQUESTOR);
        assertThat(gate.holdersOf(RESOURCE)).containsExactly("old");
        assertThat(gate.transactionCount()).isEqualTo(1);
        assertThat(gate.abortedCount()).isEqualTo(1);
    }

    @Test
    void waitDieOlderShouldWaitBehindYoungerHolder() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WAIT_DIE);
        gate.begin("young", YOUNG_TS);
        gate.begin("old", OLD_TS);
        assertThat(gate.acquire("young", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThat(gate.acquire("old", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.WAIT);
        assertThat(gate.waitersOf(RESOURCE)).containsExactly("old");
    }

    @Test
    void woundWaitReleaseShouldHandOffToOldestWaiter() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WOUND_WAIT);
        gate.begin("holder", 300L);
        gate.begin("mid-waiter", 400L);
        gate.begin("older-waiter", 350L);
        assertThat(gate.acquire("holder", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThat(gate.acquire("mid-waiter", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.WAIT);
        assertThat(gate.acquire("older-waiter", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.WAIT);
        assertThat(gate.waitersOf(RESOURCE)).containsExactly("older-waiter", "mid-waiter");
        String granted = gate.release("holder", RESOURCE);
        assertThat(granted).isEqualTo("older-waiter");
        assertThat(gate.holdersOf(RESOURCE)).containsExactly("older-waiter");
        assertThat(gate.waitersOf(RESOURCE)).containsExactly("mid-waiter");
    }

    @Test
    void finishShouldReleaseResourcesAndDropWaiterEntries() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WAIT_DIE);
        gate.begin("holder", 200L);
        gate.begin("w150", 150L);
        gate.begin("w100", 100L);
        assertThat(gate.acquire("holder", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThat(gate.acquire("w150", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.WAIT);
        assertThat(gate.acquire("w100", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.WAIT);
        assertThat(gate.waitersOf(RESOURCE)).containsExactly("w100", "w150");
        gate.finish("holder");
        assertThat(gate.holdersOf(RESOURCE)).containsExactly("w100");
        assertThat(gate.waitersOf(RESOURCE)).containsExactly("w150");
        assertThat(gate.transactionCount()).isEqualTo(2);
    }

    @Test
    void equalTimestampsShouldBreakTieByIdOrder() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WOUND_WAIT);
        gate.begin("z-holder", 500L);
        gate.begin("a-requestor", 500L);
        assertThat(gate.acquire("z-holder", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        // 同时间戳：id 字典序小者为年长——a-requestor 年长 → 枪伤 z-holder
        assertThat(gate.acquire("a-requestor", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.ABORT_HOLDER);
        assertThat(gate.holdersOf(RESOURCE)).containsExactly("a-requestor");
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        WoundWaitGate gate = new WoundWaitGate(WoundWaitGate.Mode.WOUND_WAIT);
        assertThatThrownBy(() -> new WoundWaitGate(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.begin(null, 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.begin("", 1L)).isInstanceOf(IllegalArgumentException.class);
        gate.begin("t1", OLD_TS);
        assertThatThrownBy(() -> gate.begin("t1", YOUNG_TS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.acquire("ghost", RESOURCE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.acquire("t1", null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(gate.acquire("t1", RESOURCE)).isEqualTo(WoundWaitGate.Verdict.GRANTED);
        assertThatThrownBy(() -> gate.acquire("t1", RESOURCE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.release("ghost", RESOURCE)).isInstanceOf(IllegalArgumentException.class);
        gate.begin("t2", YOUNG_TS);
        assertThatThrownBy(() -> gate.release("t2", RESOURCE)).isInstanceOf(IllegalArgumentException.class);
    }
}
