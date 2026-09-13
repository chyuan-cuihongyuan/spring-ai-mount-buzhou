package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.Map;

/**
 * impl-768 / spec 1015：超时覆盖命中只读快照（与 R3 幽灵禁用同族——
 * 配置静默失效显形；feature-flag 评估计数思想）。
 *
 * <p>守恒不变量：{@code hits + misses == lookups}。
 *
 * @param lookups       lookup 总次数
 * @param hits          glob 命中次数（返回覆盖值，含 -1「用全局」显式覆盖）
 * @param misses        无任何模式命中的次数（全局值生效）
 * @param hitsByPattern 配置模式 → 命中次数（不可变，含全部配置模式；
 *                      <b>0 = 幽灵覆盖</b>——模式从未命中任何工具名，覆盖静默失效）
 */
public record ToolTimeoutOverrideStats(long lookups, long hits, long misses,
        Map<String, Long> hitsByPattern) {

    public ToolTimeoutOverrideStats {
        hitsByPattern = Map.copyOf(hitsByPattern);
    }
}
