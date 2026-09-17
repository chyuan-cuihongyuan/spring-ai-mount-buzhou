package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.EdfScheduler.Pending;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 3003 / T5008：EDF 合同——截止期序、同刻 FIFO、空态诚实
 * （null/+∞）、peek 非破坏、laxity 正负可判、计数守恒、过期任务
 * 先出。
 */
class EdfSchedulerTest {

    @Test
    void shouldPollInDeadlineOrder() {
        EdfScheduler edf = new EdfScheduler();
        edf.offer(30, "B");
        edf.offer(10, "A");
        edf.offer(20, "C");
        assertThat(edf.poll().id()).isEqualTo("A");
        assertThat(edf.poll().id()).isEqualTo("C");
        assertThat(edf.poll().id()).isEqualTo("B");
        assertThat(edf.poll()).isNull();
    }

    @Test
    void sameDeadlineShouldBreakTieByInsertionOrder() {
        EdfScheduler edf = new EdfScheduler();
        edf.offer(10, "first");
        edf.offer(10, "second");
        edf.offer(10, "third");
        edf.offer(5, "earlier-deadline");
        assertThat(edf.poll().id()).isEqualTo("earlier-deadline");
        assertThat(edf.poll().id()).isEqualTo("first");
        assertThat(edf.poll().id()).isEqualTo("second");
        assertThat(edf.poll().id()).isEqualTo("third");
    }

    @Test
    void emptyQueueShouldBeHonest() {
        EdfScheduler edf = new EdfScheduler();
        assertThat(edf.isEmpty()).isTrue();
        assertThat(edf.size()).isZero();
        assertThat(edf.poll()).isNull();
        assertThat(edf.peek()).isNull();
        assertThat(edf.nextDeadline()).isEqualTo(EdfScheduler.NO_DEADLINE);
        assertThat(edf.nextDeadline()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void peekShouldNotRemove() {
        EdfScheduler edf = new EdfScheduler();
        edf.offer(20, "later");
        edf.offer(10, "sooner");
        assertThat(edf.peek().id()).isEqualTo("sooner");
        assertThat(edf.peek().id()).isEqualTo("sooner");
        assertThat(edf.size()).isEqualTo(2);
        assertThat(edf.poll().id()).isEqualTo("sooner");
    }

    @Test
    void headLaxityShouldSignalOverdueWhenNegative() {
        EdfScheduler edf = new EdfScheduler();
        edf.offer(100, "task");
        assertThat(edf.headLaxity(40)).isEqualTo(60);
        assertThat(edf.headLaxity(100)).isZero();
        assertThat(edf.headLaxity(150)).isEqualTo(-50);
        assertThat(edf.headLaxity(150)).isNegative();
    }

    @Test
    void overdueDeadlineShouldComeOutFirst() {
        // 截止期可为负（入队前已过期）——最小者最先出
        EdfScheduler edf = new EdfScheduler();
        edf.offer(0, "now");
        edf.offer(-5, "already-late");
        edf.offer(7, "future");
        assertThat(edf.poll().id()).isEqualTo("already-late");
        assertThat(edf.nextDeadline()).isZero();
    }

    @Test
    void sequenceShouldPreserveFullPendingRecord() {
        EdfScheduler edf = new EdfScheduler();
        edf.offer(42, "task");
        Pending p = edf.poll();
        assertThat(p.deadline()).isEqualTo(42);
        assertThat(p.id()).isEqualTo("task");
        assertThat(p.sequence()).isZero();
        edf.offer(43, "next");
        assertThat(edf.poll().sequence()).isEqualTo(1);
    }

    @Test
    void sizeAndEmptyShouldTrackLifecycle() {
        EdfScheduler edf = new EdfScheduler();
        edf.offer(1, "a");
        edf.offer(2, "b");
        assertThat(edf.isEmpty()).isFalse();
        assertThat(edf.size()).isEqualTo(2);
        edf.poll();
        edf.poll();
        assertThat(edf.isEmpty()).isTrue();
    }
}
