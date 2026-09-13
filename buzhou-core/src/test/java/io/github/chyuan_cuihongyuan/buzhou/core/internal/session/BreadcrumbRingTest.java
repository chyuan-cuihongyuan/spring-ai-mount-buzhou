package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BreadcrumbRingTest {

    @Test
    void recordsAreNewestFirst() {
        BreadcrumbRing ring = new BreadcrumbRing();

        ring.record("a");
        ring.record("b");
        ring.record("c");

        List<io.github.chyuan_cuihongyuan.buzhou.core.session.EventBreadcrumb> snapshot = ring.snapshot();
        assertThat(snapshot).hasSize(3);
        assertThat(snapshot.get(0).type()).isEqualTo("c");
        assertThat(snapshot.get(2).type()).isEqualTo("a");
        assertThat(snapshot.get(0).epochMillis()).isPositive();
    }

    @Test
    void ringIsBoundedWithOldestEvicted() {
        BreadcrumbRing ring = new BreadcrumbRing();

        int total = BreadcrumbRing.CAPACITY + 8;
        for (int i = 0; i < total; i++) {
            ring.record("e" + i);
        }

        List<io.github.chyuan_cuihongyuan.buzhou.core.session.EventBreadcrumb> snapshot = ring.snapshot();
        assertThat(snapshot).hasSize(BreadcrumbRing.CAPACITY);
        assertThat(snapshot.get(0).type()).isEqualTo("e" + (total - 1));
        assertThat(snapshot.get(snapshot.size() - 1).type()).isEqualTo("e8");
    }

    @Test
    void snapshotIsImmutable() {
        BreadcrumbRing ring = new BreadcrumbRing();
        ring.record("a");

        List<io.github.chyuan_cuihongyuan.buzhou.core.session.EventBreadcrumb> snapshot = ring.snapshot();
        assertThatThrownBy(() -> snapshot.add(snapshot.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void clearEmptiesTheRing() {
        BreadcrumbRing ring = new BreadcrumbRing();
        ring.record("a");

        ring.clear();

        assertThat(ring.snapshot()).isEmpty();
    }
}
