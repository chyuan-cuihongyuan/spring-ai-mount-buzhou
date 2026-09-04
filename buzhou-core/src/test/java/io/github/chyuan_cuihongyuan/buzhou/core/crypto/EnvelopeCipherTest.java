package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 333 / impl-356：信封密码回归——往返 / AAD 绑定防剪贴 / 篡改抛 /
 * 双钥轮换（旧信封可解、新写新 keyId）/ 幽灵 keyId 拒 / 非法钥红。
 */
class EnvelopeCipherTest {

    private static String key(int seedByte) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) seedByte);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void roundtripPreservesPlaintext() {
        EnvelopeCipher cipher = new EnvelopeCipher(key(1), null);
        String envelope = cipher.encrypt("会话内容 top-secret", "msg-1\nsess-1");
        assertThat(envelope).startsWith("buzhou:v1:").doesNotContain("top-secret");
        assertThat(cipher.decrypt(envelope, "msg-1\nsess-1")).isEqualTo("会话内容 top-secret");
    }

    @Test
    void aadMismatchRejected_cutAndPasteFails() {
        EnvelopeCipher cipher = new EnvelopeCipher(key(1), null);
        String envelope = cipher.encrypt("secret", "msg-1\nsess-1");
        assertThatThrownBy(() -> cipher.decrypt(envelope, "msg-2\nsess-1")) // 换消息
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt(envelope, "msg-1\nsess-2")) // 换会话
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tamperedCiphertextRejected() {
        EnvelopeCipher cipher = new EnvelopeCipher(key(1), null);
        String envelope = cipher.encrypt("secret", "aad");
        String b64 = envelope.substring(envelope.lastIndexOf(':') + 1);
        byte[] raw = Base64.getDecoder().decode(b64);
        raw[raw.length - 1] ^= 0x01; // 翻一位密文/标签
        String tampered = envelope.substring(0, envelope.lastIndexOf(':') + 1)
                + Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> cipher.decrypt(tampered, "aad"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("完整性优先");
    }

    @Test
    void rotation_readsOldWritesNew() {
        EnvelopeCipher first = new EnvelopeCipher(key(1), null);
        String oldEnvelope = first.encrypt("legacy", "aad");
        EnvelopeCipher rotated = new EnvelopeCipher(key(2), key(1));
        assertThat(rotated.decrypt(oldEnvelope, "aad")).isEqualTo("legacy"); // 旧钥数据可读
        String newEnvelope = rotated.encrypt("fresh", "aad");
        assertThat(newEnvelope).startsWith("buzhou:v1:" + rotated.currentKeyId());
        assertThat(first.currentKeyId()).isNotEqualTo(rotated.currentKeyId());
        assertThatThrownBy(() -> new EnvelopeCipher(key(2), key(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("相同"); // 双钥相同无意义
    }

    @Test
    void ghostKeyIdRejected() {
        EnvelopeCipher cipher = new EnvelopeCipher(key(1), key(2));
        String forged = "buzhou:v1:deadbeef:" + Base64.getEncoder()
                .encodeToString(new byte[32]);
        assertThatThrownBy(() -> cipher.decrypt(forged, "aad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在当前/前代钥环");
    }

    @Test
    void invalidKeysFailFast() {
        assertThatThrownBy(() -> new EnvelopeCipher("not-base64!!!", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnvelopeCipher(
                Base64.getEncoder().encodeToString(new byte[8]), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16/24/32");
        assertThatThrownBy(() -> new EnvelopeCipher(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void envelopeDetectionAndKeyRingView() {
        EnvelopeCipher cipher = new EnvelopeCipher(key(1), key(2));
        assertThat(EnvelopeCipher.isEnvelope(cipher.encrypt("x", "a"))).isTrue();
        assertThat(EnvelopeCipher.isEnvelope("普通明文")).isFalse();
        assertThat(EnvelopeCipher.isEnvelope(null)).isFalse();
        assertThat(cipher.keyRingView()).hasSize(2); // 当前+前代
        assertThat(cipher.keyRingView()).containsKey(cipher.currentKeyId());
    }
}
