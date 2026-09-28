package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Striped Lock 条带锁（spec 6025 / T6251 / impl 2226）——
 * Guava Striped 思想：**键哈希到固定条带的锁池**——n 把
 * ReentrantLock 覆盖无限键空间（spread 混淆后按 2 的幂取
 * 模），相近键散开、热键恒同锁——每键一把锁（内存随键基数
 * 爆炸）与全局单锁（无关键互相阻塞）两种病的同解。条带数
 * 向上取 2 的幂（位掩码取模）。
 *
 * <p>与 TicketLock（spec 5006）同族不同面：单锁 FIFO 公平
 * vs 键空间分条降争用。hash 混淆确定性（同键恒同条带）。
 */
public final class StripedLock {

    private final ReentrantLock[] stripes;
    private final int mask;

    /** 条带数（向上取 2 的幂；≤0 fail-fast）。 */
    public StripedLock(int stripes) {
        if (stripes <= 0) {
            throw new IllegalArgumentException("条带数必须为正: " + stripes);
        }
        int size = 1;
        while (size < stripes) {
            size <<= 1;
        }
        this.stripes = new ReentrantLock[size];
        for (int i = 0; i < size; i++) {
            this.stripes[i] = new ReentrantLock();
        }
        this.mask = size - 1;
    }

    /** 键所在条带锁（同键恒同锁）。 */
    public Lock lockFor(long key) {
        return stripes[indexFor(key)];
    }

    /** 在键条带锁内执行（返回执行结果）。 */
    public <T> T withLock(long key, java.util.function.Supplier<T> action) {
        Lock lock = lockFor(key);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /** 在键条带锁内执行（无返回值）。 */
    public void withLock(long key, Runnable action) {
        Lock lock = lockFor(key);
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    /** 实际条带数（2 的幂）读数。 */
    public int stripeCount() {
        return stripes.length;
    }

    /** 条带下标（SplitMix64 混淆 + 位掩码——确定性）。 */
    public int indexFor(long key) {
        long z = key;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (int) (z & mask);
    }
}
