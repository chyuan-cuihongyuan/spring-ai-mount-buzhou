package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 411 §Testing / T713–T714：优先级泳道——高优先级插队；同级 FIFO；
 * 超时让位；tryAcquire 不越线；观测快照；并发守恒烟测。
 */
class PriorityLaneTest {

    @Test
    void shouldTryAcquireUpToPermits_withoutJumpingQueue() {
        PriorityLane lane = new PriorityLane(2);
        assertThat(lane.tryAcquire(0)).isTrue();
        assertThat(lane.tryAcquire(9)).isTrue();
        assertThat(lane.tryAcquire(0)).isFalse(); // 许可尽

        assertThatThrownBy(() -> new PriorityLane(1).acquire(10, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldLetHigherPriorityJumpAheadOfQueuedLower() throws Exception {
        PriorityLane lane = new PriorityLane(1);
        assertThat(lane.tryAcquire(0)).isTrue(); // 主线程占住

        List<String> order = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch queued = new CountDownLatch(2);
        try {
            Future<?> lowPrio = pool.submit(() -> {
                try {
                    lane.acquire(5, Duration.ofSeconds(10));
                    order.add("p5");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            awaitQueued(lane, 1);
            Future<?> highPrio = pool.submit(() -> {
                try {
                    lane.acquire(1, Duration.ofSeconds(10));
                    order.add("p1");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            awaitQueued(lane, 2);

            lane.release(); // 授权 p1（插队：跳过已排队的 p5）
            highPrio.get(5, TimeUnit.SECONDS); // 完成屏障——授权顺序=完成序（逐次 release）
            lane.release(); // → p5
            lowPrio.get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(order).containsExactly("p1", "p5");
    }

    @Test
    void shouldKeepFifoWithinSamePriority() throws Exception {
        PriorityLane lane = new PriorityLane(1);
        assertThat(lane.tryAcquire(0)).isTrue();
        List<String> order = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<?> a = pool.submit(acquirer(lane, 5, "a", order));
            awaitQueued(lane, 1);
            Future<?> b = pool.submit(acquirer(lane, 5, "b", order));
            awaitQueued(lane, 2);
            Future<?> c = pool.submit(acquirer(lane, 1, "c", order)); // 插队者
            awaitQueued(lane, 3);

            lane.release(); // 授权 c（p1 插队最前）
            c.get(5, TimeUnit.SECONDS);
            lane.release(); // 授权 a（同级 FIFO）
            a.get(5, TimeUnit.SECONDS);
            lane.release(); // 授权 b
            b.get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(order).containsExactly("c", "a", "b"); // p1 插队；同级 a 先 b 后
    }

    @Test
    void shouldYieldOnTimeout_andExposeWaitingSnapshot() throws Exception {
        PriorityLane lane = new PriorityLane(1);
        assertThat(lane.tryAcquire(0)).isTrue();

        AtomicBoolean timedOut = new AtomicBoolean(false);
        Thread waiter = new Thread(() -> {
            try {
                lane.acquire(9, Duration.ofMillis(150));
            } catch (TimeoutException e) {
                timedOut.set(true);
            } catch (InterruptedException ignored) {
            }
        });
        waiter.start();
        awaitQueued(lane, 1);
        assertThat(lane.waitingByPriority()).containsEntry(9, 1);

        waiter.join(3000);
        assertThat(timedOut).isTrue();
        assertThat(lane.waitingByPriority()).isEmpty(); // 让位——队列不残留
    }

    @Test
    void shouldConservePermits_underConcurrentTraffic() throws Exception {
        PriorityLane lane = new PriorityLane(3);
        ExecutorService pool = Executors.newFixedThreadPool(12);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 60; i++) {
                final int prio = i % 10;
                futures.add(pool.submit(() -> {
                    try {
                        lane.acquire(prio, Duration.ofSeconds(10));
                        Thread.sleep(2);
                    } catch (TimeoutException e) {
                        throw new AssertionError("10s 内未取得许可——死锁或饥饿", e);
                    } catch (InterruptedException ignored) {
                    } finally {
                        lane.release();
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        // 守恒：全部归还后 3 许可可立即取满
        assertThat(lane.tryAcquire(0)).isTrue();
        assertThat(lane.tryAcquire(0)).isTrue();
        assertThat(lane.tryAcquire(0)).isTrue();
        assertThat(lane.tryAcquire(0)).isFalse();
        assertThat(lane.waitingByPriority()).isEmpty();
    }

    private static Runnable acquirer(PriorityLane lane, int priority, String tag, List<String> order) {
        return () -> {
            try {
                lane.acquire(priority, Duration.ofSeconds(10));
                order.add(tag);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /** 等到等待队列达到目标深度（轮询——时序测试确定性入口）。 */
    private static void awaitQueued(PriorityLane lane, int depth) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Map<Integer, Integer> snapshot = lane.waitingByPriority();
            int total = snapshot.values().stream().mapToInt(Integer::intValue).sum();
            if (total >= depth) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("等待队列未达深度 " + depth + "：" + lane.waitingByPriority());
    }
}
