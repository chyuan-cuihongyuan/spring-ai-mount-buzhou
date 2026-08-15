package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.util.Map;

/**
 * 审计链校验报告（impl-39 / spec 13 §T64）：全量重放结果——
 * 首个断点定位（{@code firstBreakIndex}/{@code brokenRecordId}）+ 密钥版本分布。
 *
 * <p>spec 41 §A / T153 / impl-124 增补链外锚定：{@code headHash} 为重放推得的链头哈希
 * （末条记录 JCS 哈希；空链 null）；{@code anchorMatched} 在调用方提供外部锚点
 * （{@link AuditChainVerifier#verify(List, SigningKeyRing, String)}）时为非空布尔——
 * {@code false} = 链尾与外部锚点不符（删尾/整链重写可检测），null = 未提供锚点（跳过）。
 *
 * @param verifiedCount   通过重放的记录数（含无签名记录）
 * @param firstBreakIndex 首个断点下标（-1 = 全链完整）
 * @param brokenRecordId  断点记录 id（null = 全链完整）
 * @param breakReason     断点人类可读原因（null = 全链完整）
 * @param keyVersionStats 版本分布（"unsigned" 或版本号字符串 → 记录数）
 * @param headHash        重放推得的链头哈希（空链 null；断链时为断点前已验部分的推得值）
 * @param anchorMatched   外部锚点比对结果（null = 未提供锚点）
 */
public record VerificationReport(
        long verifiedCount,
        int firstBreakIndex,
        String brokenRecordId,
        String breakReason,
        Map<String, Long> keyVersionStats,
        String headHash,
        Boolean anchorMatched) {

    /** 兼容构造（无锚点面：headHash/anchorMatched 未知）。 */
    public VerificationReport(long verifiedCount, int firstBreakIndex, String brokenRecordId,
            String breakReason, Map<String, Long> keyVersionStats) {
        this(verifiedCount, firstBreakIndex, brokenRecordId, breakReason, keyVersionStats, null, null);
    }

    /** 全链完整（无断点）。 */
    public boolean intact() {
        return firstBreakIndex < 0;
    }

    /** 链外锚定通过：链完整且（提供了锚点时）链头与锚点一致。 */
    public boolean anchored() {
        return intact() && (anchorMatched == null || anchorMatched);
    }
}
