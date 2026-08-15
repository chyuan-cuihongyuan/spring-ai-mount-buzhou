package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Map;

/**
 * 会话索引行（spec 30 / T109 / impl-84）：运维枚举/过滤会话的元数据视图。
 *
 * <p><b>最终一致</b>：索引由 {@code SessionIndexObserver} 在会话生命周期点异步维护
 * （onOpen/onTurnEnd/onClose），更新失败只 WARN 不阻断会话——索引是查询优化面，
 * 不是权威数据（权威 = 五 store）。
 *
 * @param sessionId          会话 id
 * @param appId              应用 id
 * @param agentName          agent 名
 * @param status             {@link #STATUS_ACTIVE} / {@link #STATUS_CLOSED} / {@link #STATUS_DELETED}
 * @param createdAtEpochMs   首次入库时刻
 * @param lastActiveAtEpochMs 最近活动时刻（onTurnEnd 刷新）
 * @param turnCount          已完成轮数（观察者进程内计数，重启后从 0 续——近似值）
 * @param tags               业务自定义检索键（可空）
 *
 * @since 1.0.0
 */
public record SessionInfo(
        String sessionId,
        String appId,
        String agentName,
        String status,
        long createdAtEpochMs,
        long lastActiveAtEpochMs,
        int turnCount,
        Map<String, String> tags) {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_DELETED = "DELETED";

    public SessionInfo {
        tags = tags == null ? Map.of() : Map.copyOf(tags);
    }
}
