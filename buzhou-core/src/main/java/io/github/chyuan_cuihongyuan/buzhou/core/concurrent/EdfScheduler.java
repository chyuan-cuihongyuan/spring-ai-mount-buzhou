package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * 最早截止期优先队列（spec 3003 / T5007 / impl 2004）——EDF 思想
 * （实时调度经典：单处理器可调度最优——截止期最近者先出）：截止期
 * 升序 + 同截止期按入队序 FIFO（tie-break 确定性，免同刻抖动）；
 * laxity 余量读数 = 截止期 − now（告负即已错过）。轮次 deadline /
 * 工具超时 / 排空窗统一到截止期序单队列——「谁最先到期」免各处
 * 自扫比较。
 *
 * <p>非线程安全（单消费者口径）；poll/peek 空队列返回 null（JDK
 * Queue 同约定，诚实边界不抛）。
 */
public final class EdfScheduler {

    /** 空队列的 nextDeadline 口径：+∞（无最近截止期）。 */
    public static final long NO_DEADLINE = Long.MAX_VALUE;

    /** 在队任务（截止期 + 入队序 + 标识）。 */
    public record Pending(long deadline, long sequence, String id) {
    }

    private final Queue<Pending> queue = new PriorityQueue<>(
            Comparator.comparingLong(Pending::deadline).thenComparingLong(Pending::sequence));
    private long nextSequence;

    /** 入队（截止期可为负——已过期任务照常排队，出队即最先）。 */
    public void offer(long deadline, String id) {
        queue.add(new Pending(deadline, nextSequence++, id));
    }

    /** 出队最早截止期者（同截止期先入先出；空返回 null）。 */
    public Pending poll() {
        return queue.poll();
    }

    /** 只读看最早截止期者（不出队；空返回 null）。 */
    public Pending peek() {
        return queue.peek();
    }

    /** 最早截止期（空队列 +∞ 口径——比较语义恒安全）。 */
    public long nextDeadline() {
        Pending head = queue.peek();
        return head == null ? NO_DEADLINE : head.deadline();
    }

    /** 队首余量 = 最早截止期 − now（负即已错过——超期可判）。 */
    public long headLaxity(long now) {
        return nextDeadline() - now;
    }

    /** 在队数。 */
    public int size() {
        return queue.size();
    }

    /** 空队列判定。 */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
