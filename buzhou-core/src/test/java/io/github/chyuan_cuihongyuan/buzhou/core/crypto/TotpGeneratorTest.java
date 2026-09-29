package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpGeneratorTest {

    /** RFC 4226 官方向量：secret "12345678901234567890"，6 位。 */
    private static final int[] RFC4226_VECTORS = {
            755224, 287082, 359152, 969429, 338314,
            254676, 287922, 162583, 399871, 520489};

    @Test
    void shouldMatchRfc4226HotpVectors() {
        byte[] secret = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        for (int counter = 0; counter < RFC4226_VECTORS.length; counter++) {
            assertThat(TotpGenerator.hotp(secret, counter, 6))
                    .as("RFC4226 计数 %d", counter)
                    .isEqualTo(RFC4226_VECTORS[counter]);
        }
    }

    @Test
    void shouldMatchRfc6238VectorAtT59() {
        byte[] secret = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        assertThat(TotpGenerator.totp(secret, 59, 8)).isEqualTo(94287082);
        assertThat(TotpGenerator.totp6(secret, 59)).isEqualTo(287082);
    }

    @Test
    void shouldVerifyWithWindow() {
        byte[] secret = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        int code = TotpGenerator.totp6(secret, 100 * 30);
        assertThat(TotpGenerator.verify(secret, 100 * 30, code, 6, 1)).isTrue();
        assertThat(TotpGenerator.verify(secret, 101 * 30, code, 6, 1)).isTrue();
        assertThat(TotpGenerator.verify(secret, 105 * 30, code, 6, 1)).isFalse();
        int otherCode = code == 999999 ? 999998 : 999999;
        assertThat(TotpGenerator.verify(secret, 100 * 30, otherCode, 6, 1)).isFalse();
    }

    @Test
    void shouldBeDeterministic() {
        byte[] secret = "demo-secret".getBytes(StandardCharsets.US_ASCII);
        assertThat(TotpGenerator.hotp(secret, 77, 6)).isEqualTo(TotpGenerator.hotp(secret, 77, 6));
        assertThatThrownBy(() -> TotpGenerator.hotp(null, 1, 6))
                .isInstanceOf(RuntimeException.class);
    }
}
