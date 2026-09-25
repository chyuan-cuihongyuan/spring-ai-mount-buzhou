package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MPSC 有界队列（spec 6024 / T6249 / impl 2225）——
 * JCTools MpscArrayQueue 思想：**多生产者单消费者有界环**
 * ——生产端互斥占位（同一监视器保护 claim+写入的可见性），
 * 消费端单线程无锁推进（volatile 序号 acquire 读+惰性置位）
 * ——生产者仅推进 producerIndex，消费者仅推进 consumerIndex
 * （两侧序号单调，FIFO 有界）——无界队列积压不可见与双锁
 * 队列消费端争用的病解。容量满 offer 拒新（false）。
 *
 * <p>与 DisruptorRingBuffer（spec 5008）同族不同面：多生产
 * 者有界 FIFO vs 单生产者事件派环；与 BoundedMailbox（spec
 * 5020）不同面：并发生产端 vs 策略性溢出。
 */
public final class MpscQueue<E> {

    private final Object[] buffer;
    private final int capacity;
    private final Object producerLock = new Object();
    private final AtomicLong producerIndex = new AtomicLong();
    private final AtomicLong consumerIndex = new AtomicLong();

    /** 有界队列（capacity≤0 fail-fast）。 */
    public MpscQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("容量必须为正: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /** 入队（满拒新 false；null fail-fast）。 */
    public boolean offer(E element) {
        if (element == null) {
            throw new IllegalArgumentException("元素非空");
        }
        synchronized (producerLock) {
            long producer = producerIndex.get();
            if (producer - consumerIndex.get() >= capacity) {
                return false;
            }
            buffer[(int) (producer % capacity)] = element;
            producerIndex.set(producer + 1);
            return true;
        }
    }

    /** 出队（单消费者线程调用；空返回 null）。 */
    @SuppressWarnings("unchecked")
    public E poll() {
        long consumer = consumerIndex.get();
        if (consumer >= producerIndex.get()) {
            return null;
        }
        int slot = (int) (consumer % capacity);
        E element = (E) buffer[slot];
        buffer[slot] = null;
        consumerIndex.set(consumer + 1);
        return element;
    }

    /** 当前元素数读数。 */
    public int size() {
        long producer = producerIndex.get();
        long consumer = consumerIndex.get();
        return (int) Math.max(0, producer - consumer);
    }

    /** 容量读数。 */
    public int capacity() {
        return capacity;
    }

    /** 是否空。 */
    public boolean isEmpty() {
        return size() == 0;
    }

    /** 仅限审计：内部缓冲占用视图。 */
    long[] indexSnapshot() {
        return new long[]{producerIndex.get(), consumerIndex.get()};
    }

    @Override
    public String toString() {
        return "MpscQueue{capacity=" + capacity + ", size=" + size() + "}";
    }

    static long[] sortedCopy(long[] input) {
        long[] out = Arrays.copyOf(input, input.length);
        Arrays.sort(out);
        return out;
    }
}
