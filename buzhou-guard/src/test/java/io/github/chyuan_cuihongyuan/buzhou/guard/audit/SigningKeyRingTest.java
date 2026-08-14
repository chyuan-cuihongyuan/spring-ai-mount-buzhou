package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-39 / spec 13 §T64 密钥版本化轮换：rotate 原子切换、旧钥只验不签、
 * minVerifyVersion 拒老签名、轮换后旧记录仍可验 / 新记录用新钥。
 */
class SigningKeyRingTest {

    private static SigningKeyProvider.VersionedSigningKey versioned(int version, KeyPair keyPair) {
        return new SigningKeyProvider.VersionedSigningKey(version,
                keyPair.getPrivate(), keyPair.getPublic());
    }

    @Test
    void emptyRingDegradesToHashChainMode() {
        SigningKeyRing ring = new SigningKeyRing();
        assertThat(ring.hasSigningKey()).isFalse();
        assertThat(ring.activeVersion()).isZero();
        assertThat(ring.activePrivateKey()).isNull();
    }

    @Test
    void highestVersionSignsAndOlderKeysStayVerifiable() {
        KeyPair v1 = AuditChain.generateKeyPair();
        KeyPair v2 = AuditChain.generateKeyPair();
        SigningKeyRing ring = new SigningKeyRing(0, List.of(versioned(1, v1), versioned(2, v2)));
        assertThat(ring.activeVersion()).isEqualTo(2);
        // 双版本公钥皆可验
        assertThat(ring.verifyKey(1)).isEqualTo(v1.getPublic());
        assertThat(ring.verifyKey(2)).isEqualTo(v2.getPublic());
        // 旧版本私钥不进内存（只验不签的物理保证经轮换路径断言，见下）
        assertThat(ring.registeredVersions()).containsExactly(1, 2);
    }

    @Test
    void rotationSwitchesSigningKeyAtomicallyAndOldKeyStopsSigning() {
        KeyPair first = AuditChain.generateKeyPair();
        SigningKeyRing ring = new SigningKeyRing(0, List.of(versioned(1, first)));
        AuditChain before = new AuditChain("a", "1", ring);
        before.append("s1", "guard.tool.blocked", "{}", "BLOCKED");

        ring.rotate(2, AuditChain.generateKeyPair());
        AuditChain after = new AuditChain("a", "1", ring);
        after.append("s1", "guard.tool.blocked", "{}", "BLOCKED");

        // 轮换前记录：版本 1，用旧公钥可验；新记录：版本 2，旧公钥验不过
        assertThat(before.records().getFirst().keyVersion()).isEqualTo(1);
        assertThat(after.records().getFirst().keyVersion()).isEqualTo(2);
        assertThat(AuditChainVerifier.SignatureOps.verify(before.records().getFirst(),
                first.getPublic())).isTrue();
        assertThat(AuditChainVerifier.SignatureOps.verify(after.records().getFirst(),
                first.getPublic())).isFalse();

        // 旧钥只验不签：verifyKey(1) 仍可用
        assertThat(ring.verifyKey(1)).isEqualTo(first.getPublic());
        // 轮换版本必须递增
        assertThatThrownBy(() -> ring.rotate(2, AuditChain.generateKeyPair()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void minVerifyVersionRejectsTooOldSignatures() {
        KeyPair v1 = AuditChain.generateKeyPair();
        KeyPair v2 = AuditChain.generateKeyPair();
        SigningKeyRing ring = new SigningKeyRing(2, List.of(versioned(1, v1), versioned(2, v2)));
        // v1 低于 minVerifyVersion=2：不可验（即便公钥在环上）
        assertThat(ring.verifyKey(1)).isNull();
        assertThat(ring.verifyKey(2)).isEqualTo(v2.getPublic());
        // minVerifyVersion 把 init 期低于窗口的公钥也清掉
        assertThat(ring.registeredVersions()).containsExactly(2);
    }

    @Test
    void rejectNonPositiveOrDuplicateVersions() {
        KeyPair keyPair = AuditChain.generateKeyPair();
        assertThatThrownBy(() -> new SigningKeyRing(0,
                List.of(new SigningKeyProvider.VersionedSigningKey(0,
                        keyPair.getPrivate(), keyPair.getPublic()))))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> new SigningKeyRing(0, List.of(
                versioned(1, keyPair), versioned(1, AuditChain.generateKeyPair()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复");
        SigningKeyRing ring = new SigningKeyRing(0, List.of(versioned(1, keyPair)));
        assertThatThrownBy(() -> ring.rotate(0, AuditChain.generateKeyPair()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
