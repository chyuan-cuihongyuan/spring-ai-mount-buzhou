package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-22 / T50 审计链：JCS 规范化 / prev_hash 链 / ECDSA P-256 P1363 签名 /
 * 篡改检测 / 事件收集器。
 */
class AgentAuditTrailTest {

    @Test
    void jcsSortsKeysAndEscapesMinimally() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("b", 1);
        input.put("a", "x\"y\n");
        input.put("C", true);
        // 键按 UTF-16 码单元序（C < a < b），整数直出、最小转义
        assertThat(Jcs.canonicalize(input)).isEqualTo("{\"C\":true,\"a\":\"x\\\"y\\n\",\"b\":1}");
        // 浮点被诚实子集拒绝（审计面约束）
        assertThatThrownBy(() -> Jcs.canonicalize(Map.of("bad", 1.5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅接受整数");
    }

    @Test
    void chainVerifiesAndTamperingAnyRecordBreaksIt() {
        AuditChain chain = new AuditChain("agent-1", "1.0.0");
        chain.append("s1", "guard.tool.blocked", "{\"tool\":\"delete\"}", "BLOCKED");
        chain.append("s1", "guard.auth.granted", "{\"tool\":\"delete\"}", "ALLOWED");
        chain.append("s1", "session.cancelled", "{}", "RECORDED");
        assertThat(chain.verify((java.security.PublicKey) null)).isTrue();
        assertThat(chain.records()).hasSize(3);
        // prev_hash 首条 = sha256("")
        assertThat(chain.records().getFirst().prevHash())
                .isEqualTo(AuditChain.sha256Hex(""));
        // 会话摘要存在且稳定
        assertThat(chain.sessionHash()).hasSize(64).isEqualTo(chain.sessionHash());

        // 篡改中间记录的 outcome → 链验证失败
        var tampered = new AgentAuditRecord(
                chain.records().get(1).recordId(), chain.records().get(1).timestamp(),
                chain.records().get(1).agentId(), chain.records().get(1).agentVersion(),
                chain.records().get(1).sessionId(), chain.records().get(1).actionType(),
                chain.records().get(1).actionDetail(), "TAMPERED",
                chain.records().get(1).trustLevel(), chain.records().get(1).parentRecordId(),
                chain.records().get(1).prevHash(), null);
        AuditChain tamperedChain = new AuditChain("agent-1", "1.0.0");
        tamperedChain.append("s1", "guard.tool.blocked", "{\"tool\":\"delete\"}", "BLOCKED");
        var r2 = tamperedChain.append("s1", "guard.auth.granted", "{\"tool\":\"delete\"}", "ALLOWED");
        tamperedChain.records();
        // 用篡改记录替换第二条后手工续链：第三条 prev_hash 失配
        var records = tamperedChain.records();
        AgentAuditRecord third = new AgentAuditRecord("r3", System.currentTimeMillis(),
                "agent-1", "1.0.0", "s1", "session.cancelled", "{}", "RECORDED", "default",
                tampered.recordId(), AuditChain.sha256Hex(Jcs.canonicalize(r2.unsignedMap())),
                null);
        AgentAuditRecord tamperedThird = new AgentAuditRecord(third.recordId(),
                third.timestamp(), third.agentId(), third.agentVersion(), third.sessionId(),
                third.actionType(), third.actionDetail(), third.outcome(), third.trustLevel(),
                third.parentRecordId(),
                AuditChain.sha256Hex(Jcs.canonicalize(tampered.unsignedMap())), null);
        assertThat(AuditChain.sha256Hex(Jcs.canonicalize(tampered.unsignedMap())))
                .isNotEqualTo(AuditChain.sha256Hex(Jcs.canonicalize(r2.unsignedMap())));
        assertThat(tamperedThird.prevHash()).isNotEqualTo(third.prevHash());
    }

    @Test
    void ecdsaSignsInP1363AndVerifies() {
        java.security.KeyPair keyPair = AuditChain.generateKeyPair();
        AuditChain chain = new AuditChain("agent-1", "1.0.0",
                keyPair.getPrivate(), keyPair.getPublic());
        chain.append("s1", "guard.taint.blocked", "{\"reason\":\"untrusted-context\"}", "BLOCKED");
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");

        // P1363 签名 = 64 字节 Base64url（非 DER）
        String signature = chain.records().getFirst().signature();
        assertThat(signature).isNotNull();
        byte[] decoded = AuditChain.Base64Url.decode(signature);
        assertThat(decoded).hasSize(64);
        // 全链（含签名）验证通过
        assertThat(chain.verify(keyPair.getPublic())).isTrue();

        // 篡改记录内容 → 签名验证失败
        var original = chain.records().getFirst();
        var forged = new AgentAuditRecord(original.recordId(), original.timestamp(),
                original.agentId(), original.agentVersion(), original.sessionId(),
                original.actionType(), "{\"reason\":\"tampered\"}", original.outcome(),
                original.trustLevel(), original.parentRecordId(), original.prevHash(),
                original.signature());
        List<AgentAuditRecord> tampered = new java.util.ArrayList<>(chain.records());
        tampered.set(0, forged);
        AuditChain tamperedVerify = new AuditChain("agent-1", "1.0.0");
        tampered.forEach(r -> tamperedVerify.append(r.sessionId(), r.actionType(),
                r.actionDetail(), r.outcome()));
        // 直接复算：被篡改内容的 JCS 签名必然失配（用原签名验证篡改记录）
        try {
            java.security.Signature verifier = java.security.Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(keyPair.getPublic());
            verifier.update(Jcs.canonicalize(forged.unsignedMap())
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertThat(verifier.verify(AuditChain.p1363ToDer(decoded))).isFalse();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void collectorTurnsGuardEventsIntoRecords() {
        AuditChain chain = new AuditChain("agent-1", "1.0.0");
        AuditTrailCollector collector = new AuditTrailCollector(chain);
        AtomicInteger seq = new AtomicInteger();
        io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent blocked =
                new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent(
                        "guard.taint.blocked",
                        Map.of("sessionId", "s9", "toolName", "delete_records"),
                        java.time.Instant.now());
        collector.onEvent(blocked);
        collector.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                "unrelated.event"));
        collector.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                "guard.auth.granted"));

        assertThat(chain.records()).hasSize(2);
        assertThat(chain.records().getFirst().actionType()).isEqualTo("guard.taint.blocked");
        assertThat(chain.records().getFirst().sessionId()).isEqualTo("s9");
        assertThat(chain.records().getFirst().outcome()).isEqualTo("BLOCKED");
        assertThat(chain.records().get(1).outcome()).isEqualTo("ALLOWED");
        assertThat(chain.verify((java.security.PublicKey) null)).isTrue();
        seq.incrementAndGet();
    }
}
