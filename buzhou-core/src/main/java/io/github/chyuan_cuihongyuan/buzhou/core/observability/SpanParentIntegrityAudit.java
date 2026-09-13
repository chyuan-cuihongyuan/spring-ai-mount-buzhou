package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * span 父链完整性审计（spec 736 / T1072，OTel trace 树语义）：子 span 的
 * parentSpanId 必须指向同集合内存在的 span——悬空父引用（父 span 被逐出/
 * 未落库/跨 trace 串写）使 trace 树断裂，重放与依赖分析失真。
 *
 * <p>纯函数：调用方供 spans（通常单会话或单 trace 集合——跨集合引用本就
 * 不合法）。无父（parentSpanId null/空）= 根 span，合法。
 */
public final class SpanParentIntegrityAudit {

    /** 单条发现（parentId 在集合内不存在）。 */
    public record Finding(String spanId, String parentSpanId) {
    }

    /** 不可变报告（findings 按 spanId 字典序）。 */
    public record Report(List<Finding> findings, int totalSpans, int rootSpans) {
    }

    private SpanParentIntegrityAudit() {
    }

    /** 审计（null fail-fast；空表 = 零发现）。 */
    public static Report audit(List<SpanRecord> spans) {
        Objects.requireNonNull(spans, "spans");
        Set<String> ids = new HashSet<>();
        long roots = 0;
        for (SpanRecord span : spans) {
            if (span == null) {
                continue;
            }
            ids.add(span.spanId());
            if (span.parentSpanId() == null || span.parentSpanId().isBlank()) {
                roots++;
            }
        }
        List<Finding> findings = new ArrayList<>();
        for (SpanRecord span : spans) {
            if (span == null) {
                continue;
            }
            String parent = span.parentSpanId();
            if (parent != null && !parent.isBlank() && !ids.contains(parent)) {
                findings.add(new Finding(span.spanId(), parent));
            }
        }
        findings.sort(Comparator.comparing(Finding::spanId));
        return new Report(List.copyOf(findings), ids.size(), (int) roots);
    }
}
