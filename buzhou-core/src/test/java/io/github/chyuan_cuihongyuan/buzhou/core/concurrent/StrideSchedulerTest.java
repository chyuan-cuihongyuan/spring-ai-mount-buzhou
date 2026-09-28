package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6027：StrideScheduler 合同——票数比例的确定性比例
 * 份额。比例精确性（短窗+长程）；并列 tie-break id 小；
 * 确定性序列；fail-fast。
 */
class StrideSchedulerTest {

    @Test
    void proportionalSharesExactOverWindow() {
        StrideScheduler scheduler = new StrideScheduler();
        scheduler.register(1, 1);
        scheduler.register(2, 1);
        scheduler.register(3, 2);
        Map<Long, Integer> counts = new HashMap<>();
        for (int i = 0; i < 400; i++) {
            counts.merge(scheduler.serve(), 1, Integer::sum);
        }
        assertThat(counts.get(1L)).isEqualTo(100);
        assertThat(counts.get(2L)).isEqualTo(100);
        assertThat(counts.get(3L)).isEqualTo(200);
    }

    @Test
    void twoClientStrictInterleave() {
        StrideScheduler scheduler = new StrideScheduler();
        scheduler.register(10, 1);
        scheduler.register(20, 1);
        long first = scheduler.serve();
        long second = scheduler.serve();
        assertThat(first).isLessThan(second);
        assertThat(scheduler.serve()).isEqualTo(first);
        assertThat(scheduler.serve()).isEqualTo(second);
    }

    @Test
    void deterministicSequenceForSameRegistration() {
        StrideScheduler a = new StrideScheduler();
        StrideScheduler b = new StrideScheduler();
        for (long id : new long[]{5, 3, 9}) {
            a.register(id, id % 3 + 1);
            b.register(id, id % 3 + 1);
        }
        for (int i = 0; i < 30; i++) {
            assertThat(b.serve()).isEqualTo(a.serve());
        }
    }

    @Test
    void removeExcludesClient() {
        StrideScheduler scheduler = new StrideScheduler();
        scheduler.register(1, 1);
        scheduler.register(2, 1);
        scheduler.remove(1);
        assertThat(scheduler.clientCount()).isEqualTo(1);
        for (int i = 0; i < 5; i++) {
            assertThat(scheduler.serve()).isEqualTo(2);
        }
    }

    @Test
    void failFastContract() {
        StrideScheduler scheduler = new StrideScheduler();
        assertThatThrownBy(scheduler::serve).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.register(1, 0)).isInstanceOf(IllegalArgumentException.class);
        scheduler.register(1, 1);
        assertThatThrownBy(() -> scheduler.register(1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.remove(2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.passOf(2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.ticketsOf(2)).isInstanceOf(IllegalArgumentException.class);
    }
}
