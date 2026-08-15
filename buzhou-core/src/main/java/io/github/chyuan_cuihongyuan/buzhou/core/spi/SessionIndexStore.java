package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.List;
import java.util.Optional;

/**
 * 会话索引 store（spec 30 / T109 / impl-84）：会话枚举/过滤的元数据索引——
 * 五 store 均为按 sessionId 点查，本索引补「列表/过滤」查询面（ops 排障、
 * 按 app/agent 统计活跃、dashboard 会话列表）。
 *
 * <p><b>降级语义</b>：索引不存在（未装配）= 无枚举能力，会话功能零影响——索引是
 * 查询优化面而非权威数据（最终一致：更新失败只 WARN）。
 *
 * <p>实现：内存（进程内，重启重建）/ JDBC（表 buzhou_session_index，V3 迁移）/
 * Redis（ZSET lastActive + HASH 元数据）。测试契约口径与五 store 同源（真实现、无 mock）。
 */
public interface SessionIndexStore {

    /** 插入或更新一行（onOpen/onTurnEnd/onClose 生命周期点）。 */
    void upsert(SessionInfo info);

    Optional<SessionInfo> get(String sessionId);

    /** 按 {@link SessionIndexQuery} 查询（lastActiveAt 倒序；过滤 null = 不过滤）。 */
    List<SessionInfo> list(SessionIndexQuery query);

    /** 会话删除时摘除索引行（幂等）。 */
    void delete(String sessionId);

    /**
     * 保留策略清扫（spec 37 §C / T134 / impl-107）：删除 lastActiveAt < cutoff 且
     * status != ACTIVE 的行（CLOSED/DELETED 淘汰；ACTIVE 永不扫）。默认 no-op 返回 0
     * （实现覆写）。返回实际删除数。
     */
    default int purgeOlderThan(java.time.Instant cutoff, int limit) {
        return 0;
    }
}
