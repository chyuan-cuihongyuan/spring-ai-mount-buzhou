package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded Mailbox 有界信箱（spec 5020 / T6141 / impl 2171）——
 * Akka bounded mailbox 思想：容量信箱 + **显式溢出策略**——
 * `DROP_NEWEST` 满时拒新（offer 返回 false）、`DROP_OLDEST`
 * 满时逐最旧纳新（droppedCount++ 且 offer 返回 true——新消息
 * 不因积压被拒）。无界队列（积压不可见 OOM 隐患）与静默
 * 丢弃（丢了什么不知道）的病解；size/droppedCount 读数——
 * 积压与丢弃可见。
 *
 * <p>与 DisruptorRingBuffer（spec 5008）同族不同面：预分配环
 * 零分配 vs 策略性溢出信箱。
 */
public final class BoundedMailbox<T> {

    /** 溢出策略。 */
    public enum OverflowPolicy {
        DROP_NEWEST, DROP_OLDEST
    }

    private final int capacity;
    private final OverflowPolicy policy;
    private final Deque<T> queue = new ArrayDeque<>();
    private long droppedCount;

    /** 定构（capacity ≤0 / null policy fail-fast）。 */
    public BoundedMailbox(int capacity, OverflowPolicy policy) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity>0：" + capacity);
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy 非 null");
        }
        this.capacity = capacity;
        this.policy = policy;
    }

    /**
     * 投递（未满入队返回 true；满按策略——DROP_NEWEST 拒新
     * 返回 false，DROP_OLDEST 逐最旧纳新返回 true）。
     */
    public boolean offer(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item 非 null");
        }
        if (queue.size() < capacity) {
            queue.addLast(item);
            return true;
        }
        if (policy == OverflowPolicy.DROP_OLDEST) {
            queue.pollFirst();
            droppedCount++;
            queue.addLast(item);
            return true;
        }
        droppedCount++;   // DROP_NEWEST——拒新也计数（丢弃可见）
        return false;
    }

    /** 取出头部消息（空返回 null）。 */
    public T poll() {
        return queue.pollFirst();
    }

    /** 当前积压数读数。 */
    public int size() {
        return queue.size();
    }

    /** 丢弃累计读数（含拒新与逐旧）。 */
    public long droppedCount() {
        return droppedCount;
    }

    /** 容量读数。 */
    public int capacity() {
        return capacity;
    }

    /** 溢出策略读数。 */
    public OverflowPolicy policy() {
        return policy;
    }
}
