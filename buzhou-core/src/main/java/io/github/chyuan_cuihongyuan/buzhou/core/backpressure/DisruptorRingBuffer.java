package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Disruptor 环形缓冲（spec 5008 / T6117 / impl 2159）——LMAX
 * Disruptor 思想：预分配槽数组（容量须 2 的幂，`sequence &
 * mask` 免模导航）+ **claim/publish 两段式序标** + 槽级序号簿
 * （判定槽上数据属于哪个序号）。`claim()` 认领序号 →
 * `publish(sequence, item)` 严格按发布序填槽（环满 ISE——
 * 消费侧过慢诚实上抛）；`offer(item)` 覆盖式快捷（环满覆盖
 * 最旧未消费槽）；`tryConsume` 非阻塞读取（过期/未发布返回
 * null）。有界队列每次入/出队分配节点（GC 压力）的病解。
 *
 * <p>单生产者单消费者口径（多消费者 gating 留后，诚实入档）。
 */
public final class DisruptorRingBuffer<T> {

    private final Object[] slots;
    private final long[] sequenceBook;
    private final int mask;
    private final AtomicLong claimCursor = new AtomicLong();
    private final AtomicLong publishCursor = new AtomicLong();

    /** 定构（容量须 2 的幂且 ≥2，否则 IAE fail-fast）。 */
    public DisruptorRingBuffer(int capacity) {
        if (capacity < 2 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("容量须为 2 的幂：" + capacity);
        }
        this.slots = new Object[capacity];
        this.sequenceBook = new long[capacity];
        this.mask = capacity - 1;
    }

    /** 认领下一序号（发布前不占数据）。 */
    public long claim() {
        return claimCursor.getAndIncrement();
    }

    /**
     * 发布已认领序号（严格按发布序；目标槽仍持有**可读窗口内**
     * 数据即环满 ISE——消费侧过慢诚实上抛不覆盖）。
     */
    public void publish(long sequence, T item) {
        if (item == null) {
            throw new IllegalArgumentException("item 非 null");
        }
        long current = publishCursor.get();
        if (sequence != current) {
            throw new IllegalArgumentException("publish 需按发布序（期待 "
                    + current + " 实得 " + sequence + "）");
        }
        int slot = (int) (sequence & mask);
        long oldest = sequenceBook[slot];
        long earliest = Math.max(0, current - slots.length);
        boolean slotHoldsReadableItem = oldest != sequence && oldest >= earliest && oldest < current;
        if (slotHoldsReadableItem) {
            throw new IllegalStateException("环满——消费侧过慢（槽 "
                    + slot + " 仍承载可读序号 " + oldest + "）");
        }
        store(sequence, item);
        publishCursor.incrementAndGet();
    }

    /** 一步式覆盖快捷（环满自动覆盖最旧未消费槽——终留最近窗口）。 */
    public void offer(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item 非 null");
        }
        long sequence = claimCursor.getAndIncrement();
        store(sequence, item);
        long target = sequence + 1;
        publishCursor.updateAndGet(current -> Math.max(current, target));
    }

    /**
     * 非阻塞消费（序号未发布或已被覆盖返回 null——诚实缺口）。
     */
    @SuppressWarnings("unchecked")
    public T tryConsume(long sequence) {
        long earliest = Math.max(0, publishCursor.get() - slots.length);
        if (sequence < earliest || sequence >= publishCursor.get()) {
            return null;
        }
        int slot = (int) (sequence & mask);
        if (sequenceBook[slot] != sequence) {
            return null;   // 槽已被更新的序号覆盖
        }
        return (T) slots[slot];
    }

    /** 可消费的最早序号读数（覆盖语义下最旧窗口起点）。 */
    public long earliestAvailable() {
        return Math.max(0, publishCursor.get() - slots.length);
    }

    /** 已发布数读数。 */
    public long published() {
        return publishCursor.get();
    }

    /** 已认领数读数。 */
    public long claimed() {
        return claimCursor.get();
    }

    /** 容量读数。 */
    public int capacity() {
        return slots.length;
    }

    private void store(long sequence, T item) {
        int slot = (int) (sequence & mask);
        slots[slot] = item;
        sequenceBook[slot] = sequence;
    }
}
