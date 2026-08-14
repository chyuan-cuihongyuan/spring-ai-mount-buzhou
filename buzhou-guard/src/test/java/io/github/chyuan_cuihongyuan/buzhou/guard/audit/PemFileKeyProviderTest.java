package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-39 / spec 13 §T64 PEM 文件密钥加载：PKCS#8 私钥加载、公钥从私钥材料内嵌点恢复、
 * 显式公钥文件、坏文件明示失败。
 */
class PemFileKeyProviderTest {

    @TempDir
    Path tempDir;

    private Path writePem(String banner, byte[] der) throws Exception {
        StringBuilder pem = new StringBuilder("-----BEGIN ").append(banner).append("-----\n");
        String base64 = Base64.getEncoder().encodeToString(der);
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        pem.append("-----END ").append(banner).append("-----\n");
        Path file = tempDir.resolve(banner.toLowerCase().replace(' ', '-') + "-"
                + System.nanoTime() + ".pem");
        Files.writeString(file, pem.toString(), StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void loadsPkcs8PrivateKeyAndRecoversEmbeddedPublicKey() throws Exception {
        KeyPair pair = AuditChain.generateKeyPair();
        Path privatePem = writePem("PRIVATE KEY", pair.getPrivate().getEncoded());

        List<SigningKeyProvider.VersionedSigningKey> keys =
                new PemFileKeyProvider(List.of(new PemFileKeyProvider.Entry(1, privatePem)))
                        .load();

        assertThat(keys).hasSize(1);
        assertThat(keys.getFirst().version()).isEqualTo(1);
        // 公钥从 PKCS#8 内嵌公钥点恢复（不依赖外部公钥文件）
        assertThat(keys.getFirst().publicKey()).isEqualTo(pair.getPublic());

        SigningKeyRing ring = new SigningKeyRing(0, keys);
        AuditChain chain = new AuditChain("a", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        assertThat(chain.records().getFirst().keyVersion()).isEqualTo(1);
        assertThat(chain.verify(ring)).isTrue();
    }

    @Test
    void explicitPublicKeyFilePreferredWhenConfigured() throws Exception {
        KeyPair pair = AuditChain.generateKeyPair();
        Path privatePem = writePem("PRIVATE KEY", pair.getPrivate().getEncoded());
        Path publicPem = writePem("PUBLIC KEY", pair.getPublic().getEncoded());

        List<SigningKeyProvider.VersionedSigningKey> keys = new PemFileKeyProvider(List.of(
                new PemFileKeyProvider.Entry(1, privatePem, publicPem))).load();
        assertThat(keys.getFirst().publicKey()).isEqualTo(pair.getPublic());
    }

    @Test
    void unreadableFileFailsWithExplicitMessage() {
        PemFileKeyProvider provider = new PemFileKeyProvider(List.of(
                new PemFileKeyProvider.Entry(1, tempDir.resolve("missing.pem"))));
        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不可读");
    }

    @Test
    void nonPkcs8MaterialFailsWithGuidance() throws Exception {
        Path garbage = tempDir.resolve("garbage.pem");
        Files.writeString(garbage, "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n");
        PemFileKeyProvider provider = new PemFileKeyProvider(List.of(
                new PemFileKeyProvider.Entry(1, garbage)));
        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PKCS#8");
    }
}
