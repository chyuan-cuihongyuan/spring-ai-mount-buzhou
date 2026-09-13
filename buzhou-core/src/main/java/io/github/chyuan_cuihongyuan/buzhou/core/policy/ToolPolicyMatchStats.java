package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.List;

/**
 * impl-753 / spec 1000：进程级匹配决策只读快照。
 *
 * <p>守恒不变量：{@link #total()} 恒等于自上次
 * {@link ToolPolicyMatcher#resetStats()} 以来 {@link ToolPolicyMatcher#match}
 * 的调用总次数（同一计数单点）。
 *
 * @param exactHits 精确名命中累计
 * @param globHits  通配模式命中累计
 * @param noneHits  未命中累计（返回空 Map 的调用）
 * @param recent    最近决策（新→旧，有界环形，容量 {@value ToolPolicyMatcher#RECENT_CAPACITY}）
 */
public record ToolPolicyMatchStats(long exactHits, long globHits, long noneHits,
        List<ToolPolicyMatchDecision> recent) {

    public ToolPolicyMatchStats {
        recent = List.copyOf(recent);
    }

    /** 三分类合计（守恒：== match 调用总次数）。 */
    public long total() {
        return exactHits + globHits + noneHits;
    }
}
