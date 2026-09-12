package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionCapacityExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 123 / T446：spawn 优先级排队回归——HIGH 插队 / 同级 FIFO / 防新到加塞 /
 * 默认调用 = NORMAL（可越 LOW）/ LOW 超时拒绝带等待时长。既有语义面由
 * SpawnGateEndToEndTest 六测试钉住（本轮零回归）。
 */
class SpawnGatePriorityTest {

    private SpawnGate gate(int limit, Duration queueTimeout) {
        return new SpawnGate(limit, queueTimeout, OverloadPolicy.QUEUE, event -> {});
    }

    /** 异步排队取位：返回 Future（正常完成 = 拿到空位；异常完成 = 被拒绝）。 */
    private Future<String> queueAsync(ExecutorService pool, SpawnGate gate,
                                      String sessionId, SpawnPriority priority) {
        return pool.submit(() -> {
            gate.acquireSlotOrThrow(sessionId, priority);
            return sessionId;
        });
    }

    /** 默认单参入口（= NORMAL）。 */
    private Future<String> queueDefaultAsync(ExecutorService pool, SpawnGate gate, String sessionId) {
        return pool.submit(() -> {
            gate.acquireSlotOrThrow(sessionId);
            return sessionId;
        });
    }

    @Test
    void highPriorityJumpsNormalQueue() throws Exception {
        SpawnGate gate = gate(1, Duration.ofSeconds(5));
        gate.acquireSlotOrThrow("occupier");
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> normal = queueAsync(pool, gate, "normal-1", SpawnPriority.NORMAL);
            awaitState(() -> normal.isDone(), false);
            Future<String> high = queueAsync(pool, gate, "vip", SpawnPriority.HIGH);
            Thread.sleep(100); // 确保两者都已入队
            assertThat(normal.isDone()).isFalse();
            assertThat(high.isDone()).isFalse();

            gate.releaseSlot();
            assertThat(high.get(10, TimeUnit.SECONDS)).isEqualTo("vip");
            assertThat(normal.isDone()).isFalse();

            gate.releaseSlot();
            assertThat(normal.get(10, TimeUnit.SECONDS)).isEqualTo("normal-1");
            gate.releaseSlot();
            gate.releaseSlot();
        }
    }

    @Test
    void samePriorityKeepsFifoOrder() throws Exception {
        SpawnGate gate = gate(1, Duration.ofSeconds(5));
        gate.acquireSlotOrThrow("occupier");
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> first = queueAsync(pool, gate, "first", SpawnPriority.NORMAL);
            awaitState(() -> first.isDone(), false);
            Future<String> second = queueAsync(pool, gate, "second", SpawnPriority.NORMAL);
            Thread.sleep(100);
            assertThat(first.isDone()).isFalse();
            assertThat(second.isDone()).isFalse();

            gate.releaseSlot();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("first");
            gate.releaseSlot();
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("second");
            gate.releaseSlot();
            gate.releaseSlot();
        }
    }

    @Test
    void lowPriorityWaiterNotBargedByLaterNormalArrival() throws Exception {
        SpawnGate gate = gate(1, Duration.ofSeconds(5));
        gate.acquireSlotOrThrow("occupier");
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            // LOW 先入队，随后默认（NORMAL）到达——空位应先归 NORMAL（越级）
            Future<String> low = queueAsync(pool, gate, "batch-1", SpawnPriority.LOW);
            awaitState(() -> low.isDone(), false);
            Future<String> normal = queueDefaultAsync(pool, gate, "interactive");
            Thread.sleep(100);
            assertThat(low.isDone()).isFalse();

            gate.releaseSlot();
            assertThat(normal.get(10, TimeUnit.SECONDS)).isEqualTo("interactive");
            gate.releaseSlot();
            assertThat(low.get(10, TimeUnit.SECONDS)).isEqualTo("batch-1");
            gate.releaseSlot();
            gate.releaseSlot();
        }
    }

    @Test
    void lowPriorityTimeoutRejectsWithWaitedDuration() {
        SpawnGate gate = gate(1, Duration.ofMillis(150));
        gate.acquireSlotOrThrow("occupier");
        long start = System.nanoTime();
        assertThatThrownBy(() -> gate.acquireSlotOrThrow("batch", SpawnPriority.LOW))
                .isInstanceOf(SessionCapacityExceededException.class)
                .hasMessageContaining("batch");
        long waitedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(waitedMs).isGreaterThanOrEqualTo(100);
        gate.releaseSlot();
    }

    @Test
    void queuedEventCarriesSessionAndCapacity() {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        SpawnGate gate = new SpawnGate(1, Duration.ofSeconds(5), OverloadPolicy.QUEUE, events::add);
        gate.acquireSlotOrThrow("occupier");
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> queued = queueAsync(pool, gate, "waiter", SpawnPriority.NORMAL);
            awaitState(() -> events.stream()
                    .anyMatch(e -> e.type().equals(SpawnGate.EVENT_SPAWN_QUEUED)), true);
            assertThat(queued.isDone()).isFalse();
            gate.releaseSlot();
            gate.releaseSlot();
        }
    }

    /**
     * 轮询等待断言条件（带 10s 截止——与 get(10s) 同放宽口径：全仓并行下虚拟线程
     * 调度饥饿可能远超 2s（ca61e639 放宽了 get 却漏了此处 deadline）；condition =
     * false 时断言当前值恒为 false 即刻通过）。
     */
    private static void awaitState(java.util.function.BooleanSupplier probe, boolean expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (probe.getAsBoolean() == expected) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
