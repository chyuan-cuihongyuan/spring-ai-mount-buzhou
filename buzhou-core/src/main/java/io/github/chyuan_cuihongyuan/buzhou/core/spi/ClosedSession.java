package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;

/**
 * impl-37 / spec 13 §stores-6：已封闭会话的枚举结果（保留策略的锚点事实）。
 *
 * @param sessionId 会话
 * @param closedAt  封闭时刻（SESSION span 的 endedAt——封闭才计时，Temporal 语义）
 */
public record ClosedSession(String sessionId, Instant closedAt) {
}
