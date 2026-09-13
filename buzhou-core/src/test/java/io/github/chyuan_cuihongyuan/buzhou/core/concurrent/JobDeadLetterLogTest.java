package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 809 / T1120：作业死信台账回归——观察者挂接回调/环形挤老/聚合降序/
 * truncated/观察者异常隔离/无观察者零变化。
 */
class JobDeadLetterLogTest {

    /** 可拨动时钟。 */
    private static final class MutableClock extends Clock {
        long now = 500L;

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(now);
        }
    }

    private JobDeadLetterLog log;

    private DelayedJobQueue queueWithObserver() {
        MutableClock clock = new MutableClock();
        log = new JobDeadLetterLog();
        return new DelayedJobQueue(clock, (key, err) -> log.recordFailure(key, err, clock.millis()));
    }

    @Test
    void failedJobLandsInLedger() throws Exception {
        DelayedJobQueue queue = queueWithObserver();
        CountDownLatch done = new CountDownLatch(1);
        queue.submit("bad-job", () -> {
            throw new IllegalStateException("boom-reason");
        }, Duration.ofMillis(10));
        // 观察者回调后倒计数由台账轮询替代——直接等待异步执行
        queue.submit("marker", done::countDown, Duration.ofMillis(30));
        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();

        // marker 后 bad-job 必已执行（单线程调度器顺序保证）
        List<JobDeadLetterLog.DeadJob> recent = log.snapshot().recent();
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).jobKey()).isEqualTo("bad-job");
        assertThat(recent.get(0).errorType()).isEqualTo("IllegalStateException");
        assertThat(recent.get(0).message()).isEqualTo("boom-reason");
        assertThat(recent.get(0).atMillis()).isEqualTo(500L);
        queue.close();
    }

    @Test
    void aggregateSortedByCountWithTruncationFlag() {
        JobDeadLetterLog log = new JobDeadLetterLog();
        log.recordFailure("a", new RuntimeException("x"), 1);
        log.recordFailure("a", new RuntimeException("y"), 2);
        log.recordFailure("b", new RuntimeException("z"), 3);

        JobDeadLetterLog.Snapshot snap = log.snapshot();
        assertThat(snap.totalFailed()).isEqualTo(3);
        assertThat(snap.byKey().get(0).jobKey()).isEqualTo("a");
        assertThat(snap.byKey().get(0).count()).isEqualTo(2);
        assertThat(snap.byKey().get(0).lastErrorType()).isEqualTo("RuntimeException");
        assertThat(snap.truncated()).isFalse();
        assertThat(snap.recent().get(0).jobKey()).isEqualTo("b"); // 新→旧
    }

    @Test
    void ringEvictsOldestAndAggregateCaps() {
        JobDeadLetterLog log = new JobDeadLetterLog();
        for (int i = 0; i < JobDeadLetterLog.RING_CAPACITY + 5; i++) {
            log.recordFailure("k" + i, new RuntimeException(), i);
        }
        JobDeadLetterLog.Snapshot snap = log.snapshot();
        assertThat(snap.recent()).hasSize(JobDeadLetterLog.RING_CAPACITY);
        assertThat(snap.recent().get(0).jobKey()).isEqualTo("k" + (JobDeadLetterLog.RING_CAPACITY + 4));
        assertThat(snap.recent().get(snap.recent().size() - 1).jobKey()).isEqualTo("k5");
        // 聚合键封顶：超 AGGREGATE_CAP 的新键不再入聚合（truncated 如实）
        assertThat(snap.truncated()).isTrue();
    }

    @Test
    void messageTruncatedTo200() {
        JobDeadLetterLog log = new JobDeadLetterLog();
        log.recordFailure("k", new RuntimeException("m".repeat(500)), 1);
        assertThat(log.snapshot().recent().get(0).message()).hasSize(JobDeadLetterLog.MSG_MAX);
    }

    @Test
    void observerFailureIsolatedAndNullObserverUnchanged() throws Exception {
        // 观察者自身抛异常——调度线程不炸，后续作业照常
        MutableClock clock = new MutableClock();
        DelayedJobQueue queue = new DelayedJobQueue(clock, (key, err) -> {
            throw new RuntimeException("observer-broken");
        });
        CountDownLatch done = new CountDownLatch(1);
        queue.submit("bad", () -> { throw new IllegalStateException(); }, Duration.ofMillis(5));
        queue.submit("after", done::countDown, Duration.ofMillis(15));
        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(queue.failedCount()).isEqualTo(1); // 失败计数照常
        queue.close();

        // 无观察者：原行为零变化
        DelayedJobQueue plain = new DelayedJobQueue();
        plain.submit("bad2", () -> { throw new IllegalStateException(); }, Duration.ofMillis(5));
        Thread.sleep(150);
        assertThat(plain.failedCount()).isEqualTo(1);
        plain.close();
    }
}
