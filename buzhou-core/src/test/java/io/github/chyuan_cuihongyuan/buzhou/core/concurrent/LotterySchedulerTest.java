package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6044：LotteryScheduler 合同——票数比例加权随机。
 * 长程比例期望收敛（±10%）；确定性序列；注销；fail-fast。
 */
class LotterySchedulerTest {

    @Test
    void longRunConvergesToTicketProportion() {
        LotteryScheduler lottery = new LotteryScheduler(6044L);
        lottery.register(1, 1);
        lottery.register(2, 3);
        Map<Long, Integer> counts = new HashMap<>();
        for (int i = 0; i < 4000; i++) {
            counts.merge(lottery.draw(), 1, Integer::sum);
        }
        assertThat(counts.get(1L)).isBetween(850, 1150);
        assertThat(counts.get(2L)).isBetween(2850, 3150);
    }

    @Test
    void sameSeedSameSequence() {
        LotteryScheduler a = new LotteryScheduler(99L);
        LotteryScheduler b = new LotteryScheduler(99L);
        a.register(1, 1);
        a.register(2, 2);
        b.register(1, 1);
        b.register(2, 2);
        for (int i = 0; i < 50; i++) {
            assertThat(b.draw()).isEqualTo(a.draw());
        }
    }

    @Test
    void removeExcludesAndRebalances() {
        LotteryScheduler lottery = new LotteryScheduler(7L);
        lottery.register(1, 1);
        lottery.register(2, 1);
        lottery.remove(1);
        assertThat(lottery.clientCount()).isEqualTo(1);
        assertThat(lottery.totalTickets()).isEqualTo(1);
        for (int i = 0; i < 10; i++) {
            assertThat(lottery.draw()).isEqualTo(2);
        }
    }

    @Test
    void failFastContract() {
        LotteryScheduler lottery = new LotteryScheduler(1L);
        assertThatThrownBy(lottery::draw).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lottery.register(1, 0)).isInstanceOf(IllegalArgumentException.class);
        lottery.register(1, 5);
        assertThatThrownBy(() -> lottery.register(1, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lottery.remove(2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lottery.ticketsOf(2)).isInstanceOf(IllegalArgumentException.class);
    }
}
