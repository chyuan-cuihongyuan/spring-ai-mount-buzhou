package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 降级链单窗视图（spec 207 / T581，Grafana 单窗思想）：链（15）+ 驱逐（149）
 * + 演练（195）三源合成一页备胎全景。纯函数只读组合——recommendedOrder =
 * 链序剔除被逐者（过期未验<b>保留标注</b>——标注与剔除分明，决策归人）。
 */
public final class FallbackChainView {

    /** 单备胎行：链位/被逐/新鲜/演练两态。 */
    public record Row(String name, int position, boolean ejected, boolean fresh,
                      java.time.Instant lastVerifiedAt, java.time.Instant lastFailedAt) {
    }

    /** 一页报告。 */
    public record Report(List<Row> rows, List<String> recommendedOrder) {
    }

    private FallbackChainView() {
    }

    /**
     * 三源合成（ejection/drill 可 null——对应能力未启用时降级为「未逐/未验证」）。
     */
    public static Report report(FallbackChain chain, ModelOutlierEjection ejection,
                                FallbackDrill drill, Duration freshMaxAge) {
        List<Row> rows = new ArrayList<>();
        List<String> recommended = new ArrayList<>();
        if (chain == null || chain.isEmpty()) {
            return new Report(List.of(), List.of()); // 空链诚实空态
        }
        List<NamedFallbackModel> models = chain.models();
        for (int i = 0; i < models.size(); i++) {
            String name = models.get(i).name();
            boolean ejected = ejection != null && ejection.isEjected(name);
            boolean fresh = drill != null && drill.isFresh(name, freshMaxAge);
            FallbackDrill.DrillState state = drill == null
                    ? null : drill.states().get(name);
            rows.add(new Row(name, i, ejected, fresh,
                    state == null ? null : state.lastVerifiedAt(),
                    state == null ? null : state.lastFailedAt()));
            if (!ejected) {
                recommended.add(name); // 过期未验保留——面板标注不剔除
            }
        }
        return new Report(List.copyOf(rows), List.copyOf(recommended));
    }
}
