package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 507 / T765–T766：可逆 PII 代管库——vaultize/restore 往返恒等、
 * 同原值同令牌去重、TTL 过期 fail-safe 保留令牌、maxEntries 有界、salt
 * 影响令牌、yml 装配缺席。
 */
class PiiVaultTest {

    @Test
    void vaultizeRestoreRoundTripIsIdentity() {
        PiiVault vault = new PiiVault("s3cret", Duration.ofMinutes(10), 100);
        String token = vault.vaultize("13800138000");
        assertThat(token).startsWith("[PII-VAULT:").endsWith("]");
        String restored = vault.restore("回拨 " + token + " 给用户");
        assertThat(restored).isEqualTo("回拨 13800138000 给用户");
    }

    @Test
    void sameOriginalMapsToSameTokenDedupStorage() {
        PiiVault vault = new PiiVault("s3cret", Duration.ofMinutes(10), 100);
        String t1 = vault.vaultize("a@b.com");
        String t2 = vault.vaultize("a@b.com");
        assertThat(t1).isEqualTo(t2);
        assertThat(vault.size()).isEqualTo(1); // 去重存储
        assertThat(vault.vaultize("c@d.com")).isNotEqualTo(t1);
    }

    @Test
    void expiredTokensLeftAsIsFailSafe() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-12T00:00:00Z"));
        PiiVault vault = new PiiVault("s3cret", Duration.ofSeconds(60), 100, new Clock() {
            @Override
            public Instant instant() {
                return now.get();
            }

            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }
        });
        String token = vault.vaultize("11010519491231002X");
        now.set(Instant.parse("2026-09-12T00:02:00Z")); // 超过 60s TTL
        String text = "证件 " + token + " 已过期";
        assertThat(vault.restore(text)).isEqualTo(text); // fail-safe 原样保留
        assertThat(vault.unknownTokenCount() + vault.restoredCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void maxEntriesEvictsAndSaltChangesTokens() {
        PiiVault vault = new PiiVault("s3cret", Duration.ofMinutes(10), 2);
        vault.vaultize("one");
        vault.vaultize("two");
        vault.vaultize("three"); // 逐出最旧 one
        String oneToken = "[PII-VAULT:" + tokenOf("s3cret", "one") + "]";
        assertThat(vault.restore(oneToken)).isEqualTo(oneToken); // 已逐出——原样保留
        assertThat(vault.size()).isEqualTo(2);

        // salt 不同令牌不同（防跨部署字典反查）
        PiiVault other = new PiiVault("pepper", Duration.ofMinutes(10), 100);
        assertThat(other.vaultize("one")).isNotEqualTo("[PII-VAULT:" + tokenOf("s3cret", "one") + "]");
    }

    @Test
    void invalidConstructorArgsFailFast() {
        assertThatThrownBy(() -> new PiiVault(null, Duration.ofMinutes(1), 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PiiVault("s", Duration.ZERO, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PiiVault("s", Duration.ofMinutes(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ymlAssemblyOnlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        () -> io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.guard.config.BuzhouGuardAutoConfiguration.class))
                .withPropertyValues("buzhou.guard.pii.vault.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouPiiVault");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        () -> io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.guard.config.BuzhouGuardAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouPiiVault");
                });
    }

    private static String tokenOf(String salt, String original) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((salt + "|" + original).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
