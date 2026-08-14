package io.github.chyuan_cuihongyuan.buzhou.memory.consolidation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-38 / spec 13 §growth-8：sleep-time 调度器深度治理——pending 队列上限
 * （超限丢弃计数）、会话结束摘除、失败指数退避重试（成功复位）。
 */
class SleepTimeSchedulerGovernanceTest {

    @Test
    void pendingCapDropsOverflowVisibly() throws Exception {
        // 首任务阻塞队列：后续 69 个进 pending（上限 64）→ 丢弃 6 个
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch firstStarted = new CountDownLatch(1);
        try (SleepTimeScheduler scheduler = new SleepTimeScheduler(
                64, java.time.Duration.ofMillis(5), java.time.Duration.ofMillis(50))) {
            scheduler.submit("s1", () -> {
                firstStarted.countDown();
                await(release);
            });
            assertThat(firstStarted.await(5, TimeUnit.SECONDS)).isTrue();
            int accepted = 0;
            for (int i = 0; i < 69; i++) {
                if (scheduler.submit("s1", () -> { })) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(64);
            assertThat(scheduler.droppedTaskCount()).isEqualTo(5); // 69 - 64
            release.countDown();
        }
    }

    @Test
    void removeSessionDropsQueueAndRegistry() throws Exception {
        try (SleepTimeScheduler scheduler = new SleepTimeScheduler(
                64, java.time.Duration.ofMillis(5), java.time.Duration.ofMillis(50))) {
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch started = new CountDownLatch(1);
            scheduler.submit("s1", () -> {
                started.countDown();
                await(release);
            });
            scheduler.submit("s1", () -> { }); // pending 一枚
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(scheduler.sessionCount()).isEqualTo(1);

            scheduler.removeSession("s1"); // 会话结束摘除（幂等）
            scheduler.removeSession("s1");
            assertThat(scheduler.sessionCount()).isZero();
            release.countDown();
        }
    }

    @Test
    void failingTaskIsRetriedWithBackoffUntilSuccess() throws Exception {
        try (SleepTimeScheduler scheduler = new SleepTimeScheduler(
                64, java.time.Duration.ofMillis(10), java.time.Duration.ofMillis(50))) {
            AtomicInteger attempts = new AtomicInteger();
            CountDownLatch done = new CountDownLatch(1);
            AtomicInteger successRuns = new AtomicInteger();
            scheduler.submit("s1", () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new IllegalStateException("poison turn " + attempts.get());
                }
                done.countDown();
            });
            scheduler.submit("s1", successRuns::incrementAndGet);

            // 毒任务重试至成功（指数退避 base 10ms）；后继任务随后照常执行
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(attempts.get()).isGreaterThanOrEqualTo(3);
            assertThat(successRuns.get()).isGreaterThanOrEqualTo(1);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
