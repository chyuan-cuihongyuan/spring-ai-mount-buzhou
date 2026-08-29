package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 122 §B / T443：事务性并行批红队——全员成功按提交序返回（完成序打乱不扰动）；
 * 首败即中止在途（不等慢同伴 + 在途者被中断）+ 部分结果不可见 + 每任务去向入异常
 * message；空批零提交（不触碰执行器）；参数 fail-fast。借鉴：LangGraph superstep。
 */
class SuperstepBatchTest {

    @AfterEach
    void cleanup() {
        ErrorSignatures.install(null);
    }

    @Test
    void allSucceedReturnsResultsInSubmissionOrder() throws Exception {
        Map<String, Callable<String>> tasks = new LinkedHashMap<>();
        tasks.put("first-slow", () -> {
            Thread.sleep(60);
            return "v1";
        });
        tasks.put("second-fast", () -> "v2");
        tasks.put("third-mid", () -> {
            Thread.sleep(20);
            return "v3";
        });
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<String, String> out = SuperstepBatch.runAll("s1", tasks, executor);
            // 完成序 second→third→first，返回仍按提交序
            assertThat(out.keySet()).containsExactly("first-slow", "second-fast", "third-mid");
            assertThat(out).containsEntry("first-slow", "v1").containsEntry("second-fast", "v2");
        }
    }

    @Test
    void firstFailureAbortsInFlightAndHidesPartialResults() throws Exception {
        CountDownLatch slowStarted = new CountDownLatch(1);
        AtomicBoolean slowInterrupted = new AtomicBoolean(false);
        Map<String, Callable<String>> tasks = new LinkedHashMap<>();
        tasks.put("quick-ok", () -> "done");
        tasks.put("slow-inflight", () -> {
            slowStarted.countDown();
            try {
                Thread.sleep(5_000);
                return "late";
            } catch (InterruptedException e) {
                slowInterrupted.set(true);
                throw e;
            }
        });
        tasks.put("boom", () -> {
            throw new IllegalStateException("upstream 503 after 2 tries");
        });
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertThatThrownBy(() -> SuperstepBatch.runAll("s2", tasks, executor))
                    .isInstanceOf(BuzhouException.class)
                    .hasMessageContaining("<boom>")
                    .hasMessageContaining("IllegalStateException")
                    .hasMessageContaining("upstream 503 after 2 tries") // message 保留原文
                    .hasMessageContaining("aborted=[slow-inflight]")
                    .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                            .isEqualTo(ErrorCode.SUPERSTEP_FAILED));
            // 首败不等慢同伴：slow 被中断（5s 任务在毫秒级内结束）
            assertThat(slowStarted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(slowInterrupted).isTrue();
        }
        // 首败入错误签名族（kind=superstep；Throwable 面 = 简名:归一消息）
        assertThat(ErrorSignatures.global().snapshot())
                .containsKey("superstep:IllegalStateException:upstream # after # tries");
    }

    @Test
    void emptyBatchReturnsEmptyWithoutTouchingExecutor() throws Exception {
        // 执行器一旦被使用即炸——空批零提交的诚实面
        ExecutorService hostile = new NoopExecutorService();
        Map<String, String> out = SuperstepBatch.runAll("s3", Map.of(), hostile);
        assertThat(out).isEmpty();
    }

    @Test
    void argumentsAreValidatedFailFast() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertThatThrownBy(() -> SuperstepBatch.runAll(" ", Map.of("a", () -> 1), executor))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> SuperstepBatch.runAll("s", null, executor))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> SuperstepBatch.runAll("s", Map.of("a", () -> 1), null))
                    .isInstanceOf(IllegalArgumentException.class);
            Map<String, Callable<Integer>> blankKey = new LinkedHashMap<>();
            blankKey.put("", () -> 1);
            assertThatThrownBy(() -> SuperstepBatch.runAll("s", blankKey, executor))
                    .isInstanceOf(IllegalArgumentException.class);
            Map<String, Callable<Integer>> nullTask = new LinkedHashMap<>();
            nullTask.put("a", null);
            assertThatThrownBy(() -> SuperstepBatch.runAll("s", nullTask, executor))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /** 恶意执行器：任何提交都是违规（空批零提交断言用）。 */
    private static final class NoopExecutorService
            extends java.util.concurrent.AbstractExecutorService {
        @Override
        public void execute(Runnable command) {
            throw new AssertionError("empty batch must not touch the executor");
        }

        @Override public void shutdown() { }
        @Override public java.util.List<Runnable> shutdownNow() { return java.util.List.of(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
    }
}
