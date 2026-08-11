package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 持久化强度分档（spec「崩溃中轮次恢复」/ CONTEXT「持久化强度分档」）。
 *
 * <p>会话状态落盘强度的按部署取舍，直接对标 LangGraph durability 三档。
 * 档位是<b>存储实现侧的写缓冲策略</b>：编排方（记忆写路径）不按档位分支，
 * 由 {@code DurabilityTieredStores} 装饰器在存储写路径上表达，不新增 SPI。
 *
 * <ul>
 *   <li>{@link #SYNC} —— {@code append}/{@code put} 写直达底层（同步落盘后返回）：相邻步骤间崩溃至多丢在途那一步。</li>
 *   <li>{@link #ASYNC} —— 默认档：内存 / JDBC / Redis 后端的写本身即「shortly after 持久」的语义边界，
 *       本实现下与 SYNC 同行为（写直达），吞吐优先 + 最终持久。</li>
 *   <li>{@link #EXIT} —— {@code append}/{@code put} 仅入缓冲，会话关闭时 flush：最高吞吐，崩溃丢整轮，由恢复语义兜底。</li>
 * </ul>
 */
public enum DurabilityTier {
    SYNC,
    ASYNC,
    EXIT
}
