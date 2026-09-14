package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;

/**
 * 租约信息——持有人 / 护栏令牌 / 获取与到期时刻（选主与租约巡检读面）。
 */
public record LeaseInfo(String ownerId, long fencingToken, Instant acquiredAt, Instant expiresAt) {
}
