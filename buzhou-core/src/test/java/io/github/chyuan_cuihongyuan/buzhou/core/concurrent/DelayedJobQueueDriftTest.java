package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1434 / T2170：延迟作业调度漂移读数——准时触发漂移≈0、人为推进时钟
 * 后漂移显形、executed 计数、reset；既有 submit/cancel 语义不变。
 */
class DelayedJobQueueDriftTest {

    @Test
    void executedJobsCountedWithDrift() throws Exception {
        DelayedJobQueue queue = new DelayedJobQueue();
        CountDownLatch done = new CountDownLatch(2);
        queue.submit("j1", done::countDown, Instant.now().plusMillis(50));
        queue.submit("j2", done::countDown, Instant.now().plusMillis(80));
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        queue.close();
        var s = queue.driftStats();
        assertThat(s.executed()).isEqualTo(2);
        assertThat(s.lastDriftMillis()).isGreaterThanOrEqualTo(0);
        assertThat(s.maxDriftMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void lateFireExposesPositiveDrift() throws Exception {
        // 阻塞调度线程制造迟到：占满 platform 调度器——用大 delay 任务先行占位不可行，
        // 改为直接验证：提交已过期的 fireAt（立即补跑）漂移=任务延迟量
        DelayedJobQueue queue = new DelayedJobQueue();
        CountDownLatch done = new CountDownLatch(1);
        queue.submit("late", done::countDown, Instant.now().minusSeconds(60)); // 已过期 60s
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        queue.close();
        // 过期补跑：漂移 ≈ 60_000ms（计划时刻早已过去）
        assertThat(queue.driftStats().maxDriftMillis())
                .isGreaterThanOrEqualTo(59_000);
    }

    @Test
    void replacedJobDoesNotDoubleExecute() throws Exception {
        DelayedJobQueue queue = new DelayedJobQueue();
        AtomicBoolean ran = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        queue.submit("dup", () -> assertThat(ran.compareAndSet(false, true)).isTrue(),
                Instant.now().plusMillis(60));
        queue.submit("dup", done::countDown, Instant.now().plusMillis(120)); // 替换
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        queue.close();
        assertThat(ran.get()).isFalse(); // 旧任务被取消不双跑
        assertThat(queue.driftStats().executed()).isEqualTo(1);
    }

    @Test
    void resetDriftClearsCounters() throws Exception {
        DelayedJobQueue queue = new DelayedJobQueue();
        CountDownLatch done = new CountDownLatch(1);
        queue.submit("j", done::countDown, Instant.now());
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        queue.resetDriftForTest();
        assertThat(queue.driftStats()).isEqualTo(new DelayedJobQueue.DriftStats(0, 0, 0));
        queue.close();
    }

    @Test
    void durationOverloadStillInstruments() throws Exception {
        DelayedJobQueue queue = new DelayedJobQueue();
        CountDownLatch done = new CountDownLatch(1);
        queue.submit("d", done::countDown, Duration.ofMillis(30));
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        queue.close();
        assertThat(queue.driftStats().executed()).isEqualTo(1);
    }
}
