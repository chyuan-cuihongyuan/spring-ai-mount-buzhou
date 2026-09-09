package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 优先级泳道原语（spec 411 / T713，Envoy priority levels 借鉴）：有界许可 +
 * 优先级插队——priority 数小者优先（0-9 有界）、同优先级 FIFO 保公平底线；
 * release 唤醒队首等待者（跳过所有低优先级）；超时让位（退出等待队列，
  后来高优先级照常插队）；不剥夺（已持有者不被抢占——协作式）。
 * waitingByPriority 快照 = 饥饿可见性。
 */
public final class PriorityLane {

    /** 优先级下界（最高）。 */
    public static final int MIN_PRIORITY = 0;
    /** 优先级上界（最低）。 */
    public static final int MAX_PRIORITY = 9;

    private static final class Waiter {
        final int priority;
        final long seq;
        boolean granted;

        Waiter(int priority, long seq) {
            this.priority = priority;
            this.seq = seq;
        }
    }

    private final int permits;
    private int available;
    private long seqGen;
    private final PriorityBlockingQueue<Waiter> waiters = new PriorityBlockingQueue<>(
            16, Comparator.comparingInt((Waiter w) -> w.priority).thenComparingLong(w -> w.seq));
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition granted = lock.newCondition();
    private final Map<Integer, Integer> waitingCounts = new ConcurrentHashMap<>();

    public PriorityLane(int permits) {
        if (permits < 1) {
            throw new IllegalArgumentException("permits >= 1（当前 " + permits + "）");
        }
        this.permits = permits;
        this.available = permits;
    }

    /** 取许可：占用中则排队（priority 小者优先、同级 FIFO）；超时 TimeoutException。 */
    public void acquire(int priority, Duration timeout) throws InterruptedException, TimeoutException {
        checkPriority(priority);
        long nanos = timeout.toNanos();
        lock.lockInterruptibly();
        try {
            if (available > 0 && waiters.isEmpty()) {
                available--;
                return;
            }
            Waiter me = new Waiter(priority, seqGen++);
            waiters.add(me);
            waitingCounts.merge(priority, 1, Integer::sum);
            long deadline = System.nanoTime() + nanos;
            try {
                while (!me.granted) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        throw new TimeoutException("优先级泳道等待超时（priority=" + priority
                                + "，等待队列深度 " + waiters.size() + "）");
                    }
                    granted.awaitNanos(remaining);
                }
            } finally {
                waitingCounts.compute(priority, (k, v) -> v == null || v == 1 ? null : v - 1);
                if (!me.granted) {
                    waiters.remove(me); // 超时让位——退出队列不碍后来者
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** 立即判定（不排队）：成功 false→true 语义与 Semaphore 对齐。 */
    public boolean tryAcquire(int priority) {
        checkPriority(priority);
        lock.lock();
        try {
            if (available > 0 && waiters.isEmpty()) {
                available--;
                return true;
            }
            return false; // 有等待者时不插队（尊重排队序——tryAcquire 不越线）
        } finally {
            lock.unlock();
        }
    }

    /** 归还：唤醒队首等待者（跳过低优先级）或恢复许可。 */
    public void release() {
        lock.lock();
        try {
            Waiter head = waiters.poll();
            if (head != null) {
                head.granted = true;
                granted.signalAll(); // 队首检查 O(log n)——惊群换实现简明
            } else {
                if (available < permits) {
                    available++;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** 各优先级当前等待数快照（饥饿可见性）。 */
    public Map<Integer, Integer> waitingByPriority() {
        Map<Integer, Integer> snapshot = new TreeMap<>();
        waitingCounts.forEach((k, v) -> snapshot.put(k, v));
        return snapshot;
    }

    /** 许可总数。 */
    public int permits() {
        return permits;
    }

    private static void checkPriority(int priority) {
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority 取值 [" + MIN_PRIORITY + "," + MAX_PRIORITY + "]：" + priority);
        }
    }
}
