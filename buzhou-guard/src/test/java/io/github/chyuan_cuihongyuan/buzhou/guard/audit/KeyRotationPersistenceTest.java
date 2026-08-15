package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 41 §A / T153 / impl-124：审计密钥轮换持久化（写而后切）与链外锚定。
 */
class KeyRotationPersistenceTest {

    @TempDir
    Path keyDir;

    @Test
    void rotatedKeyPersistedAndRediscoveredOnRestart() {
        // v1 以约定命名放入 key-dir（模拟初始部署）
        KeyPair v1 = AuditChain.generateKeyPair();
        writePems(1, v1);

        // 启动：目录扫描入环（active=v1）+ 挂轮换持久化器
        SigningKeyRing ring = new SigningKeyRing(0,
                PemFileKeyProvider.scanDirectory(keyDir).load(), new PemFileKeyPersister(keyDir));
        assertThat(ring.hasSigningKey()).isTrue();
        assertThat(ring.activeVersion()).isEqualTo(1);

        // 运行期轮换到 v2：写而后切——磁盘出现 v2 PEM
        KeyPair v2 = AuditChain.generateKeyPair();
        ring.rotate(2, v2);
        assertThat(Files.exists(PemFileKeyPersister.privateKeyFile(keyDir, 2))).isTrue();
        assertThat(Files.exists(PemFileKeyPersister.publicKeyFile(keyDir, 2))).isTrue();
        assertThat(ring.activeVersion()).isEqualTo(2);

        // 「重启」：同一目录重新扫描——v2 自动入环且为 active（既往 bug：轮换丢失→断链）
        SigningKeyRing restarted = new SigningKeyRing(0,
                PemFileKeyProvider.scanDirectory(keyDir).load(), new PemFileKeyPersister(keyDir));
        assertThat(restarted.activeVersion()).isEqualTo(2);
        assertThat(restarted.verifyKey(1)).isNotNull();
        assertThat(restarted.verifyKey(2)).isNotNull();

        // 轮换期签的记录在重启后的环下仍可验（v2 公钥在环）
        AuditChain chain = new AuditChain("a", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED"); // v2 签名
        assertThat(AuditChainVerifier.verify(chain.records(), restarted).intact()).isTrue();
        assertThat(chain.records().getFirst().keyVersion()).isEqualTo(2);
    }

    @Test
    void persistFailureAbortsRotationAndKeepsActive() {
        KeyPair v1 = AuditChain.generateKeyPair();
        writePems(1, v1);
        SigningKeyRing ring = new SigningKeyRing(0,
                PemFileKeyProvider.scanDirectory(keyDir).load(),
                (version, keyPair) -> {
                    throw new IllegalStateException("保险库不可达");
                });

        // 写而后切：持久化失败 → 轮换整体失败、active 不变
        assertThatThrownBy(() -> ring.rotate(2, AuditChain.generateKeyPair()))
                .hasMessageContaining("保险库不可达");
        assertThat(ring.activeVersion()).isEqualTo(1);
        assertThat(ring.verifyKey(2)).isNull();
    }

    @Test
    void externalAnchorDetectsTailTruncationAndRewrite() {
        KeyPair v1 = AuditChain.generateKeyPair();
        SigningKeyRing ring = new SigningKeyRing(0, List.of(
                new SigningKeyProvider.VersionedSigningKey(1, v1.getPrivate(), v1.getPublic())));
        AuditChain chain = new AuditChain("a", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");
        chain.append("s1", "session.cancelled", "{}", "RECORDED");

        // 运维链外保存的锚点 = 完整链校验报告给出的链头哈希
        String anchor = AuditChainVerifier.verify(chain.records(), ring).headHash();
        assertThat(anchor).isNotBlank();

        // 锚点一致：anchored 通过
        VerificationReport full = AuditChainVerifier.verify(chain.records(), ring, anchor);
        assertThat(full.intact()).isTrue();
        assertThat(full.anchored()).isTrue();
        assertThat(full.anchorMatched()).isTrue();

        // 删尾：链内部仍自洽（intact），但链头与外锚不符 → anchorMatched=false（既往盲区）
        List<AgentAuditRecord> truncated = chain.records().subList(0, 2);
        VerificationReport tailCut = AuditChainVerifier.verify(truncated, ring, anchor);
        assertThat(tailCut.intact()).isTrue();
        assertThat(tailCut.anchored()).isFalse();
        assertThat(tailCut.anchorMatched()).isFalse();

        // 未提供锚点：anchorMatched=null（跳过外锚，行为与既往一致）
        assertThat(AuditChainVerifier.verify(truncated, ring).anchorMatched()).isNull();
    }

    private void writePems(int version, KeyPair pair) {
        new PemFileKeyPersister(keyDir).persist(version, pair);
    }
}
