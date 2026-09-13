package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 选举竞争读数（spec 838 / T1175，Redisson RedLock 竞争统计思想；839 选举
 * 后端的观测姊妹面）：tryAcquireOrRenew 的四种结果计数（获选/续期保位/
 * 他主在位让位/失位）+竞争烈度（失位率）——「选主抖不抖、谁是常任主」
 * 从连接日志考古变计数面。
 *
 * <p>纯记账（AtomicLong 原子计数）；record 由选举器包装/装配侧在
 * {@code tryAcquireOrRenew} 返回后喂（不改选举行为）。{@code Leadership}
 * 形状由调用方判别后按四态归类喂入。
 */
public final class LeaderElectionStats {

    /** 选举结果四态。 */
    public enum Outcome { ACQUIRED, RENEWED, OTHER_HOLDER, LOST }

    private final AtomicLong acquired = new AtomicLong();
    private final AtomicLong renewed = new AtomicLong();
    private final AtomicLong otherHolder = new AtomicLong();
    private final AtomicLong lost = new AtomicLong();

    /** 记录一次选举尝试结果（null 忽略）。 */
    public void record(Outcome outcome) {
        if (outcome == null) {
            return;
        }
        switch (outcome) {
            case ACQUIRED -> acquired.incrementAndGet();
            case RENEWED -> renewed.incrementAndGet();
            case OTHER_HOLDER -> otherHolder.incrementAndGet();
            case LOST -> lost.incrementAndGet();
        }
    }

    /** 竞争烈度 = (OTHER_HOLDER+LOST)/(总尝试)——0 稳态、趋 1 激烈抖动。 */
    public double contentionRatio() {
        long total = totalAttempts();
        return total == 0 ? 0 : (double) (otherHolder.get() + lost.get()) / total;
    }

    /** 总尝试次数。 */
    public long totalAttempts() {
        return acquired.get() + renewed.get() + otherHolder.get() + lost.get();
    }

    public long acquired() {
        return acquired.get();
    }

    public long renewed() {
        return renewed.get();
    }

    public long otherHolder() {
        return otherHolder.get();
    }

    public long lost() {
        return lost.get();
    }
}
