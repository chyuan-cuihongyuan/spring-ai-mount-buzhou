package io.github.chyuan_cuihongyuan.buzhou.examples.redteam;

import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChain;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChainVerifier;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyProvider;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.VerificationReport;
import io.github.chyuan_cuihongyuan.buzhou.spill.DiskSpillStore;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillCipher;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillEntry;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillQuota;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 静态安全对抗（spec 45 §B / T162 / impl-133——观察档，替身模型域外确定性用例）：
 * 密钥错配/密文篡改不静默产出脏数据；DB 写权限攻击者对审计链的重写/删尾在签名+外锚下可检。
 */
class StaticSecurityAdversarialTest {

    private static String randomKey() {
        byte[] key = new byte[32];
        ThreadLocalRandom.current().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    // ---- 攻击面 1：加密 spill 的密钥与密文攻击 ----

    @Test
    void keySwapFailsLoudInsteadOfReturningGarbage(@TempDir Path rootDir) {
        SpillUri uri = new SpillUri("agent", "adv", "tc-key-swap");
        new DiskSpillStore(rootDir, SpillQuota.unbounded(),
                SpillCipher.fromBase64Key(randomKey()))
                .store(SpillEntry.of(uri, "机密内容"), 2048);

        // 「攻击/误配」：换钥重启后读旧文件——必须快速失败，绝不静默返回脏明文
        DiskSpillStore attackerStore = new DiskSpillStore(rootDir, SpillQuota.unbounded(),
                SpillCipher.fromBase64Key(randomKey()));
        assertThatThrownBy(() -> attackerStore.load(uri))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("encryption-key");
    }

    @Test
    void ciphertextBitFlipDetectedByGcmTag(@TempDir Path rootDir) throws Exception {
        SpillCipher cipher = SpillCipher.fromBase64Key(randomKey());
        DiskSpillStore store = new DiskSpillStore(rootDir, SpillQuota.unbounded(), cipher);
        SpillUri uri = new SpillUri("agent", "adv", "tc-bitflip");
        store.store(SpillEntry.of(uri, "机密内容"), 2048);

        // 磁盘位翻转（篡改密文体）：GCM tag 验失败 → 快速失败（不产出被改明文）
        Path dataFile = rootDir.resolve("agent").resolve("adv").resolve("tc-bitflip.spill");
        String wire = Files.readString(dataFile);
        byte[] flipped = wire.getBytes(java.nio.charset.StandardCharsets.UTF_8).clone();
        flipped[flipped.length - 3] ^= 0x01;
        Files.write(dataFile, flipped);

        assertThatThrownBy(() -> store.load(uri)).isInstanceOf(IllegalStateException.class);
    }

    // ---- 攻击面 2：DB 写权限攻击者 vs 审计链 ----

    @Test
    void recordRewriteBreaksChainAtFirstTamperedRecord() {
        SigningKeyRing ring = ringWithSignedKey();
        AuditChain chain = new AuditChain("adv", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");
        chain.append("s1", "session.cancelled", "{}", "RECORDED");

        // 攻击者改写中间记录内容（哈希链断在首个被改记录）
        var tampered = new java.util.ArrayList<>(chain.records());
        tampered.set(1, new io.github.chyuan_cuihongyuan.buzhou.guard.audit.AgentAuditRecord(
                tampered.get(1).recordId(), tampered.get(1).timestamp(),
                tampered.get(1).agentId(), tampered.get(1).agentVersion(),
                tampered.get(1).sessionId(), tampered.get(1).actionType(),
                "FORGED", tampered.get(1).outcome(), tampered.get(1).trustLevel(),
                tampered.get(1).parentRecordId(), tampered.get(1).prevHash(),
                tampered.get(1).signature(), tampered.get(1).keyVersion()));
        VerificationReport report = AuditChainVerifier.verify(tampered, ring);
        assertThat(report.intact()).isFalse();
        assertThat(report.firstBreakIndex()).isEqualTo(1);
    }

    @Test
    void consistentFullRewriteStillCaughtBySignaturesOrAnchor() {
        SigningKeyRing ring = ringWithSignedKey();
        AuditChain chain = new AuditChain("adv", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");
        String anchor = AuditChainVerifier.verify(chain.records(), ring).headHash();

        // 攻击者删尾重算内部一致：无外锚时 intact（盲区）；带外锚即检出
        List<io.github.chyuan_cuihongyuan.buzhou.guard.audit.AgentAuditRecord> tailCut = chain.records().subList(0, 1);
        assertThat(AuditChainVerifier.verify(tailCut, ring).intact()).isTrue();
        assertThat(AuditChainVerifier.verify(tailCut, ring, anchor).anchored()).isFalse();

        // 攻击者整链重写重算哈希（无签名链，模拟纯哈希降级模式）：内部一致但锚点检出
        AuditChain unsigned = new AuditChain("adv", "2");
        unsigned.append("s1", "forged.event", "{}", "RECORDED");
        String forgedHead = AuditChainVerifier.verify(unsigned.records(),
                (io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing) null).headHash();
        assertThat(AuditChainVerifier.verify(unsigned.records(),
                (io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing) null, anchor).anchored()).isFalse();
        assertThat(forgedHead).isNotEqualTo(anchor);
    }

    private SigningKeyRing ringWithSignedKey() {
        java.security.KeyPair pair = AuditChain.generateKeyPair();
        return new SigningKeyRing(0, List.of(
                new SigningKeyProvider.VersionedSigningKey(1, pair.getPrivate(), pair.getPublic())));
    }
}
