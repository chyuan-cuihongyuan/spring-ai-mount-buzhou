package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5043 / T6188：PN 计数器合同——增减取值、CRDT 三律
 * （交换/幂等/结合）、乱序同步收敛、单调表读数、fail-fast。
 */
class CrdtPnCounterTest {

    @Test
    void incrementsAndDecrementsShouldNetOut() {
        CrdtPnCounter counter = new CrdtPnCounter();
        counter.increment("a", 5);
        counter.increment("b", 2);
        counter.decrement("a", 3);
        assertThat(counter.value()).isEqualTo(4);
        assertThat(counter.incrementEntries()).containsEntry("a", 5L).containsEntry("b", 2L);
        assertThat(counter.decrementEntries()).containsEntry("a", 3L);
    }

    @Test
    void valueMayGoNegativeViaDecrementSide() {
        CrdtPnCounter counter = new CrdtPnCounter();
        counter.decrement("a", 7);
        assertThat(counter.value()).isEqualTo(-7);
    }

    @Test
    void mergeShouldBeCommutativeAndIdempotent() {
        CrdtPnCounter a = new CrdtPnCounter();
        a.increment("a", 3);
        a.decrement("b", 1);
        CrdtPnCounter b = new CrdtPnCounter();
        b.increment("c", 4);
        b.decrement("a", 2);
        CrdtPnCounter ab = cloneOf(a).merge(cloneOf(b));
        CrdtPnCounter ba = cloneOf(b).merge(cloneOf(a));
        assertThat(ab.value()).isEqualTo(ba.value());
        assertThat(ab.incrementEntries()).isEqualTo(ba.incrementEntries());
        CrdtPnCounter before = cloneOf(a);
        a.merge(a);
        assertThat(a.value()).isEqualTo(before.value());
        assertThat(a.incrementEntries()).isEqualTo(before.incrementEntries());
    }

    @Test
    void mergeShouldBeAssociative() {
        CrdtPnCounter a = counterFor("a", 1);
        CrdtPnCounter b = counterFor("b", 2);
        CrdtPnCounter c = counterFor("c", 3);
        CrdtPnCounter abC = cloneOf(a).merge(cloneOf(b)).merge(cloneOf(c));
        CrdtPnCounter aBc = cloneOf(a).merge(cloneOf(b).merge(cloneOf(c)));
        assertThat(abC.value()).isEqualTo(aBc.value());
        assertThat(abC.incrementEntries()).isEqualTo(aBc.incrementEntries());
    }

    @Test
    void replicasSyncingInDifferentOrdersShouldConverge() {
        CrdtPnCounter replica1 = counterFor("n1", 5);
        replica1.decrement("n1", 2);
        CrdtPnCounter replica2 = counterFor("n2", 8);
        replica2.decrement("n2", 6);
        CrdtPnCounter syncA = cloneOf(replica1).merge(replica2);
        CrdtPnCounter syncB = cloneOf(replica2).merge(replica1);
        assertThat(syncA.value()).isEqualTo(syncB.value());
        assertThat(syncA.incrementEntries()).isEqualTo(syncB.incrementEntries());
        assertThat(syncA.decrementEntries()).isEqualTo(syncB.decrementEntries());
        assertThat(syncA.value()).isEqualTo(5);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        CrdtPnCounter counter = new CrdtPnCounter();
        assertThatThrownBy(() -> counter.increment(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> counter.increment("", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> counter.increment("a", -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> counter.decrement("a", -2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> counter.merge(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private CrdtPnCounter counterFor(String nodeId, long amount) {
        CrdtPnCounter counter = new CrdtPnCounter();
        counter.increment(nodeId, amount);
        return counter;
    }

    private CrdtPnCounter cloneOf(CrdtPnCounter source) {
        CrdtPnCounter copy = new CrdtPnCounter();
        for (Map.Entry<String, Long> entry : source.incrementEntries().entrySet()) {
            copy.increment(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Long> entry : source.decrementEntries().entrySet()) {
            copy.decrement(entry.getKey(), entry.getValue());
        }
        return copy;
    }
}
