package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * 双阈值迟滞水位门（spec 2036 / T3173 / impl 1587）——Netty write
 * buffer watermark 思想：单阈值门的已知病是抖动——水位在阈值附近波动
 * 时 writable 反复翻转（背压开开关关）；迟滞双阈值根治——**超过高水位
 * 停写**（writable=false，生产者暂停），**泄到低水位才恢复**（≤ LOW），
 * 两阈值之间是保持区（状态延续，不翻转）。toggleCount 翻转计数显形
 * 抖动（频繁翻转 = 两阈值设太近的诊断信号）。
 *
 * <p>synchronized 小临界区；纯记账无副作用（停/续动作归调用方）。
 */
public final class HysteresisWatermark {

    private final long highWatermark;
    private final long lowWatermark;
    private long pending;
    private boolean writable = true;
    private long toggleCount;

    /** 契约：low ≥ 0、high &gt; low（fail-fast——两阈值重合即退化单阈值抖动门）。 */
    public HysteresisWatermark(long lowWatermark, long highWatermark) {
        if (lowWatermark < 0) {
            throw new IllegalArgumentException("lowWatermark 须 ≥ 0：" + lowWatermark);
        }
        if (highWatermark <= lowWatermark) {
            throw new IllegalArgumentException("highWatermark 须 > low："
                    + highWatermark + " ≤ " + lowWatermark);
        }
        this.lowWatermark = lowWatermark;
        this.highWatermark = highWatermark;
    }

    /** 记写入（pending 累加）：超 HIGH 停写（writable=false，toggleCount++）。契约：bytes ≥ 0。 */
    public synchronized void onWrite(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes 须 ≥ 0：" + bytes);
        }
        pending += bytes;
        if (writable && pending > highWatermark) {
            writable = false; // 停写——超 HIGH
            toggleCount++;
        }
    }

    /** 记泄出（pending 递减钳 0）：泄到 ≤ LOW 才恢复（writable=true，toggleCount++）。契约：bytes ≥ 0。 */
    public synchronized void onDrain(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes 须 ≥ 0：" + bytes);
        }
        pending = Math.max(0L, pending - bytes);
        if (!writable && pending <= lowWatermark) {
            writable = true; // 恢复——泄到 LOW（迟滞：HIGH 与 LOW 间保持）
            toggleCount++;
        }
    }

    /** 当前是否可写（true = 生产者继续；false = 停）。 */
    public synchronized boolean isWritable() {
        return writable;
    }

    /** 当前积压字节数（水位读数）。 */
    public synchronized long pendingBytes() {
        return pending;
    }

    /** 停/续翻转计数（抖动显形——频繁翻转即两阈值过近）。 */
    public synchronized long toggleCount() {
        return toggleCount;
    }
}
