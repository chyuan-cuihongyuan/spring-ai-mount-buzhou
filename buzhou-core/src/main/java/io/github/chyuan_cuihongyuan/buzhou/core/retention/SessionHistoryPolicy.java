package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import java.time.Duration;
import java.time.Instant;

/**
 * impl-37 / spec 13 §stores-6：会话保留策略（Temporal 22.3K★ 语义——<b>封闭才计时</b>）。
 *
 * <ul>
 *   <li><b>锚点 = closedAt</b>（SESSION span 的 endedAt）：只有已封闭的会话才进入保留计时，
 *       活动会话永不被保留策略清理；</li>
 *   <li><b>默认保留 PT72H</b>：closedAt + retention 之后可被级联清理；</li>
 *   <li><b>改短不追溯</b>：{@code notBefore} 之前封闭的会话不受本策略约束——改短保留期时
 *       把 {@code notBefore} 设为改短时刻，即实现「旧封闭会话按旧窗口、不追溯」
 *       （Temporal 的期限在关闭时即定、不可中途改已确定期限）。默认 {@link Instant#EPOCH}
 *       = 追溯（首次部署即清理存量过期会话）。</li>
 * </ul>
 *
 * @param retention 保留期；null/非正 → 默认 PT72H
 * @param notBefore 只清理此时刻之后封闭的会话；null → EPOCH（追溯）
 */
public record SessionHistoryPolicy(Duration retention, Instant notBefore) {

    /** 默认保留期（Temporal workflow 默认 retention 72h 对齐）。 */
    public static final Duration DEFAULT_RETENTION = Duration.ofHours(72);

    public SessionHistoryPolicy {
        retention = retention == null || retention.isZero() || retention.isNegative()
                ? DEFAULT_RETENTION : retention;
        notBefore = notBefore == null ? Instant.EPOCH : notBefore;
    }

    public static SessionHistoryPolicy defaults() {
        return new SessionHistoryPolicy(null, null);
    }

    /** closedAt 是否已到期（可被清理）：锚点起算超保留期，且封闭时点在 notBefore 之后。 */
    public boolean expired(Instant closedAt, Instant now) {
        return !closedAt.isBefore(notBefore) && closedAt.plus(retention).isBefore(now);
    }
}
