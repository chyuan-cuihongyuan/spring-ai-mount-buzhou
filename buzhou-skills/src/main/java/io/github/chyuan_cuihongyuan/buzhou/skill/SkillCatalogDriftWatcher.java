package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 技能目录漂移看门狗（spec 617 / T884，spec 201 CatalogDriftWatcher 的 skills 镜像；
 * impl 469 指纹原语的接线扩散）：首拍建基线（不发事件——装配期变化是常态）；后续
 * {@link #check} 重拍指纹 diff——非空即发 {@code skill.catalog.drifted} 事件
 * （三分类 + 新旧摘要）+ 基线推进 + 计数。check 由宿主定时/事件触发（本类无调度）。
 */
public final class SkillCatalogDriftWatcher {

    public static final String EVENT_DRIFTED = "skill.catalog.drifted";
    static final String DRIFTED_COUNTER = "buzhou.skill.catalog.drifted";

    private final Consumer<Map<String, Object>> emitter;
    private SkillCatalogFingerprint baseline;

    public SkillCatalogDriftWatcher(Consumer<Map<String, Object>> emitter) {
        this.emitter = emitter == null ? payload -> {} : emitter;
    }

    /**
     * 重拍并比对基线：首拍建基线返回 empty；变化返回 diff（并已发事件、
     * 基线已推进）；无变化返回 empty。
     */
    public Optional<SkillCatalogFingerprint.Diff> check(List<SkillMetadata> current) {
        SkillCatalogFingerprint now = SkillCatalogFingerprint.of(current);
        if (baseline == null) {
            baseline = now; // 首拍只建基线——不是事件
            return Optional.empty();
        }
        SkillCatalogFingerprint.Diff diff = baseline.diff(now);
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
