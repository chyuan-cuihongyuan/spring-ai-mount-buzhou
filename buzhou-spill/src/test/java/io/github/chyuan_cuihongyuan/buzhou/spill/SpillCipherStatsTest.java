package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1079 / impl 831：Spill 加解密读面——加密/解密调用与失败双组计数、
 * resetForTest 归零。密钥构造同既有 SpillCipher 测试（32 字节 base64）。
 */
class SpillCipherStatsTest {

    private static String randomKey() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @BeforeEach
    void reset() {
        SpillCipher.resetForTest();
    }

    @Test
    void roundTripCountsBothSides() {
        SpillCipher cipher = SpillCipher.fromBase64Key(randomKey());
        String secret = "机密 spill 内容";
        String wire = cipher.encrypt(secret);
        assertThat(SpillCipher.isEncrypted(wire)).isTrue();
        assertThat(cipher.decryptIfEncrypted(wire)).isEqualTo(secret);

        SpillCipher.SpillCipherStats stats = SpillCipher.stats();
        assertThat(stats.encryptCalls()).isEqualTo(1);
        assertThat(stats.encryptFailures()).isZero();
        assertThat(stats.decryptCalls()).isEqualTo(1);
        assertThat(stats.decryptFailures()).isZero();
    }

    @Test
    void wrongKeyDecryptCountsFailure() {
        SpillCipher writer = SpillCipher.fromBase64Key(randomKey());
        SpillCipher reader = SpillCipher.fromBase64Key(randomKey());
        String wire = writer.encrypt("机密");

        assertThatThrownBy(() -> reader.decryptIfEncrypted(wire))
                .isInstanceOf(IllegalStateException.class);

        SpillCipher.SpillCipherStats stats = SpillCipher.stats();
        assertThat(stats.decryptCalls()).isEqualTo(1);
        assertThat(stats.decryptFailures()).isEqualTo(1);
    }

    @Test
    void plainPassthroughStillCountsDecryptCall() {
        SpillCipher cipher = SpillCipher.fromBase64Key(randomKey());
        String plain = "旧明文内容（无魔法前缀）";
        assertThat(cipher.decryptIfEncrypted(plain)).isEqualTo(plain);

        assertThat(SpillCipher.stats().decryptCalls()).isEqualTo(1);
        assertThat(SpillCipher.stats().decryptFailures()).isZero();
    }

    @Test
    void resetForTestZeroesCounters() {
        SpillCipher cipher = SpillCipher.fromBase64Key(randomKey());
        cipher.encrypt("x");
        assertThat(SpillCipher.stats().encryptCalls()).isEqualTo(1);

        SpillCipher.resetForTest();

        SpillCipher.SpillCipherStats stats = SpillCipher.stats();
        assertThat(stats.encryptCalls()).isZero();
        assertThat(stats.decryptCalls()).isZero();
        assertThat(stats.encryptFailures()).isZero();
        assertThat(stats.decryptFailures()).isZero();
    }
}
