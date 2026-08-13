package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IETF Agent Audit Trail（draft-sharif-agent-audit-trail-00）审计记录（wayfinder2 impl-22 / T50）：
 * 11 必填字段 + prev_hash 哈希链（{@code prev_hash(N) = SHA-256(JCS(record(N-1)))}），
 * 可选 ECDSA P-256 签名（IEEE P1363 r||s 64 字节 Base64url，非 JWS DER）。
 *
 * <p><b>数值约束</b>：本实现 JCS 子集仅接受整数（时间戳 epoch 毫秒、序号）——审计面
 * 不出现浮点数，规避 RFC 8785 的 ECMAScript number 规范化复杂度（诚实子集，非法数值即拒）。
 */
public record AgentAuditRecord(
        String recordId,
        long timestamp,
        String agentId,
        String agentVersion,
        String sessionId,
        String actionType,
        String actionDetail,
        String outcome,
        String trustLevel,
        String parentRecordId,
        String prevHash,
        String signature) {

    /** 无签名的字段视图（JCS 序列化/签名对象——去 signature 字段）。 */
    Map<String, Object> unsignedMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("record_id", recordId);
        map.put("timestamp", timestamp);
        map.put("agent_id", agentId);
        map.put("agent_version", agentVersion);
        map.put("session_id", sessionId);
        map.put("action_type", actionType);
        map.put("action_detail", actionDetail == null ? "" : actionDetail);
        map.put("outcome", outcome);
        map.put("trust_level", trustLevel == null ? "" : trustLevel);
        map.put("parent_record_id", parentRecordId == null ? "" : parentRecordId);
        map.put("prev_hash", prevHash == null ? "" : prevHash);
        return map;
    }

    /** 含签名的完整视图（导出/留档）。 */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>(unsignedMap());
        if (signature != null) {
            map.put("signature", signature);
        }
        return map;
    }

    public AgentAuditRecord withSignature(String signature) {
        return new AgentAuditRecord(recordId, timestamp, agentId, agentVersion, sessionId,
                actionType, actionDetail, outcome, trustLevel, parentRecordId, prevHash, signature);
    }
}
