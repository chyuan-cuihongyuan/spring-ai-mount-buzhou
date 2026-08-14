package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-39 / spec 13 §T64 独立校验器：全量重放定位首个断点（篡改任一记录）+ 密钥版本
 * 分布统计 + 跨重启（JDBC 持久化 → 全量加载 → 独立重放）链可验。
 */
class AuditChainVerifierTest {

    private SigningKeyRing ringWith(KeyPair... pairs) {
        List<SigningKeyProvider.VersionedSigningKey> keys = new ArrayList<>();
        for (int i = 0; i < pairs.length; i++) {
            keys.add(new SigningKeyProvider.VersionedSigningKey(i + 1,
                    pairs[i].getPrivate(), pairs[i].getPublic()));
        }
        return new SigningKeyRing(0, keys);
    }

    @Test
    void intactChainPassesAndReportsKeyVersionStats() {
        KeyPair v1 = AuditChain.generateKeyPair();
        KeyPair v2 = AuditChain.generateKeyPair();
        SigningKeyRing ring = ringWith(v1);                          // active=v1
        AuditChain chain = new AuditChain("a", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");   // v1

        ring.rotate(2, v2);                                          // 轮换到新钥
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");   // v2

        VerificationReport report = AuditChainVerifier.verify(chain.records(), ring);
        assertThat(report.intact()).isTrue();
        assertThat(report.verifiedCount()).isEqualTo(2);
        assertThat(report.firstBreakIndex()).isEqualTo(-1);
        assertThat(report.keyVersionStats())
                .containsEntry("1", 1L)
                .containsEntry("2", 1L);

        // 轮换后旧记录仍可验（旧钥公钥在环）、新记录用新钥
        assertThat(chain.records().get(0).keyVersion()).isEqualTo(1);
        assertThat(chain.records().get(1).keyVersion()).isEqualTo(2);
        assertThat(chain.verify(ring)).isTrue();
    }

    @Test
    void tamperingAnyRecordLocatesFirstBreak() {
        SigningKeyRing ring = ringWith(AuditChain.generateKeyPair());
        AuditChain chain = new AuditChain("a", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");
        chain.append("s1", "session.cancelled", "{}", "RECORDED");

        // 篡改中间（index=1）记录内容 → prev_hash 链仍承接，但该记录自身签名对不上内容
        // （JCS 覆盖 action_detail）——签名校验先把断点定位到篡改记录本身
        AgentAuditRecord original = chain.records().get(1);
        AgentAuditRecord tampered = new AgentAuditRecord(original.recordId(),
                original.timestamp(), original.agentId(), original.agentVersion(),
                original.sessionId(), original.actionType(), "{\"evil\":true}",
                original.outcome(), original.trustLevel(), original.parentRecordId(),
                original.prevHash(), original.signature(), original.keyVersion());
        List<AgentAuditRecord> tamperedRecords = new ArrayList<>(chain.records());
        tamperedRecords.set(1, tampered);

        VerificationReport report = AuditChainVerifier.verify(tamperedRecords, ring);
        assertThat(report.intact()).isFalse();
        assertThat(report.firstBreakIndex()).isEqualTo(1);
        assertThat(report.brokenRecordId()).isEqualTo(original.recordId());
        assertThat(report.breakReason()).contains("签名");
        assertThat(report.verifiedCount()).isEqualTo(1);

        // 无签名（纯哈希链）同款篡改 → 下一条 prev_hash 失配，断点后移到 index=2
        AuditChain unsignedChain = new AuditChain("a", "1");
        unsignedChain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        unsignedChain.append("s1", "guard.auth.granted", "{}", "ALLOWED");
        unsignedChain.append("s1", "session.cancelled", "{}", "RECORDED");
        List<AgentAuditRecord> unsigned = new ArrayList<>(unsignedChain.records());
        AgentAuditRecord mid = unsigned.get(1);
        unsigned.set(1, new AgentAuditRecord(mid.recordId(), mid.timestamp(), mid.agentId(),
                mid.agentVersion(), mid.sessionId(), mid.actionType(), "{\"evil\":true}",
                mid.outcome(), mid.trustLevel(), mid.parentRecordId(), mid.prevHash(), null, 0));
        VerificationReport hashBreak = AuditChainVerifier.verify(unsigned, (SigningKeyRing) null);
        assertThat(hashBreak.firstBreakIndex()).isEqualTo(2);
        assertThat(hashBreak.brokenRecordId()).isEqualTo(unsignedChain.records().get(2).recordId());
        assertThat(hashBreak.breakReason()).contains("prev_hash");

        // 篡改首条记录 → 断在 index=0
        AgentAuditRecord first = chain.records().getFirst();
        tamperedRecords.set(0, new AgentAuditRecord(first.recordId(), first.timestamp(),
                first.agentId(), first.agentVersion(), first.sessionId(), first.actionType(),
                "{\"evil\":true}", first.outcome(), first.trustLevel(), first.parentRecordId(),
                AuditChain.sha256Hex("fake-prev"), first.signature(), first.keyVersion()));
        VerificationReport firstBreak = AuditChainVerifier.verify(tamperedRecords, ring);
        assertThat(firstBreak.firstBreakIndex()).isZero();
    }

    @Test
    void forgedSignatureBreaksAtThatRecord() {
        KeyPair realKey = AuditChain.generateKeyPair();
        SigningKeyRing ring = ringWith(realKey);
        AuditChain chain = new AuditChain("a", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");

        // 保留 prev_hash，仅换签名（伪造：另一密钥签的）
        KeyPair forger = AuditChain.generateKeyPair();
        SigningKeyRing forgerRing = ringWith(forger);
        AuditChain forgedChain = new AuditChain("a", "1", forgerRing);
        AgentAuditRecord forgedRecord = forgedChain.append("s1", "guard.tool.blocked", "{}",
                "BLOCKED");
        AgentAuditRecord original = chain.records().getFirst();
        AgentAuditRecord forged = new AgentAuditRecord(original.recordId(),
                original.timestamp(), original.agentId(), original.agentVersion(),
                original.sessionId(), original.actionType(), original.actionDetail(),
                original.outcome(), original.trustLevel(), original.parentRecordId(),
                original.prevHash(), forgedRecord.signature(), original.keyVersion());

        VerificationReport report = AuditChainVerifier.verify(List.of(forged), ring);
        assertThat(report.intact()).isFalse();
        assertThat(report.firstBreakIndex()).isZero();
        assertThat(report.breakReason()).contains("签名");
    }

    @Test
    void crossRestartChainVerifiableViaJdbcPersistence() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:audit-restart-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa", "");
        JdbcAuditRecordStore store = new JdbcAuditRecordStore(new JdbcTemplate(dataSource));

        KeyPair v1 = AuditChain.generateKeyPair();
        SigningKeyRing ring = ringWith(v1);
        // 「进程 1」：链上 2 条 + 即时落库
        AuditChain process1 = new AuditChain("a", "1", ring);
        process1.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        process1.append("s1", "guard.auth.granted", "{}", "ALLOWED");
        process1.records().forEach(store::append);
        assertThat(store.count()).isEqualTo(2);

        // 「进程 2」：从持久化续链再追加 1 条（只落新增记录，恢复的旧记录不重复 append）
        SigningKeyRing ring2 = ringWith(v1);
        AuditChain process2 = new AuditChain("a", "1", ring2);
        process2.resume(store.loadAll());
        AgentAuditRecord resumed = process2.append("s1", "session.cancelled", "{}", "RECORDED");
        store.append(resumed);

        // 独立校验器全量重放（模拟 nightly 重放校验）：跨重启链完整
        VerificationReport report = AuditChainVerifier.verify(store.loadAll(), ring2);
        assertThat(report.intact()).isTrue();
        assertThat(report.verifiedCount()).isEqualTo(3);
        assertThat(report.keyVersionStats()).containsEntry("1", 3L);
    }

    @Test
    void pureHashChainWithoutKeysStillVerifies() {
        AuditChain chain = new AuditChain("a", "1");
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        VerificationReport report = AuditChainVerifier.verify(chain.records(), (SigningKeyRing) null);
        assertThat(report.intact()).isTrue();
        assertThat(report.keyVersionStats()).containsEntry("unsigned", 1L);
    }
}
