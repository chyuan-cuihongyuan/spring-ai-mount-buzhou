package io.github.chyuan_cuihongyuan.buzhou.memory;

import java.util.List;

/**
 * 记忆分层容量读数（spec 816 / T1133，MemGPT/Letta 记忆分层 core/archival/
 * recall 借鉴）：三层（九段摘要=core、事实库=archival、消息台账=recall）
 * 的条目/字符量与可选容量上限的水位分级——「记忆为什么这么贵/哪层快满」
 * 从猜变表。
 *
 * <p>纯函数：快照（各层计数与字符）由调用方从 store 采集（零 store 侵入）；
 * 上限 ≤0/缺失 = 不设限（fillRatio null、恒 OK）；分级 OK &lt;80% ≤ WARN &lt;
 * 100% ≤ FULL（达到即超限）。字符口径与 738/808 一致。
 */
public final class MemoryHierarchyCapacity {

    /** 水位分级。 */
    public static final String LEVEL_OK = "OK";
    /** 警戒水位标签。 */
    public static final String LEVEL_WARN = "WARN";
    /** 满仓标签。 */
    public static final String LEVEL_FULL = "FULL";

    /** 单层读数（capChars ≤0 表示不设限——fillRatio/level 为 null）。 */
    public record LayerUsage(String layer, long items, long chars, Long capChars,
                             Double fillRatio, String level) {
    }

    /** 采集快照（store 侧计数——调用方填充）。 */
    public record Snapshot(long summaryChars, int factItems, long factChars,
                           int messageItems, long messageChars) {
    }

    /** 不可变报告（层序固定 core→archival→recall）。 */
    public record Report(List<LayerUsage> layers, long totalChars) {
    }

    private MemoryHierarchyCapacity() {
    }

    /**
     * 分层容量分析：summaryCapChars/factsCapChars/recallCapChars ≤0 = 该层
     * 不设限；负计数按 0 计（快照脏数据不炸读数）。
     */
    public static Report analyze(Snapshot snapshot,
                                 long summaryCapChars, long factsCapChars, long recallCapChars) {
        long summaryChars = Math.max(0, snapshot.summaryChars());
        long factChars = Math.max(0, snapshot.factChars());
        long messageChars = Math.max(0, snapshot.messageChars());

        List<LayerUsage> layers = List.of(
                layer("core-summary", Math.max(1, 1), summaryChars, summaryCapChars),
                layer("archival-facts", Math.max(0, snapshot.factItems()), factChars, factsCapChars),
                layer("recall-window", Math.max(0, snapshot.messageItems()), messageChars, recallCapChars));
        return new Report(layers, summaryChars + factChars + messageChars);
    }

    private static LayerUsage layer(String name, long items, long chars, long capChars) {
        if (capChars <= 0) {
            return new LayerUsage(name, items, chars, capChars <= 0 ? null : capChars, null, LEVEL_OK);
        }
        double ratio = (double) chars / capChars;
        String level = ratio >= 1.0 ? LEVEL_FULL : ratio >= 0.8 ? LEVEL_WARN : LEVEL_OK;
        return new LayerUsage(name, items, chars, capChars, ratio, level);
    }
}
