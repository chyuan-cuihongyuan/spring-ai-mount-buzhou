package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 单调队列滑窗极值（spec 2061 / T3223 / impl 1612）——单调双端队列
 * 经典算法思想：定长滑窗内最大/最小值的 O(1) 摊销查询——入队时弹掉
 * 永无出头之日的元素（求 max：≤ 新者的队尾全弹——它们更老且不大于
 * 新者，窗口滑动后也轮不到），队首恒为当前窗极值。尖峰检测（滑动
 * 最大延迟）/谷底检测（滑动最小配额）的流式底座。
 *
 * <p>元素带序号自滑出（无需显式窗口数组）；synchronized 小临界区。
 */
public final class SlidingExtremum {

    /** 带序号元素（序号用于过期滑出）。 */
    private record Indexed(double value, long seq) {
    }

    private final boolean maximum; // true 求 max（队首最大）；false 求 min
    private final int window;
    private final Deque<Indexed> deque = new ArrayDeque<>();
    private long seqCounter;

    /** 契约：window ≥ 1；maximum=false 即最小值口径（fail-fast）。 */
    public SlidingExtremum(int window, boolean maximum) {
        if (window < 1) {
            throw new IllegalArgumentException("window 须 ≥ 1：" + window);
        }
        this.window = window;
        this.maximum = maximum;
    }

    /** 推入一个值（隐式推进序号），返回**当前窗极值**。契约：value 有限非 NaN。 */
    public synchronized double offer(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("value 须有限非 NaN：" + value);
        }
        long seq = seqCounter++;
        // 弹掉永无出头之日的队尾（max：≤ 新者；min：≥ 新者）
        while (!deque.isEmpty() && (maximum
                ? deque.peekLast().value() <= value
                : deque.peekLast().value() >= value)) {
            deque.pollLast();
        }
        deque.addLast(new Indexed(value, seq));
        // 队首过期滑出（窗宽内保留）
        while (deque.peekFirst().seq() <= seq - window) {
            deque.pollFirst();
        }
        return deque.peekFirst().value();
    }

    /** 当前窗极值（空窗 NaN——首值前）。 */
    public synchronized double current() {
        Indexed head = deque.peekFirst();
        return head == null ? Double.NaN : head.value();
    }

    /** 窗内候选数（≤ window——被压制弹出的不在内）。 */
    public synchronized int size() {
        return deque.size();
    }
}
