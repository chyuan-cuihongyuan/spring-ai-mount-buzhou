package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 344 / impl-367：审计链完整性巡检回归——干净 UP / 断链 DOWN 定位 /
 * 空链 UP / 超窗 UNKNOWN 带修法 / 审计关 UNKNOWN。
 */
class AuditChainHealthTest {

    /** 真实链铺底：chain append → store（collector 同路径语义）。 */
    private static InMemoryAuditRecordStore seededChain(int count, SigningKeyRing keyRing) {
        AuditChain chain = new AuditChain("agent-1", "1.0", keyRing);
        for (int i = 0; i < count; i++) {
            chain.append("sess-" + i, "HITL", "detail-" + i, "APPROVED");
        }
        InMemoryAuditRecordStore store = new InMemoryAuditRecordStore();
        chain.records().forEach(store::append);
        return store;
    }

    @Test
    void cleanChainIsUp_withVerifiedDetails() {
        SigningKeyRing keyRing = new SigningKeyRing();
        AuditChainHealth health = new AuditChainHealth(true,
                seededChain(5, keyRing), keyRing);
        assertThat(health.status()).isEqualTo(BuzhouHealth_Status.UP);
        assertThat(health.mechanism()).isEqualTo("guard-audit-chain");
        assertThat(health.details()).containsEntry("records", 5L)
                .containsEntry("verifiedCount", 5L)
                .containsKey("headHash");
    }

    @Test
    void tamperedMiddleRecordIsDown_breakLocated() {
        SigningKeyRing keyRing = new SigningKeyRing();
        AuditChain chain = new AuditChain("agent-1", "1.0", keyRing);
        for (int i = 0; i < 5; i++) {
            chain.append("sess-" + i, "HITL", "detail-" + i, "APPROVED");
        }
        List<AgentAuditRecord> originals = chain.records();
        // 重铺到新 store：中段记录 actionDetail 被篡改（链哈希失配）
        InMemoryAuditRecordStore tampered = new InMemoryAuditRecordStore();
        for (int i = 0; i < originals.size(); i++) {
            AgentAuditRecord r = originals.get(i);
            if (i == 2) {
                tampered.append(new AgentAuditRecord(r.recordId(), r.timestamp(),
                        r.agentId(), r.agentVersion(), r.sessionId(), r.actionType(),
                        "TAMPERED", r.outcome(), r.trustLevel(), r.parentRecordId(),
                        r.prevHash(), r.signature(), r.keyVersion()));
            } else {
                tampered.append(r);
            }
        }
        AuditChainHealth health = new AuditChainHealth(true, tampered, keyRing);
        assertThat(health.status()).isEqualTo(BuzhouHealth_Status.DOWN);
        assertThat(health.details()).containsKeys("firstBreakIndex", "breakReason");
    }

    @Test
    void emptyChainIsUp_nothingHappenedIsNotSick() {
        SigningKeyRing keyRing = new SigningKeyRing();
        AuditChainHealth health = new AuditChainHealth(true,
                new InMemoryAuditRecordStore(), keyRing);
        assertThat(health.status()).isEqualTo(BuzhouHealth_Status.UP);
        assertThat(health.details()).containsEntry("empty", true);
    }

    @Test
    void chainLongerThanWindowIsUnknown_withAction() {
        SigningKeyRing keyRing = new SigningKeyRing();
        AuditChainHealth health = new AuditChainHealth(true,
                seededChain(5, keyRing), keyRing, 3); // 窗 3 < 链 5
        assertThat(health.status()).isEqualTo(BuzhouHealth_Status.UNKNOWN);
        assertThat(health.details()).containsEntry("reason", "chain-longer-than-verify-window")
                .containsKey("action"); // 带修法
    }

    @Test
    void auditDisabledIsUnknown_notDown() {
        SigningKeyRing keyRing = new SigningKeyRing();
        AuditChainHealth health = new AuditChainHealth(false,
                seededChain(3, keyRing), keyRing);
        assertThat(health.status()).isEqualTo(BuzhouHealth_Status.UNKNOWN);
        assertThat(health.details()).containsEntry("reason", "audit-disabled");
    }

    // BuzhouHealth.Status 静态导入等价（guard 模块可见）
    private static final class BuzhouHealth_Status {
        static final io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status UP =
                io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP;
        static final io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status DOWN =
                io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.DOWN;
        static final io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status UNKNOWN =
                io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UNKNOWN;
    }
}
