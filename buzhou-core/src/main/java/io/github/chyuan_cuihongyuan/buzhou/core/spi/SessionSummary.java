package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Map;

/**
 * 会话摘要（spec 03 推演 #11，ticket 17）：dashboard 会话列表行。
 *
 * @param sessionId          会话 id
 * @param firstActivityAt    会话内最早 span startedAt
 * @param lastActivityAt     会话内最晚 span 活动时刻（endedAt 兜底 startedAt）
 * @param turnCount          TURN 类 span 数
 * @param spanCount          span 总数
 * @param sessionAttributes  SESSION 类 span 的属性袋（agent.name / app.id 等），无则空表
 */
public record SessionSummary(
        String sessionId,
        Instant firstActivityAt,
        Instant lastActivityAt,
        int turnCount,
        int spanCount,
        Map<String, Object> sessionAttributes) {

    public SessionSummary {
        sessionAttributes = sessionAttributes == null ? Map.of() : Map.copyOf(sessionAttributes);
    }
}
