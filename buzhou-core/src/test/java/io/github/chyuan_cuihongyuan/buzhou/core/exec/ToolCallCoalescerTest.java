package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 139 / T488：在飞合并回归——同键并发单次执行扇出同值 / 异键各执行 /
 * 失败传播所有等待者 / 完成即忘（第二波再执行）。
 */
class ToolCallCoalescerTest {

    @Test
    void sameKeyConcurrentSubmissionsShareOneExecution() throws Exception {
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        AtomicInteger executions = new AtomicInteger();
        // 时序：首提交开始执行（started 计数）并挂起 → 追加 5 个同键（加入在飞）→ 放行
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<String> first = coalescer.submit("tool:hash-a", () -> {
                started.countDown();
                release.await();
                return "value-" + executions.incrementAndGet();
            }, pool);
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

            List<CompletableFuture<String>> joiners = new java.util.ArrayList<>();
            for (int i = 0; i < 5; i++) {
                joiners.add(coalescer.submit("tool:hash-a",
                        () -> "wrong-" + executions.incrementAndGet(), pool));
            }
            release.countDown();

            assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo("value-1");
            for (CompletableFuture<String> f : joiners) {
                assertThat(f.get(2, TimeUnit.SECONDS)).isEqualTo("value-1");
            }
            assertThat(executions.get()).isEqualTo(1);
            assertThat(coalescer.coalescedCount()).isEqualTo(5);
            assertThat(coalescer.inFlightCount()).isZero(); // 完成即忘
        }
    }

    @Test
    void differentKeysExecuteIndependently() throws Exception {
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        AtomicInteger executions = new AtomicInteger();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Integer> a = coalescer.submit("k-a",
                    executions::incrementAndGet, pool);
            CompletableFuture<Integer> b = coalescer.submit("k-b",
                    executions::incrementAndGet, pool);
            assertThat(a.get(2, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(b.get(2, TimeUnit.SECONDS)).isEqualTo(2);
            assertThat(coalescer.coalescedCount()).isZero();
        }
    }

    @Test
    void failurePropagatesToAllWaiters() throws Exception {
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Object> first = coalescer.submit("boom-key", () -> {
                started.countDown();
                release.await();
                throw new IllegalStateException("downstream 503");
            }, pool);
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

            List<CompletableFuture<Object>> joiners = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                joiners.add(coalescer.submit("boom-key", () -> "never", pool));
            }
            release.countDown();

            // 等待语义（failsWithin）：release→leader 醒来抛异常→future 异常完成之间
            // 有调度窗口——立即断言在高负载下抢跑假红（全仓 clean verify 复现）
            for (CompletableFuture<Object> f : joiners) {
                assertThat(f).failsWithin(2, TimeUnit.SECONDS);
            }
            assertThat(first).failsWithin(2, TimeUnit.SECONDS);
            assertThat(first.handle((r, e) -> e.getCause() == null ? e : e.getCause()))
                    .succeedsWithin(1, TimeUnit.SECONDS)
                    .isInstanceOf(IllegalStateException.class);
            assertThat(coalescer.inFlightCount()).isZero(); // 失败也即忘
        }
    }

    @Test
    void forgetOnCompletionAllowsSecondWave() throws Exception {
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        AtomicInteger executions = new AtomicInteger();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            coalescer.submit("k", executions::incrementAndGet, pool).get(2, TimeUnit.SECONDS);
            coalescer.submit("k", executions::incrementAndGet, pool).get(2, TimeUnit.SECONDS);
            // 完成即忘：合并≠缓存——同键第二波真实再执行
            assertThat(executions.get()).isEqualTo(2);
            assertThat(coalescer.coalescedCount()).isZero();
        }
    }

    @Test
    void argumentsValidated() {
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> coalescer.submit(" ", () -> 1, pool))
                    .isInstanceOf(IllegalArgumentException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> coalescer.submit("k", null, pool))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
