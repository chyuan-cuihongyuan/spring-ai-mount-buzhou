package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * Prefetch 信用窗口（spec 1810 / T2821 / impl 1411）——RabbitMQ basic.qos
 * prefetch / AMQP credit-based flow control 思想：消费侧以**未确认（in-flight）
 * 配额**反向节流生产侧——窗口满即停取（tryAcquire 拒绝），确认归还信用
 * （release）才续流。与令牌桶（按速率补充）不同，信用窗口按**在飞上限**
 * 节流：下游处理多快，上游就能发多快，天然免速率估计。窗口耗拒计数
 * （totalExhausted）即流控压力频率读数。
 *
 * <p>线程安全（synchronized 小临界区——窗口账目本身就是热点路径）；配额
 * 静态（动态调整归宿主重建窗口）。
 */
public final class PrefetchCreditWindow {

    /** 窗口容量下限（RabbitMQ prefetch 至少 1——0 是无限，语义不同）。 */
    public static final int MIN_CAPACITY = 1;

    private final int capacity;
    private int inFlight;
    private long totalExhausted;

    /** 契约：capacity ≥ {@link #MIN_CAPACITY}（fail-fast）。 */
    public PrefetchCreditWindow(int capacity) {
        if (capacity < MIN_CAPACITY) {
            throw new IllegalArgumentException(
                    "capacity 不能小于 " + MIN_CAPACITY + "：" + capacity);
        }
        this.capacity = capacity;
    }

    /** 取一条消息的信用：满窗即拒（拒绝计入 totalExhausted——流控压力读数）。 */
    public synchronized boolean tryAcquire() {
        if (inFlight >= capacity) {
            totalExhausted++;
            return false;
        }
        inFlight++;
        return true;
    }

    /** 确认归还一条信用（无在飞可归即 fail-fast——确认必须对应已取消息）。 */
    public synchronized void release() {
        if (inFlight == 0) {
            throw new IllegalStateException("无在飞信用可归还（release 必须对应已 acquire）");
        }
        inFlight--;
    }

    /** 只读快照（容量/在飞/可用/累计耗拒）。 */
    public synchronized Snapshot stats() {
        return new Snapshot(capacity, inFlight, capacity - inFlight, totalExhausted);
    }

    /** @param utilization 在飞占比 = inFlight/capacity（窗口吃多满） */
    public record Snapshot(int capacity, int inFlight, int available, long totalExhausted) {

        public double utilization() {
            return (double) inFlight / capacity;
        }
    }
}
