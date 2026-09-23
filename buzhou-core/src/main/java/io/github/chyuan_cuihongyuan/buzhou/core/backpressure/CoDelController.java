package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.LongSupplier;

/**
 * CoDel 受控延迟队列（spec 4018 / T6037 / impl 2119）——sojourn
 * 时间驱动的 AQM 思想（Nichols-Jacobson 2012，RFC 8290；Linux
 * fq_codel 同款）：只盯**驻留时间**（入队→出队时延）是否超目标
 * （target，默认 5ms）——超目标持续一个 interval（默认 100ms）
 * 即进入丢包态，此后按 interval/√n 递缩间隔持续早丢（脏数据排完
 * 后立即退出）。「bufferbloat：队列满才丢、尾丢成批同步」病的
 * 时间维度解——丢不丢看延迟不看水位，早丢小量打散同步突发。
 *
 * <p>与 RandomEarlyDrop（RED 水位概率丢包）成 AQM 双档：RED 看
 * 队列长度、CoDel 看驻留时间（对低速链路/变速流量更公平）。
 * 时钟可注入（确定性回放）。
 */
public final class CoDelController {

    private final long targetNanos;
    private final long intervalNanos;
    private final LongSupplier clockNanos;
    private long firstAboveTime = -1;   // 首次超目标的时刻（−1=在目标下）
    private long nextDropTime = -1;     // 丢包态下次丢包时刻
    private int dropCount;              // 当前丢包态已丢数（间隔递缩指数）

    /** 定构（target/interval 正数且 target<interval、时钟非 null 否则 fail-fast）。 */
    public CoDelController(long targetNanos, long intervalNanos, LongSupplier clockNanos) {
        if (targetNanos <= 0 || intervalNanos <= targetNanos || clockNanos == null) {
            throw new IllegalArgumentException("0<target<interval 且时钟非空："
                    + targetNanos + "/" + intervalNanos);
        }
        this.targetNanos = targetNanos;
        this.intervalNanos = intervalNanos;
        this.clockNanos = clockNanos;
    }

    /**
     * 出队裁决（纯逻辑——真队列/真丢包归调用方）：返回应否丢弃
     * 本条（sojourn 超目标持续一个 interval 进入丢包态、间隔按
     * interval/√n 递缩；回到目标下立即复位）。
     */
    public boolean shouldDrop(long sojournNanos) {
        long now = clockNanos.getAsLong();
        if (sojournNanos < targetNanos || sojournNanos < 0) {
            firstAboveTime = -1;   // 回到目标下——脏数据排空，复位
            dropCount = 0;
            return false;
        }
        if (firstAboveTime < 0) {
            firstAboveTime = now;   // 首次超目标——观察窗起算
            return false;
        }
        if (now - firstAboveTime < intervalNanos) {
            return false;   // 观察窗内——还不动手
        }
        if (nextDropTime < 0) {
            nextDropTime = now + intervalNanos;
            dropCount = 1;
            return true;   // 进入丢包态——首丢
        }
        if (now >= nextDropTime) {
            dropCount++;
            nextDropTime = now + intervalNanos / dropCount2Root();
            return true;   // 间隔递缩持续丢
        }
        return false;
    }

    /** 丢包态已丢计数读数（递缩指数）。 */
    public int dropCount() {
        return dropCount;
    }

    /** 目标读数。 */
    public long targetNanos() {
        return targetNanos;
    }

    /** 间隔读数。 */
    public long intervalNanos() {
        return intervalNanos;
    }

    private long dropCount2Root() {
        return Math.max(1, (long) Math.sqrt(dropCount));
    }

    /** 便捷门面：驱动的简单 FIFO + CoDel 早丢（入队/出队双面）。 */
    public static final class Queue<T> {

        private final CoDelController controller;
        private final ArrayDeque<Entry<T>> entries = new ArrayDeque<>();
        private long dropped;
        private long passed;

        private record Entry<T>(T value, long enqueuedAt) {
        }

        /** 定构（同控制器参）。 */
        public Queue(long targetNanos, long intervalNanos, LongSupplier clockNanos) {
            this.controller = new CoDelController(targetNanos, intervalNanos, clockNanos);
        }

        /** 入队。 */
        public void offer(T value) {
            if (value == null) {
                throw new IllegalArgumentException("value 非空");
            }
            entries.add(new Entry<>(value, controller.clockNanos.getAsLong()));
        }

        /** 出队（null=空；sojourn 超标的头按 CoDel 裁决丢弃并重试）。 */
        public T poll() {
            while (!entries.isEmpty()) {
                Entry<T> head = entries.poll();
                long sojourn = controller.clockNanos.getAsLong() - head.enqueuedAt();
                if (controller.shouldDrop(sojourn)) {
                    dropped++;
                    continue;   // 丢掉这条继续取下一条
                }
                passed++;
                return head.value();
            }
            return null;
        }

        /** 已丢计数。 */
        public long dropped() {
            return dropped;
        }

        /** 已过计数。 */
        public long passed() {
            return passed;
        }

        /** 在队深度。 */
        public int depth() {
            return entries.size();
        }
    }
}
