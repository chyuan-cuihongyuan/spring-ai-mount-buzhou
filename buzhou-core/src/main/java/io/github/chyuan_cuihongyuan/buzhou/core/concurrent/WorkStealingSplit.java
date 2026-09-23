package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 工作窃取对半分割（spec 4021 / T6043 / impl 2122）——调度器偷半
 * 思想（Cilk THE 协议双端队列；Go scheduler/Ray/Dask steal-half
 * 变体）：owner 一端 LIFO 热取（缓存友好——最新最深任务先跑），
 * 闲工从**另一端**冷偷（最老任务——依赖浅可独立展开）；偷取量
 * **对半**而非一个（一次性摊平负载梯度，减少偷取风暴往返），
 * 下限 minSteal 保底。
 *
 * <p>纯裁决件（真线程/真双端队列并发归运行时）：stealCount 定量
 * + stealFrom 冷端搬运。与 TwoChoiceSelector（到达时刻择短）互补：
 * 彼管「新任务去哪」，本管「存量失衡怎么搬」。
 */
public final class WorkStealingSplit {

    private final int minSteal;

    /** 定构（minSteal≥1 否则 fail-fast）。 */
    public WorkStealingSplit(int minSteal) {
        if (minSteal < 1) {
            throw new IllegalArgumentException("minSteal≥1：" + minSteal);
        }
        this.minSteal = minSteal;
    }

    /** 偷取量裁决：victimSize/2（下限 minSteal、上限 victimSize；空 0）。 */
    public int stealCount(int victimSize) {
        if (victimSize < 0) {
            throw new IllegalArgumentException("victimSize≥0：" + victimSize);
        }
        if (victimSize == 0) {
            return 0;
        }
        return Math.min(Math.max(victimSize / 2, minSteal), victimSize);
    }

    /** 冷端偷取（从 victim 尾端搬 n=stealCount 项——最老任务先离队）。 */
    public <T> List<T> stealFrom(Deque<T> victim) {
        if (victim == null) {
            throw new IllegalArgumentException("victim 非 null");
        }
        int n = stealCount(victim.size());
        List<T> stolen = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            stolen.add(victim.pollLast());
        }
        return stolen;
    }

    /** 下限读数。 */
    public int minSteal() {
        return minSteal;
    }

    /** 便捷构造（下限 1）。 */
    public static WorkStealingSplit defaults() {
        return new WorkStealingSplit(1);
    }
}
