package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Ticket Lock 票据锁（spec 5006 / T6113 / impl 2157）——
 * Linux 内核 ticket spinlock 思想：`lock()` 原子取号
 * （nextTicket++）并自旋等待 nowServing 追上自己的票——
 * **先到先服务**（FIFO，饥饿结构性排除）；`unlock(ticket)`
 * 校验票号即当前号后放行（乱序 IAE fail-fast）。裸 CAS 自旋
 * （先到未必先得）与 synchronized（无排队可见性）的病解。
 *
 * <p>读数面：nowServing/nextTicket/queueLength（等待深度）；
 * 自旋对虚拟线程友好（onSpinWait + 定期 yield）；不可重入
 * （自旋锁语义——重入自死锁由调用方自查）。
 */
public final class TicketLock {

    /** 自旋期间周期性让出 CPU 的间隔（次）。 */
    private static final int YIELD_EVERY = 16;

    private final AtomicLong nextTicket = new AtomicLong();
    private final AtomicLong nowServing = new AtomicLong();

    /** 取票并自旋候号（返回本线程持有的票号）。 */
    public long lock() {
        long ticket = nextTicket.getAndIncrement();
        long spins = 0;
        while (nowServing.get() != ticket) {
            Thread.onSpinWait();
            if (++spins % YIELD_EVERY == 0) {
                Thread.yield();
            }
        }
        return ticket;
    }

    /** 放行（ticket 非当前号 IAE fail-fast）。 */
    public void unlock(long ticket) {
        long current = nowServing.get();
        if (ticket != current) {
            throw new IllegalArgumentException("乱序放行：ticket " + ticket + " ≠ nowServing " + current);
        }
        nowServing.incrementAndGet();
    }

    /** 当前服务票号读数。 */
    public long nowServing() {
        return nowServing.get();
    }

    /** 已发最大票号读数。 */
    public long nextTicket() {
        return nextTicket.get();
    }

    /** 排队深度读数（含持票在区者）。 */
    public int queueLength() {
        return (int) Math.max(0, nextTicket.get() - nowServing.get());
    }
}
