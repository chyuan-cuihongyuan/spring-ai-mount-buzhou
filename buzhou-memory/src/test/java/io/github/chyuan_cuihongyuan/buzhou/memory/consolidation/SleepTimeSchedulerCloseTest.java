package io.github.chyuan_cuihongyuan.buzhou.memory.consolidation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 44 §A / T159 / impl-130：SleepTimeScheduler 优雅关闭——close 先排空在途整理任务
 * （有界预算），超时才硬截断；修前 shutdownNow 直接丢 pending（Lifecycle javadoc 自认遗留）。
 */
class SleepTimeSchedulerCloseTest {

    @Test
    void closeDrainsPendingTaskWithinGrace() throws Exception {
        SleepTimeScheduler scheduler = new SleepTimeScheduler(64,
                Duration.ofMillis(50), Duration.ofSeconds(1), Duration.ofSeconds(5));
        CountDownLatch taskDone = new CountDownLatch(1);
        CountDownLatch taskStarted = new CountDownLatch(1);

        assertThat(scheduler.submit("s1", () -> {
            taskStarted.countDown();
            try {
                Thread.sleep(100); // 短任务：预算内必然完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            taskDone.countDown();
        })).isTrue();
        assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();

        scheduler.close(); // 优雅：等待任务收尾，不中断

        assertThat(taskDone.await(2, TimeUnit.SECONDS)).isTrue(); // 未被丢弃/中断
    }

    @Test
    void closeHardCutsAfterGraceBudgetExceeded() throws Exception {
        // 任务远超预算（10s），close 预算 200ms：有界等待后硬截断返回（close 本身不悬挂）
        SleepTimeScheduler scheduler = new SleepTimeScheduler(64,
                Duration.ofMillis(50), Duration.ofSeconds(1), Duration.ofMillis(200));
        AtomicBoolean finished = new AtomicBoolean();
        assertThat(scheduler.submit("s1", () -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 硬截断路径：shutdownNow 中断长任务
            }
            finished.set(true);
        })).isTrue();

        long start = System.nanoTime();
        scheduler.close();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(5_000); // 预算 + 收尾余量内返回
    }
}
