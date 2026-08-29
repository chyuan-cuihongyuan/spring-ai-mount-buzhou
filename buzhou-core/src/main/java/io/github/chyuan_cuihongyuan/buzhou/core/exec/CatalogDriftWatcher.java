package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 工具目录漂移看门狗（spec 201 / T573）：首拍建基线（不发事件——装配期变化
 * 是常态）；后续 {@link #check} 重拍指纹 diff——非空即发
 * {@code tool.catalog.drifted} 事件（三分类 + 新旧摘要）+ 基线推进 + 计数。
 * 内部复用 {@link ToolCatalogFingerprint}（spec 175）零新哈希；check 由宿主
 * 定时/事件触发（本类无调度）。
 */
public final class CatalogDriftWatcher {

    public static final String EVENT_DRIFTED = "tool.catalog.drifted";
    static final String DRIFTED_COUNTER = "buzhou.catalog.drifted";

    private final Consumer<Map<String, Object>> emitter;
    private ToolCatalogFingerprint baseline;

    public CatalogDriftWatcher(Consumer<Map<String, Object>> emitter) {
        this.emitter = emitter == null ? payload -> {} : emitter;
    }

    /**
     * 重拍并比对基线：首拍建基线返回 empty；变化返回 diff（并已发事件、
     * 基线已推进）；无变化返回 empty。
     */
    public Optional<ToolCatalogFingerprint.Diff> check(List<ToolDefinition> current) {
        ToolCatalogFingerprint now = ToolCatalogFingerprint.of(current);
        if (baseline == null) {
            baseline = now; // 首拍只建基线——不是事件
            return Optional.empty();
        }
        ToolCatalogFingerprint.Diff diff = baseline.diff(now);
        if (!diff.isEmpty()) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                    .metrics().counter(DRIFTED_COUNTER, 1);
            emitter.accept(Map.of(
                    "added", diff.added(),
                    "removed", diff.removed(),
                    "changed", diff.changed(),
                    "oldSummary", baseline.summaryHex(),
                    "newSummary", now.summaryHex()));
            baseline = now; // 基线推进——再变再报不重放旧闻
        }
        return diff.isEmpty() ? Optional.empty() : Optional.of(diff);
    }

    /** 当前基线摘要（观测/测试；未建 = null）。 */
    public String baselineSummary() {
        return baseline == null ? null : baseline.summaryHex();
    }
}
