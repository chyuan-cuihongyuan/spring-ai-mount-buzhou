package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 技能漏斗读面（L 会话 1700 系 R35 = effort #1734 / spec 1734 /
 * 票 T2669 + T2670 / impl 1334）——PostHog / Amplitude 漏斗分析思想：
 * 技能生命周期「搜索 → 加载 → 应用」三级漏斗——搜索命中但没人加载 =
 * 排序/描述问题；加载了没人应用 = 内容问题；掉哪个环节，修哪个环节。
 * {@link SkillUsageStats} 管使用量，本面管环节转化。
 *
 * <p>实例面线程安全：recordSearch/recordLoad/recordApply 三计数 +
 * `census()` 吐漏斗与两级转化率（搜索为 0 时哨兵 −1）。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class SkillFunnelStats {

    private final AtomicLong searched = new AtomicLong();
    private final AtomicLong loaded = new AtomicLong();
    private final AtomicLong applied = new AtomicLong();

    /** 记一次搜索（结果非空时记账——空结果不计入漏斗）。 */
    public void recordSearch() {
        searched.incrementAndGet();
    }

    /** 记一次技能加载。 */
    public void recordLoad() {
        loaded.incrementAndGet();
    }

    /** 记一次技能应用（内容真正进入上下文/被执行）。 */
    public void recordApply() {
        applied.incrementAndGet();
    }

    /**
     * @param searched     搜索数
     * @param loaded       加载数
     * @param applied      应用数
     * @param loadRate     搜索→加载转化率（搜索 0 哨兵 −1）
     * @param applyRate    加载→应用转化率（加载 0 哨兵 −1）
     */
    public record FunnelCensus(long searched, long loaded, long applied,
                               double loadRate, double applyRate) {
    }

    /** 快照。 */
    public FunnelCensus census() {
        long searches = searched.get();
        long loads = loaded.get();
        double loadRate = searches == 0 ? -1d : (double) loads / searches;
        double applyRate = loads == 0 ? -1d : (double) applied.get() / loads;
        return new FunnelCensus(searches, loads, applied.get(), loadRate, applyRate);
    }
}
