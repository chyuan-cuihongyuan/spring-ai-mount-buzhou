package io.github.chyuan_cuihongyuan.buzhou.observability;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 观测管道内存限流器（spec 812 / T1125，OpenTelemetry Collector
 * {@code memory_limiter} processor 借鉴）：对在途观测项按<b>估算权重</b>
 * （字节或字符——单位由调用方定义）做总量上限判定——超限拒收并计数，
 * 消费方按拒收信号决定丢弃/降级/背压（拒绝语义 = 信号不执行——本类
 * 只记账不执行策略，与 OTel refuse-and-log 口径一致）。
 *
 * <p>严格模式（soft→refuse 同一步：inFlight + weight &gt; max 即拒）；
 * 单项权重本身超上限也拒（永不无限占账）；release 归账；权重 ≤0 忽略。
 * 纯内存原子计数——无队列无线程。
 */
public final class PipelineMemoryLimiter {

    private final long maxBytes;
    private final AtomicLong inFlight = new AtomicLong();
    private final AtomicLong admitted = new AtomicLong();
    private final AtomicLong refused = new AtomicLong();
    private final AtomicLong released = new AtomicLong();

    public PipelineMemoryLimiter(long maxBytes) {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes 必须 >= 1（当前 " + maxBytes + "）");
        }
        this.maxBytes = maxBytes;
    }

    /**
     * 申请在途额度：{@code inFlight + weight <= max} 时入账并返回 true；
     * 超限返回 false 且<b>不记账</b>（拒收不计入在途——诚实口径）。
     * 权重 ≤0 忽略并返回 true（零权重恒过——空项不设障）。
     */
    public boolean tryAdmit(long weightBytes) {
        if (weightBytes <= 0) {
            return true;
        }
        while (true) {
            long current = inFlight.get();
            if (current + weightBytes > maxBytes) {
                refused.incrementAndGet();
                return false;
            }
            if (inFlight.compareAndSet(current, current + weightBytes)) {
                admitted.incrementAndGet();
                return true;
            }
        }
    }

    /** 归账（超发归账不会低于 0——防御 release 多于 admit 的调用方错误）。 */
    public void release(long weightBytes) {
        if (weightBytes <= 0) {
            return;
        }
        released.incrementAndGet();
        inFlight.updateAndGet(current -> Math.max(0, current - weightBytes));
    }

    /** 当前在途权重。 */
    public long currentBytes() {
        return inFlight.get();
    }

    public long maxBytes() {
        return maxBytes;
    }

    /** 不可变统计。 */
    public LimiterStats stats() {
        return new LimiterStats(admitted.get(), refused.get(), released.get(),
                inFlight.get(), maxBytes);
    }

    /** 统计行。 */
    public record LimiterStats(long admitted, long refused, long released,
                               long currentBytes, long maxBytes) {
    }
}
