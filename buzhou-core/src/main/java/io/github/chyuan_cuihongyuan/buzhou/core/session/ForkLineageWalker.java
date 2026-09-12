package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * fork 谱系游走环防护（spec 711 / T973，静态分析 call-graph 环检测双闸借鉴——
 * visited 集合 + 深度上限）：沿 {@link SessionForkKeys#SOURCE} 逐跳上溯读面。
 * 正常 fork 只造新会话（谱系恒为树）——环只能经导入/还原路径（spec 6）注入；
 * 消费方（面板/导出/审计）游走前经本防护即不死循环。
 *
 * <p><b>只读不修</b>：环/超深只报告不修正（处置权留给导入方裁决）。
 */
public final class ForkLineageWalker {

    /** 默认最大上溯深度（超过即 depthCapped——正常谱系远低于此）。 */
    public static final int DEFAULT_MAX_DEPTH = 64;

    /** 谱系游走结果（不可变）。 */
    public record Lineage(List<String> ancestors, boolean loopDetected, boolean depthCapped) {
    }

    private ForkLineageWalker() {
    }

    /**
     * 上溯谱系（最近→根有序）。
     *
     * @param store    会话态仓（读 SOURCE）
     * @param sessionId 起始会话（其自身不入 ancestors——只含祖先）
     * @param maxDepth 上溯深度上限（≥1）
     */
    public static Lineage walk(SessionStateStore store, String sessionId, int maxDepth) {
        if (store == null || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("store/sessionId 非空");
        }
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth 必须 ≥1（当前 " + maxDepth + "）");
        }
        List<String> ancestors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(sessionId);
        String current = sessionId;
        boolean loopDetected = false;
        boolean depthCapped = false;
        for (int depth = 0; depth < maxDepth; depth++) {
            String source = store.get(current, SessionForkKeys.SOURCE)
                    .map(e -> e.value()).orElse(null);
            if (source == null || source.isBlank()) {
                break; // 根——正常树终止
            }
            if (!visited.add(source)) {
                loopDetected = true; // 重复访问——环
                break;
            }
            ancestors.add(source);
            current = source;
        }
        if (ancestors.size() >= maxDepth) {
            depthCapped = true;
        }
        return new Lineage(List.copyOf(ancestors), loopDetected, depthCapped);
    }

    /** 默认深度便捷重载。 */
    public static Lineage walk(SessionStateStore store, String sessionId) {
        return walk(store, sessionId, DEFAULT_MAX_DEPTH);
    }
}
