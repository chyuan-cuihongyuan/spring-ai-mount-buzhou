package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 追限事件会话化合并（spec 1807 / T2815 / impl 1408）——Prometheus 告警
 * 分组（group_interval 把相邻同类告警并成一条）/ Google Analytics session
 * gap（30 分钟无活动即切会话）思想：裸追限事件流按「间隔 ≤ 容忍窗」切成
 * **事件段（episode）**——10 次散点追限与 1 段持续 10 连击是两种病（前者
 * 偶发抖动、后者系统性超载），段数/最长段/压缩比把两者分开。
 *
 * <p>纯函数零状态：输入为追限时点序列（毫秒，乱序容忍——内部排序）与
 * 合并容忍窗；只读不裁决（限流动作归宿主）。
 */
public final class ViolationEpisodeMerger {

    private ViolationEpisodeMerger() {
    }

    /** 单事件段：start/end 为段内首尾时点，hits 为段内事件数；span = end−start。 */
    public record Episode(long startMillis, long endMillis, int hits) {

        public long spanMillis() {
            return endMillis - startMillis;
        }
    }

    /**
     * @param episodes    段数（散点各自成段也算）
     * @param totalHits   事件总数（分母）
     * @param longest     最长段（按 span；无事件 null）
     * @param mergedSpans 全段 span 合计（毫秒）
     */
    public record MergeReport(int episodes, long totalHits, Episode longest, long mergedSpans) {

        /** 平均段密度 = totalHits/episodes（无事件 -1 哨兵；越高越接近持续超载）。 */
        public double hitsPerEpisode() {
            return episodes == 0 ? -1d : (double) totalHits / episodes;
        }
    }

    /**
     * 会话化入口。契约：gapToleranceMillis ≥ 0、时点非 null（fail-fast）；
     * null 列表按空表。语义：排序后相邻时点差 ≤ gap 同段；单点成段（span 0）。
     */
    public static MergeReport merge(long gapToleranceMillis, List<Long> violationAtMillis) {
        if (gapToleranceMillis < 0) {
            throw new IllegalArgumentException("gapToleranceMillis 不能为负：" + gapToleranceMillis);
        }
        List<Long> points = new ArrayList<>();
        if (violationAtMillis != null) {
            for (Long t : violationAtMillis) {
                if (t == null) {
                    throw new IllegalArgumentException("追限时点不能为 null");
                }
                points.add(t);
            }
        }
        points.sort(Comparator.naturalOrder());
        int episodes = 0;
        int hits = 0;
        Episode longest = null;
        long mergedSpans = 0;
        int i = 0;
        while (i < points.size()) {
            int j = i;
            while (j + 1 < points.size() && points.get(j + 1) - points.get(j) <= gapToleranceMillis) {
                j++;
            }
            Episode episode = new Episode(points.get(i), points.get(j), j - i + 1);
            episodes++;
            hits += j - i + 1;
            mergedSpans += episode.spanMillis();
            if (longest == null || episode.spanMillis() > longest.spanMillis()) {
                longest = episode;
            }
            i = j + 1;
        }
        return new MergeReport(episodes, hits, longest, mergedSpans);
    }
}
