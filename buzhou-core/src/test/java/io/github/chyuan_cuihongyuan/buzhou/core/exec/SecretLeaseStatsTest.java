package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretLeaseStatsTest {

    /** 固定步进钟（全确定性——spec 9 时钟注入同风格）。 */
    private static final class MutableClock extends Clock {
        private long millis;

        MutableClock(long startMillis) {
            this.millis = startMillis;
        }

        void advanceMillis(long delta) {
            millis += delta;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    }

    @Test
    void successfulRenewIsCounted() {
        MutableClock clock = new MutableClock(1_000_000L);
        SecretLeases leases = new SecretLeases(clock);

        leases.issue("db-password", "v1", Duration.ofMillis(1_000));
        clock.advanceMillis(500);
        leases.renew("db-password", Duration.ofMillis(1_000));

        SecretLeaseStats stats = leases.stats();
        assertThat(stats.renewed()).isEqualTo(1);
        assertThat(stats.renewRejected()).isZero();
        assertThat(stats.issued()).isEqualTo(1);
    }

    @Test
    void renewAfterExpiryIsCountedAsRejected() {
        MutableClock clock = new MutableClock(1_000_000L);
        SecretLeases leases = new SecretLeases(clock);

        leases.issue("db-password", "v1", Duration.ofMillis(1_000));
        clock.advanceMillis(2_000);
        assertThatThrownBy(() -> leases.renew("db-password", Duration.ofMillis(1_000)))
                .isInstanceOf(IllegalStateException.class);

        SecretLeaseStats stats = leases.stats();
        assertThat(stats.renewRejected()).isEqualTo(1);
        assertThat(stats.renewed()).isZero();
        assertThat(stats.expired()).isEqualTo(1);
    }

    @Test
    void renewMissingLeaseIsCountedAsRejected() {
        SecretLeases leases = new SecretLeases();

        assertThatThrownBy(() -> leases.renew("ghost", Duration.ofMillis(1_000)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(leases.stats().renewRejected()).isEqualTo(1);
    }

    @Test
    void resolveAfterExpiryCountsExpiredLazily() {
        MutableClock clock = new MutableClock(1_000_000L);
        SecretLeases leases = new SecretLeases(clock);

        leases.issue("k", "v", Duration.ofMillis(1_000));
        clock.advanceMillis(2_000);
        assertThat(leases.resolve("k")).isEmpty();

        assertThat(leases.stats().expired()).isEqualTo(1);
    }

    @Test
    void revokeIsCountedOnceDespiteIdempotentCalls() {
        SecretLeases leases = new SecretLeases();

        leases.issue("k", "v", Duration.ofMillis(10_000));
        leases.revoke("k");
        leases.revoke("k");

        SecretLeaseStats stats = leases.stats();
        assertThat(stats.revoked()).isEqualTo(1);
        assertThat(stats.issued()).isEqualTo(1);
    }

    @Test
    void statsAlignsWithLegacyGetters() {
        MutableClock clock = new MutableClock(0L);
        SecretLeases leases = new SecretLeases(clock);

        leases.issue("a", "v", Duration.ofMillis(10_000));
        leases.renew("a", Duration.ofMillis(10_000));
        leases.revoke("b");

        SecretLeaseStats stats = leases.stats();
        assertThat(stats.issued()).isEqualTo(leases.issuedCount());
        assertThat(stats.expired()).isEqualTo(leases.expiredCount());
        assertThat(stats.revoked()).isEqualTo(leases.revokedCount());
    }
}
