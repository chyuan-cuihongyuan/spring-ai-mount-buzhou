package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 153 / T512：凭证租约回归——TTL 内可用 / 过期失效 / 续租 / 过期续租拒 /
 * 吊销 / 重签覆盖 / 隔离与观测。
 */
class SecretLeasesTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.now();

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void resolvesWithinTtlAndExpiresLazily() {
        MutableClock clock = new MutableClock();
        SecretLeases leases = new SecretLeases(clock);
        leases.issue("db-password", "s3cret", Duration.ofSeconds(10));

        assertThat(leases.resolve("db-password")).contains("s3cret");

        clock.advanceMillis(10_050);
        assertThat(leases.resolve("db-password")).isEmpty();
        assertThat(leases.expiredCount()).isEqualTo(1);
        assertThat(leases.activeLeases()).isEmpty();
    }

    @Test
    void renewExtendsWithinLeaseAndRejectsAfterExpiry() {
        MutableClock clock = new MutableClock();
        SecretLeases leases = new SecretLeases(clock);
        leases.issue("api-key", "k1", Duration.ofSeconds(10));

        clock.advanceMillis(9_000);
        leases.renew("api-key", Duration.ofSeconds(10)); // 续在过期前
        clock.advanceMillis(9_500);
        assertThat(leases.resolve("api-key")).contains("k1"); // 续租生效

        clock.advanceMillis(1_000); // 过期
        assertThatThrownBy(() -> leases.renew("api-key", Duration.ofSeconds(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重新签发");
    }

    @Test
    void revokeKillsImmediatelyAndIdempotent() {
        SecretLeases leases = new SecretLeases();
        leases.issue("token", "t1", Duration.ofMinutes(5));
        assertThat(leases.resolve("token")).contains("t1");

        leases.revoke("token");
        assertThat(leases.resolve("token")).isEmpty();
        leases.revoke("token"); // 幂等
        assertThat(leases.revokedCount()).isEqualTo(1);
    }

    @Test
    void reissueOverwritesAndKillsOldValue() {
        MutableClock clock = new MutableClock();
        SecretLeases leases = new SecretLeases(clock);
        leases.issue("token", "old", Duration.ofSeconds(60));
        leases.issue("token", "new", Duration.ofSeconds(60));

        assertThat(leases.resolve("token")).contains("new"); // 旧值即刻失效
        assertThat(leases.issuedCount()).isEqualTo(2);
    }

    @Test
    void leasesAreIsolatedByName() {
        SecretLeases leases = new SecretLeases();
        leases.issue("a", "va", Duration.ofSeconds(60));
        leases.issue("b", "vb", Duration.ofSeconds(60));
        leases.revoke("a");

        assertThat(leases.resolve("a")).isEmpty();
        assertThat(leases.resolve("b")).contains("vb");
        assertThat(leases.activeLeases()).containsExactly("b");
    }

    @Test
    void issueValidatesArguments() {
        SecretLeases leases = new SecretLeases();
        assertThatThrownBy(() -> leases.issue(" ", "v", Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> leases.issue("k", "v", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renewAbsentLeaseFailsFast() {
        assertThatThrownBy(() -> new SecretLeases().renew("ghost", Duration.ofSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不存在");
    }
}
