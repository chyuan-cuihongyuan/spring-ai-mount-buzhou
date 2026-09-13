package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 导出体积去向审计（spec 738 / T1076，存储成本归因思想）：导出文档的
 * 字符量按段归因（消息/摘要/状态/各扩展段）——「导出为什么这么大」的
 * 治理证据面。
 *
 * <p>纯函数：字符口径（JSON 序列化后的实际大小可用 toJson().length() 交叉
 * 验证——本面按组件字符长度估算，口径稳定）。扩展段按名分行。
 */
public final class SessionExportSizeAudit {

    /** 单段行（段名 → 字符量；extensions 按扩展名分行）。 */
    public record Segment(String segment, long chars, double share) {
    }

    /** 不可变报告（segments 除总行外按 chars 降序）。 */
    public record Report(List<Segment> segments, long totalChars) {
    }

    private SessionExportSizeAudit() {
    }

    /** 归因（null fail-fast；空导出 = 空 segments）。 */
    public static Report analyze(SessionExport export) {
        Objects.requireNonNull(export, "export");
        Map<String, Long> parts = new LinkedHashMap<>();
        long messages = 0;
        for (BuzhouMessage message : export.messages()) {
            messages += message.content() == null ? 0 : message.content().length();
        }
        if (messages > 0) {
            parts.put("messages", messages);
        }
        if (export.summary() != null) {
            parts.put("summary", 128L); // 摘要结构体固定小头——精确值需序列化，标记存在性
        }
        long stateChars = 0;
        for (Map.Entry<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry> e
                : export.state().entrySet()) {
            stateChars += e.getKey().length()
                    + (e.getValue().value() == null ? 0 : e.getValue().value().length());
        }
        if (stateChars > 0) {
            parts.put("state", stateChars);
        }
        for (Map.Entry<String, String> ext : export.extensions().entrySet()) {
            long chars = ext.getKey().length() + (ext.getValue() == null ? 0 : ext.getValue().length());
            if (chars > 0) {
                parts.merge("ext:" + ext.getKey(), chars, Long::sum);
            }
        }
        long total = parts.values().stream().mapToLong(Long::longValue).sum();
        List<Segment> segments = new ArrayList<>(parts.size());
        parts.forEach((segment, chars) -> segments.add(new Segment(segment, chars,
                total == 0 ? 0.0 : (double) chars / total)));
        segments.sort(Comparator.comparingLong(Segment::chars).reversed().thenComparing(Segment::segment));
        return new Report(List.copyOf(segments), total);
    }
}
