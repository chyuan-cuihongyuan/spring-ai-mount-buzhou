package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 句柄生命周期注册表（wayfinder2 impl-16 / T44 / docs/spec/12 §spill-16）：
 * context-clearing 的<b>显式逐出双路径</b>之状态层——
 *
 * <ul>
 *   <li><b>模型主动</b>：{@code EvictHandleTool} 逐出已消费句柄（显式标记）；</li>
 *   <li><b>框架自动</b>：引用计数 TTL——{@code ReadRangeTool} 成功回读即刷新引用轮次，
 *       连续 N 轮未引用的句柄自动过期（由视图处理器替换为极简墓碑，Anthropic
 *       「清除已消费 tool_result 是最安全最轻的压缩」语义）。</li>
 * </ul>
 *
 * <p>进程内实现（与会话实例同生命周期）；跨实例续接时句柄逐出随视图重建自然重算——
 * 逐出是<b>视图级</b>优化而非数据删除（SpillStore 原文不变、可随时回读）。
 */
public final class HandleLifecycleRegistry {

    /** 句柄引用计数：URI → 最近被回读的轮次。 */
    private final Map<String, Integer> lastReferencedTurn = new ConcurrentHashMap<>();
    /** 显式逐出标记。 */
    private final Set<String> evicted = ConcurrentHashMap.newKeySet();
    /** 回读标记（ReadRangeTool 置位；视图处理器按当前轮吸收为引用）。 */
    private final Set<String> readFlagged = ConcurrentHashMap.newKeySet();

    /** 模型主动逐出（显式路径）。 */
    public void markEvicted(String uri) {
        evicted.add(uri);
    }

    /** 是否已（显式或 TTL）逐出：显式标记，或引用轮次 + TTL ≤ 当前轮。 */
    public boolean isEvicted(String uri, int currentTurn, int ttlTurns) {
        if (evicted.contains(uri)) {
            return true;
        }
        Integer lastRef = lastReferencedTurn.get(uri);
        return lastRef != null && currentTurn - lastRef >= Math.max(1, ttlTurns);
    }

    /** 成功回读置位标记（ReadRangeTool 调用；轮次上下文由视图处理器吸收时补全）。 */
    public void markRead(String uri) {
        readFlagged.add(uri);
        evicted.remove(uri); // 回读即复活（重新有用的句柄不再逐出）
    }

    /**
     * 视图处理器每轮吸收回读标记：被回读的句柄引用刷新到当前轮（TTL 重启）。
     */
    public void absorbReads(int currentTurn) {
        if (readFlagged.isEmpty()) {
            return;
        }
        for (String uri : readFlagged) {
            lastReferencedTurn.merge(uri, currentTurn, Math::max);
        }
        readFlagged.clear();
    }

    /** 首次登记句柄（溢出时调用；未登记的句柄不参与 TTL 判定——只清「冷」不清「新」）。 */
    public void track(String uri, int currentTurn) {
        lastReferencedTurn.putIfAbsent(uri, currentTurn);
    }
}
