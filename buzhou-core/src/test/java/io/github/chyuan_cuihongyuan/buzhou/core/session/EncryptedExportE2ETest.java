package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.crypto.EnvelopeCipher;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-672 / spec 919：加密导出×审计×指纹联动——密文进明文审计面 fail-closed、
 * seal→open→审计/严格导入全链咬合、nonce 密文不同但内容指纹稳定、既有零回归。
 */
class EncryptedExportE2ETest {

    private static String key(int seedByte) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) seedByte);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static SessionExport sample() {
        return SessionExport.of("s1", "app", "agent",
                List.of(new BuzhouMessage("m1", "s1", 1, 1, Role.USER, "hi",
                        null, null, null, null, Map.of(), Instant.EPOCH)), null, Map.of());
    }

    @Test
    void sealedPayloadFailsClosedInPlaintextAudit() {
        EncryptedSessionExport crypto =
                new EncryptedSessionExport(new EnvelopeCipher(key(1), null));
        String sealed = crypto.seal(sample());

        // 密文不是明文 JSON——审计面 fail-closed（IllegalArgumentException 同明文口径）
        assertThatThrownBy(() -> SessionExportAudit.audit(sealed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sealOpenAuditStrictChainPasses() {
        EncryptedSessionExport crypto =
                new EncryptedSessionExport(new EnvelopeCipher(key(1), null));
        SessionExport original = sample();
        String sealed = crypto.seal(original);

        SessionExport reopened = crypto.open(sealed);
        SessionExportAudit.AuditReport report = SessionExportAudit.audit(reopened.toJson());
        assertThat(report.strictCompatible()).isTrue();
        // 严格导入产物与原件同 sessionId（内容逐位迁移）
        assertThat(SessionExportAudit.fromJsonStrict(reopened.toJson()).sessionId())
                .isEqualTo("s1");
    }

    @Test
    void nonceCipherDiffersButContentFingerprintStable() {
        EncryptedSessionExport crypto =
                new EncryptedSessionExport(new EnvelopeCipher(key(1), null));
        SessionExport original = sample();
        String sealed1 = crypto.seal(original);
        String sealed2 = crypto.seal(original);

        // nonce：两次封缄密文不同
        assertThat(sealed1).isNotEqualTo(sealed2);
        // 内容指纹：解封后规范化指纹稳定（密文形态变、内容不变）
        String fp1 = SessionExportChecksum.canonicalContentFingerprint(crypto.open(sealed1));
        String fp2 = SessionExportChecksum.canonicalContentFingerprint(crypto.open(sealed2));
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void legacySealOpenSemanticsUnchanged() {
        EncryptedSessionExport crypto =
                new EncryptedSessionExport(new EnvelopeCipher(key(1), null));
        // 非 seal 载荷 open → DATA_CORRUPTION（BuzhouException，既有语义）
        assertThatThrownBy(() -> crypto.open("not-sealed"))
                .isInstanceOf(BuzhouException.class);
        assertThat(EncryptedSessionExport.isSealed(null)).isFalse();
        assertThat(EncryptedSessionExport.isSealed("plain-json")).isFalse();
        assertThat(EncryptedSessionExport.isSealed(crypto.seal(sample()))).isTrue();
    }
}
