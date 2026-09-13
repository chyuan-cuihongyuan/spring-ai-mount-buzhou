package io.github.chyuan_cuihongyuan.buzhou.core.export;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 导出内容去重统计（spec 808 / T1117，restic dedupe stats 借鉴——逻辑大小
 * vs 打包后大小的节省口径）：对导出内容块列表（消息正文/摘要段/状态值）
 * 统计重复块与可节省字节——「导出为什么大/去重能省多少」结构化。
 *
 * <p>纯函数：去重键 = 块的精确串值（MD5/相似度归其他族——本类只做精确
 * 重复口径，javadoc 声明）； savings = Σ (count−1)×len（每块 UTF-16 长度
 * 口径——与 SessionExportSizeAudit 的字符口径一致，非字节精确值）；
 * Top 重复块封顶 {@value #TOP_LIMIT}。空块（null/空白）不计入重复（但计入
 * totalItems 占位——输入即事实）。
 */
public final class ExportDedupeStats {

    /** Top 重复块封顶。 */
    public static final int TOP_LIMIT = 16;

    /** 单个重复块读数。 */
    public record DuplicateBlock(String preview, int occurrences, int charsWasted) {
    }

    /** 统计报告（不可变）。 */
    public record Report(int totalItems, int uniqueItems, long totalChars, long uniqueChars,
                         long duplicateChars, double savingsRatio, List<DuplicateBlock> top) {
    }

    private ExportDedupeStats() {
    }

    /**
     * 去重统计：savingsRatio = duplicateChars / totalChars（total=0 → 0）；
     * top 按 charsWasted 降序封顶 {@value #TOP_LIMIT}；preview 截 32 字符。
     */
    public static Report analyze(List<String> blocks) {
        Objects.requireNonNull(blocks, "blocks");
        Map<String, Integer> counts = new HashMap<>();
        int totalItems = 0;
        long totalChars = 0;
        for (String block : blocks) {
            totalItems++;
            if (block == null || block.isBlank()) {
                continue; // 空块不计重复，但计入 totalItems
            }
            counts.merge(block, 1, Integer::sum);
            totalChars += block.length();
        }
        int uniqueItems = counts.size();
        long uniqueChars = counts.keySet().stream().mapToLong(String::length).sum();
        long duplicateChars = totalChars - uniqueChars;

        List<DuplicateBlock> top = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > 1) {
                top.add(new DuplicateBlock(preview(e.getKey()), e.getValue(),
                        (e.getValue() - 1) * e.getKey().length()));
            }
        }
        top.sort(Comparator.comparingLong(DuplicateBlock::charsWasted).reversed());
        List<DuplicateBlock> limited = top.size() > TOP_LIMIT
                ? List.copyOf(top.subList(0, TOP_LIMIT)) : List.copyOf(top);

        double ratio = totalChars == 0 ? 0 : (double) duplicateChars / totalChars;
        return new Report(totalItems, uniqueItems, totalChars, uniqueChars,
                duplicateChars, ratio, limited);
    }

    private static String preview(String block) {
        return block.length() <= 32 ? block : block.substring(0, 32) + "…";
    }
}
