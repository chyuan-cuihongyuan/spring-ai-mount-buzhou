package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1893 / T2988：幂等键判定——三态、TTL 边界、畸形。 */
class IdempotencyKeyGuardTest {

    /** 三态：首见放行、同参重放、异参冲突。 */
    @Test
    void threeWayDecision() {
        assertThat(IdempotencyKeyGuard.decide(null, "h1"))
                .isEqualTo(IdempotencyKeyGuard.Decision.FIRST);
        assertThat(IdempotencyKeyGuard.decide("h1", "h1"))
                .isEqualTo(IdempotencyKeyGuard.Decision.REPLAY);
        assertThat(IdempotencyKeyGuard.decide("h1", "h2"))
                .isEqualTo(IdempotencyKeyGuard.Decision.CONFLICT);
    }

    /** 异参复用连指纹都缺失——不可信请求按 CONFLICT 处理。 */
    @Test
    void missingFingerprintOnReuseConflicts() {
        assertThat(IdempotencyKeyGuard.decide("h1", null))
                .isEqualTo(IdempotencyKeyGuard.Decision.CONFLICT);
    }

    /** TTL 边界：恰到期即失效；差一毫秒存活。 */
    @Test
    void ttlExpiryBoundary() {
        assertThat(IdempotencyKeyGuard.isExpired(1000, 1000 + 24 * 3600_000L,
                24 * 3600_000L)).isTrue();
        assertThat(IdempotencyKeyGuard.isExpired(1000, 1000 + 24 * 3600_000L - 1,
                24 * 3600_000L)).isFalse();
    }

    /** 畸形入参 fail-fast：负 TTL、负时点。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> IdempotencyKeyGuard.isExpired(0, 100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttl 不能为负");
        assertThatThrownBy(() -> IdempotencyKeyGuard.isExpired(-1, 100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时点不能为负");
    }
}
