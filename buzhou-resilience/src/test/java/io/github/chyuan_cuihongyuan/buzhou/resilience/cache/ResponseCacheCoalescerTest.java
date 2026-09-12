package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * miss 惊群合并测试（spec 641 / T932–T933 / impl 494，singleflight +
 * proxy_cache_lock 语义）：并发同 key 收敛一次调用、失败不共享（各自降级直调）、
 * 终态清 entry 无泄漏。
 */
class ResponseCacheCoalescerTest {

    private static ChatResponse text(String s) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(s))));
    }

    /** 合并：N 并发同 key → supplier 只执行 1 次，全部拿到同一结果，waiters = N-1。 */
    @Test
    void concurrentSameKeyCoalescesToSingleCall() throws Exception {
        ResponseCacheCoalescer coalescer = new ResponseCacheCoalescer();
        int threads = 6;
        CyclicBarrier start = new CyclicBarrier(threads);
        AtomicInteger calls = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            Future<ChatResponse>[] futures = new Future[threads];
            for (int i = 0; i < threads; i++) {
                futures[i] = pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return coalescer.coalesce("k", () -> {
                        calls.incrementAndGet();
                        // leader 挂住足够久——确保其余线程全部进入等待路径（真合并而非先后到达）
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("sleep interrupted", e);
                        }
                        return text("one");
                    });
                });
            }
            for (Future<ChatResponse> f : futures) {
                assertThat(f.get(10, TimeUnit.SECONDS).getResult().getOutput().getText())
                        .isEqualTo("one");
            }
            assertThat(calls.get()).isEqualTo(1);
            assertThat(coalescer.coalescedWaiters()).isEqualTo(threads - 1);
            // 终态清 entry：在途归零（无泄漏）
            assertThat(coalescer.inFlightCount()).isZero();
        }
    }

    /** 失败不共享：leader 异常只归 leader 自己——等待者各自降级直调全部成功。 */
    @Test
    void leaderFailureDoesNotShareErrorWaitersFallBackToOwnCall() throws Exception {
        ResponseCacheCoalescer coalescer = new ResponseCacheCoalescer();
        int threads = 4;
        CyclicBarrier start = new CyclicBarrier(threads);
        AtomicInteger calls = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            Future<ChatResponse>[] futures = new Future[threads];
            for (int i = 0; i < threads; i++) {
                futures[i] = pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    // 第一次调用（真 leader）失败；此后任何调用（等待者降级直调 / 新 leader）都成功
                    return coalescer.coalesce("k", () -> {
                        int n = calls.incrementAndGet();
                        if (n == 1) {
                            throw new IllegalStateException("leader boom");
                        }
                        return text("own-" + n);
                    });
                });
            }
            int leaderFailures = 0;
            java.util.List<String> waitersOwn = new java.util.ArrayList<>();
            for (Future<ChatResponse> f : futures) {
                try {
                    waitersOwn.add(f.get(10, TimeUnit.SECONDS).getResult().getOutput().getText());
                } catch (java.util.concurrent.ExecutionException leaderOwn) {
                    leaderFailures++;
                    assertThat(leaderOwn.getCause()).isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("leader boom");
                }
            }
            // 失败只归 leader（恰 1 个失败 Future）；其余全部降级直调成功（失败不共享）
            assertThat(leaderFailures).isEqualTo(1);
            assertThat(waitersOwn).hasSize(threads - 1).allSatisfy(t -> assertThat(t).startsWith("own-"));
            assertThat(calls.get()).isGreaterThanOrEqualTo(threads);
            // 降级不计数——成功共享口径
            assertThat(coalescer.coalescedWaiters()).isZero();
            assertThat(coalescer.inFlightCount()).isZero();
        }
    }

    /** 不同 key 互不合并（并发异 key 各自执行——合并粒度 = 精确键）。 */
    @Test
    void differentKeysNeverCoalesce() throws Exception {
        ResponseCacheCoalescer coalescer = new ResponseCacheCoalescer();
        AtomicInteger calls = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<ChatResponse> a = pool.submit(() ->
                    coalescer.coalesce("k1", () -> { calls.incrementAndGet(); return text("a"); }));
            Future<ChatResponse> b = pool.submit(() ->
                    coalescer.coalesce("k2", () -> { calls.incrementAndGet(); return text("b"); }));
            assertThat(a.get(5, TimeUnit.SECONDS).getResult().getOutput().getText()).isEqualTo("a");
            assertThat(b.get(5, TimeUnit.SECONDS).getResult().getOutput().getText()).isEqualTo("b");
        }
        assertThat(calls.get()).isEqualTo(2);
        assertThat(coalescer.coalescedWaiters()).isZero();
    }
}
