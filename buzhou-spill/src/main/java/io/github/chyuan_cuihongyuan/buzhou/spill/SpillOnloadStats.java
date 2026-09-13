package io.github.chyuan_cuihongyuan.buzhou.spill;

/**
 * impl-761 / spec 1008：spill 回读命中率只读快照（PostgreSQL buffer hit-ratio
 * 借鉴——命中率恶化 = spill 侵蚀/容量不足的第一信号）。
 *
 * <p>守恒不变量：{@code attempts == loaded + failed}（空 path 参数不计尝试）。
 *
 * @param attempts 回读尝试累计（每个非空 path 参数一次）
 * @param loaded   回读成功累计（内容完好在盘并回灌参数）
 * @param failed   回读失败累计（文件缺失/侵蚀/为空——spill 侵蚀信号）
 */
public record SpillOnloadStats(long attempts, long loaded, long failed) {
}
