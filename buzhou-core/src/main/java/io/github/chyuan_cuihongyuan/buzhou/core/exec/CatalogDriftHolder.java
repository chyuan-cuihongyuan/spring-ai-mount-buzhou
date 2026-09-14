package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.List;

/**
 * 工具目录漂移看门狗进程级 Holder（spec 1613 / T2377，spec 201 孤类接线）：
 * 会话构造节拍（HarnessAssembler 组装完最终工具目录）拍指纹——进程级基线
 * 跨会话收敛，目录变化即 WARN + {@code buzhou.catalog.drifted} 计数
 * （RetryBudgetHolder 同款 Holder 模式）。
 *
 * <p>首拍只建基线（装配期变化是常态，不发事件）；包装层不改 ToolDefinition，
 * 指纹只捕捉目录语义变化（增删改）。纯旁路读数——不影响会话构造。
 */
public final class CatalogDriftHolder {

    private static volatile CatalogDriftWatcher watcher = newWatcher();

    private CatalogDriftHolder() {
    }

    private static CatalogDriftWatcher newWatcher() {
        return new CatalogDriftWatcher(payload -> {
            System.getLogger(CatalogDriftHolder.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "工具目录漂移（tool.catalog.drifted）：added={0} removed={1} changed={2}",
                    payload.get("added"), payload.get("removed"), payload.get("changed"));
        });
    }

    /** 当前看门狗（测试可替换；null 重置为新实例）。 */
    public static void setWatcher(CatalogDriftWatcher replacement) {
        watcher = replacement == null ? newWatcher() : replacement;
    }

    /** 当前看门狗。 */
    public static CatalogDriftWatcher watcher() {
        return watcher;
    }

    /** 会话构造节拍拍指纹（List&lt;ToolDefinition&gt; 便捷面）。 */
    public static void snapshot(List<org.springframework.ai.tool.definition.ToolDefinition> catalog) {
        watcher().check(catalog);
    }
}
