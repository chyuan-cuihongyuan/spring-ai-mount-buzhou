package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.crypto.EnvelopeCipher;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 510 / T771–T772：加密会话导出封缄——seal/open 往返恒等、错主钥
 * DATA_CORRUPTION 带修法、非封缄输入 fail-fast、跨域 AAD 剪贴不可解、
 * 篡改失败、isSealed 判定。
 */
class EncryptedSessionExportTest {

    private static final String MASTER_KEY = java.util.Base64.getEncoder()
            .encodeToString(new byte[]{
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31});

    private static SessionExport sample() {
        return SessionExport.of("sess-1", "app", "agent",
                List.of(), null, Map.of());
    }

    @Test
    void sealOpenRoundTripPreservesExport() {
        EncryptedSessionExport sealer = new EncryptedSessionExport(
                new EnvelopeCipher(MASTER_KEY, null));
        SessionExport export = sample();
        String sealed = sealer.seal(export);
        assertThat(sealed).startsWith(EncryptedSessionExport.SEAL_PREFIX);
        assertThat(EncryptedSessionExport.isSealed(sealed)).isTrue();

        SessionExport opened = sealer.open(sealed);
        assertThat(opened.sessionId()).isEqualTo("sess-1");
        assertThat(opened.appId()).isEqualTo("app");
        assertThat(EncryptedSessionExport.isSealed(export.toJson())).isFalse(); // 明文形态
    }

    @Test
    void wrongMasterKeyFailsWithRecoveryHint() {
        String otherKey = java.util.Base64.getEncoder().encodeToString(new byte[]{
                31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16,
                15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0});
        EncryptedSessionExport sealer = new EncryptedSessionExport(
                new EnvelopeCipher(MASTER_KEY, null));
        String sealed = sealer.seal(sample());

        EncryptedSessionExport stranger = new EncryptedSessionExport(
                new EnvelopeCipher(otherKey, null));
        assertThatThrownBy(() -> stranger.open(sealed))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("解密失败")
                .hasMessageContaining("密钥环");
    }

    @Test
    void plainJsonRejectedWithGuidance() {
        EncryptedSessionExport sealer = new EncryptedSessionExport(
                new EnvelopeCipher(MASTER_KEY, null));
        String plain = sample().toJson();
        assertThatThrownBy(() -> sealer.open(plain))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("非加密会话导出封缄")
                .hasMessageContaining("fromJson");
        assertThatThrownBy(() -> sealer.open(null))
                .isInstanceOf(BuzhouException.class);
    }

    @Test
    void crossPurposeEnvelopeRejection() {
        // 同一 cipher：333 消息域信封（AAD=消息标识）不能当导出封缄打开
        EnvelopeCipher cipher = new EnvelopeCipher(MASTER_KEY, null);
        EncryptedSessionExport sealer = new EncryptedSessionExport(cipher);
        String messageEnvelope = cipher.encrypt("会话消息密文", "msg:sess-1:42");
        String forged = EncryptedSessionExport.SEAL_PREFIX + messageEnvelope;
        assertThatThrownBy(() -> sealer.open(forged))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void tamperedCiphertextFails() {
        EncryptedSessionExport sealer = new EncryptedSessionExport(
                new EnvelopeCipher(MASTER_KEY, null));
        String sealed = sealer.seal(sample());
        char last = sealed.charAt(sealed.length() - 1);
        String tampered = sealed.substring(0, sealed.length() - 1)
                + (last == 'A' ? 'B' : 'A');
        assertThatThrownBy(() -> sealer.open(tampered))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void invalidCipherAndExportFailFast() {
        assertThatThrownBy(() -> new EncryptedSessionExport(null))
                .isInstanceOf(IllegalArgumentException.class);
        EncryptedSessionExport sealer = new EncryptedSessionExport(
                new EnvelopeCipher(MASTER_KEY, null));
        assertThatThrownBy(() -> sealer.seal(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
