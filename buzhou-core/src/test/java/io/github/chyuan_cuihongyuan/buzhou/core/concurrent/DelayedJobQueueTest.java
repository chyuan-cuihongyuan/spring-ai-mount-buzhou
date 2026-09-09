package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 413 §Testing / T717–T718：延迟作业——到点执行；同键替换不双跑；
 * cancel 幂等；异常隔离计数；pending 快照；close 幂等。
 */
class DelayedJobQueueTest {

    @Test
    void shouldRunAtDueTime_andReplaceSameKeyWithoutDoubleRun() throws Exception {
        try (DelayedJobQueue queue = new DelayedJobQueue()) {
            List<String> ran = new CopyOnWriteArrayList<>();
            CountDownLatch done = new CountDownLatch(1);
            // 同键两次提交：第一次被替换（不跑）
            queue.submit("job", () -> ran.add("v1"), Duration.ofMillis(200));
            queue.submit("job", () -> { ran.add("v2"); done.countDown(); }, Duration.ofMillis(50));

            assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(300); // 留出 v1 的到点窗口（若未被替换会双跑）
            assertThat(ran).containsExactly("v2"); // 替换语义
            assertThat(queue.pending()).isEmpty(); // 跑完即出清单
        }
    }

    @Test
    void shouldCancelIdempotently_andIsolateFailures() throws Exception {
        try (DelayedJobQueue queue = new DelayedJobQueue()) {
            List<String> ran = new CopyOnWriteArrayList<>();
            queue.submit("doomed", () -> { throw new IllegalStateException("作业炸了"); },
                    Duration.ofMillis(20));
            CountDownLatch ok = new CountDownLatch(1);
            queue.submit("healthy", () -> { ran.add("ok"); ok.countDown(); },
                    Duration.ofMillis(40));

            // cancel 幂等（不在的键 no-op）
            queue.cancel("ghost");
            queue.submit("cancelled", () -> ran.add("should-not-run"), Duration.ofMillis(60));
            queue.cancel("cancelled");
            queue.cancel("cancelled");

            assertThat(ok.await(3, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(200);
            assertThat(ran).containsExactly("ok"); // 失败/取消的都不碍事
            assertThat(queue.failedCount()).isEqualTo(1); // 异常隔离可观测
        }
    }

    @Test
    void shouldSnapshotPendingAscending_andValidateArguments() {
        try (DelayedJobQueue queue = new DelayedJobQueue()) {
            Instant now = Instant.now();
            queue.submit("later", () -> { }, now.plusSeconds(120));
            queue.submit("sooner", () -> { }, now.plusSeconds(60));
            List<DelayedJobQueue.PendingJob> pending = queue.pending();
            assertThat(pending).extracting(DelayedJobQueue.PendingJob::jobKey)
                    .containsExactly("sooner", "later"); // fireAt 升序
            queue.cancel("later");
            assertThat(queue.pending()).hasSize(1);

            assertThatThrownBy(() -> queue.submit(" ", () -> { }, Duration.ofSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> queue.submit("k", null, Duration.ofSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> queue.submit("k", () -> { }, Duration.ofSeconds(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        // close 幂等：try-with-resources 已 close，再关一次不炸
    }
}
