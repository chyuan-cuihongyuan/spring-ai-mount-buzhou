package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 配额空间告警门（spec 1882 / T2965 / impl 1483）——etcd NOSPACE
 * 告警语义：后端配额耗尽进入只读——写拒绝、读放行；恢复不是自愈，
 * 必须「显式确认 + 释放后空闲 ≥ 阈值」才复位。保守在：清理不足就
 * 恢复写的抖动被门物理拦住。
 *
 * <p>持态小门；读路径零锁（volatile + AtomicLong）。
 */
public final class QuotaAlarmGate {

    private final long quotaBytes;
    private final AtomicLong usedBytes;
    private volatile boolean noSpace;

    /**
     * @param quotaBytes 空间配额（≥ 1，fail-fast）
     */
    public QuotaAlarmGate(long quotaBytes) {
        if (quotaBytes < 1) {
            throw new IllegalArgumentException("quotaBytes 不能小于 1：" + quotaBytes);
        }
        this.quotaBytes = quotaBytes;
        this.usedBytes = new AtomicLong(0);
    }

    /**
     * 写路径记账：告警期一票拒绝（etcd 只读语义——哪怕装得下）；未
     * 告警时超配额的写入触发 NOSPACE 并拒绝本次。返回 false = 拒绝
     * （门保持 triggered 直到足额 acknowledge）。
     */
    public boolean onWrite(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("写入字节数不能为负：" + bytes);
        }
        if (noSpace) {
            return false;
        }
        if (usedBytes.get() + bytes > quotaBytes) {
            noSpace = true;
            return false;
        }
        usedBytes.addAndGet(bytes);
        return true;
    }

    /** 释放空间记账（清理后调用；用量下限 0）。 */
    public void onRelease(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("释放字节数不能为负：" + bytes);
        }
        usedBytes.updateAndGet(v -> Math.max(0, v - bytes));
    }

    /** 读路径永不受告警影响——etcd 只读语义的核心。 */
    public boolean readsAllowed() {
        return true;
    }

    /** 当前是否处于 NOSPACE 告警（写被拒）。 */
    public boolean writesBlocked() {
        return noSpace;
    }

    /** 用量水位读数：用量/配额（可超 1——告警后仍计入满载用量）。 */
    public double usageRatio() {
        return (double) usedBytes.get() / quotaBytes;
    }

    /**
     * 显式解除告警：仅当「配额 − 当前用量 ≥ minFreeBytes」才复位；
     * 不足额保持拒绝（防清理不彻底就恢复写的抖动）。
     */
    public boolean acknowledge(long minFreeBytes) {
        if (minFreeBytes < 0) {
            throw new IllegalArgumentException("minFreeBytes 不能为负：" + minFreeBytes);
        }
        if (quotaBytes - usedBytes.get() >= minFreeBytes) {
            noSpace = false;
            return true;
        }
        return false;
    }
}
