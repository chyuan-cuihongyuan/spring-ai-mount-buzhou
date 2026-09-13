package io.github.chyuan_cuihongyuan.buzhou.core.hook;

/**
 * impl-767 / spec 1014：Spotlighting 应用与损坏只读快照（防御覆盖率 +
 * 包裹篡改/截断探测——静默行为显形谱系）。
 *
 * <p>守恒不变量：{@code wrapped == unwrapped + malformed}。
 *
 * @param wrapped   unwrap 入口含标记头的尝试累计
 * @param unwrapped 成功还原累计
 * @param malformed 含标记头但结构不完整被原样放行累计（篡改/截断信号）
 */
public record SpotlightingStats(long wrapped, long unwrapped, long malformed) {
}
