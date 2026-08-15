package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * 审计链独立校验器（impl-39 / spec 13 §T64）：独立于生产进程——输入导出记录集
 * （{@link AuditRecordStore#loadAll()} 或 JSON 导出）+ 密钥环，全量重放哈希链与签名，
 * {@link VerificationReport} 定位<b>首个断点</b>（事后篡改可被证明并定位）。
 *
 * <p>校验规则（任一失败即停在该记录）：
 * <ol>
 *   <li>{@code prev_hash} 链一致（首条 = SHA-256("")，其后逐条承接前条 JCS 哈希）；</li>
 *   <li>{@code parent_record_id} 承接前条 record_id；</li>
 *   <li>有签名记录按 {@code keyVersion} 取公钥可验（未知版本 / 低于 minVerifyVersion /
 *       签名失配 = 断）；无签名记录跳过签名校验（纯哈希链兼容）。</li>
 * </ol>
 */
public final class AuditChainVerifier {

    private AuditChainVerifier() {
    }

    /** 密钥环模式：按记录 keyVersion 验签（keyRing 为 null 时仅校验哈希链，遇签名即断）。 */
    public static VerificationReport verify(List<AgentAuditRecord> records, SigningKeyRing keyRing) {
        return verify(records, keyRing, null);
    }

    /**
     * spec 41 §A / T153 / impl-124：带链外锚点校验——expectedHeadAnchor 为运维在链外
     * （保险库/配置/异地）保存的期望链头哈希；链内部一致但链头与锚点不符 →
     * {@code anchorMatched=false}（删尾或整链重写可检测——纯内部一致性校验的盲区）。
     */
    public static VerificationReport verify(List<AgentAuditRecord> records, SigningKeyRing keyRing,
            String expectedHeadAnchor) {
        VerificationReport report = verify(records, (version, record) -> {
            if (record.signature() == null) {
                return true;
            }
            java.security.PublicKey key = keyRing == null ? null : keyRing.verifyKey(version);
            return key != null && SignatureOps.verify(record, key);
        });
        if (expectedHeadAnchor == null || expectedHeadAnchor.isBlank()) {
            return report;
        }
        boolean matched = report.headHash() != null
                && report.headHash().equalsIgnoreCase(expectedHeadAnchor.trim());
        return new VerificationReport(report.verifiedCount(), report.firstBreakIndex(),
                report.brokenRecordId(), report.breakReason(), report.keyVersionStats(),
                report.headHash(), matched);
    }

    /** 单钥模式（impl-22 兼容：全部签名用同一公钥验）。 */
    public static VerificationReport verify(List<AgentAuditRecord> records,
            java.security.PublicKey singleKey) {
        return verify(records, (version, record) ->
                record.signature() == null
                        || (singleKey != null && SignatureOps.verify(record, singleKey)));
    }

    /** 核心：签名检查回调（version, record)→bool；无签名记录由调用方跳过。 */
    static VerificationReport verify(List<AgentAuditRecord> records,
            BiPredicate<Integer, AgentAuditRecord> signatureCheck) {
        long verified = 0;
        int firstBreakIndex = -1;
        String brokenRecordId = null;
        String breakReason = null;
        Map<String, Long> keyVersionStats = new LinkedHashMap<>();
        String expectedPrev = AuditChain.sha256Hex("");
        AgentAuditRecord previous = null;
        for (int i = 0; i < records.size(); i++) {
            AgentAuditRecord record = records.get(i);
            if (!record.prevHash().equals(expectedPrev)) {
                firstBreakIndex = i;
                brokenRecordId = record.recordId();
                breakReason = "prev_hash 链断裂（index=" + i + "）";
                break;
            }
            if (previous != null && !Objects.equals(record.parentRecordId(),
                    previous.recordId())) {
                firstBreakIndex = i;
                brokenRecordId = record.recordId();
                breakReason = "parent_record_id 承接断裂（index=" + i + "）";
                break;
            }
            if (!signatureCheck.test(record.keyVersion(), record)) {
                firstBreakIndex = i;
                brokenRecordId = record.recordId();
                breakReason = record.keyVersion() > 0
                        ? "签名不可验（keyVersion=" + record.keyVersion() + "，index=" + i + "）"
                        : "签名失配（index=" + i + "）";
                break;
            }
            verified++;
            String versionLabel = record.signature() == null ? "unsigned"
                    : String.valueOf(record.keyVersion());
            keyVersionStats.merge(versionLabel, 1L, Long::sum);
            expectedPrev = AuditChain.sha256Hex(Jcs.canonicalize(record.unsignedMap()));
            previous = record;
        }
        // spec 41 §A / T153：链头哈希 = 已重放前缀末条记录的 JCS 哈希（空链 null）
        String headHash = verified > 0 ? expectedPrev : null;
        return new VerificationReport(verified, firstBreakIndex, brokenRecordId, breakReason,
                keyVersionStats, headHash, null);
    }

    /** 签名原语（P1363 验证，与 AuditChain 共享实现）。 */
    static final class SignatureOps {

        static boolean verify(AgentAuditRecord record, java.security.PublicKey key) {
            try {
                java.security.Signature verifier = java.security.Signature
                        .getInstance("SHA256withECDSA");
                verifier.initVerify(key);
                verifier.update(Jcs.canonicalize(record.unsignedMap())
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return verifier.verify(AuditChain.p1363ToDer(
                        AuditChain.Base64Url.decode(record.signature())));
            } catch (Exception e) {
                return false;
            }
        }

        private SignatureOps() {
        }
    }
}
