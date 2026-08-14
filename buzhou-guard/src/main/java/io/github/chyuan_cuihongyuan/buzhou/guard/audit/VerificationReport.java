package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.util.Map;

/**
 * 审计链校验报告（impl-39 / spec 13 §T64）：全量重放结果——
 * 首个断点定位（{@code firstBreakIndex}/{@code brokenRecordId}）+ 密钥版本分布。
 *
 * @param verifiedCount   通过重放的记录数（含无签名记录）
 * @param firstBreakIndex 首个断点下标（-1 = 全链完整）
 * @param brokenRecordId  断点记录 id（null = 全链完整）
 * @param breakReason     断点人类可读原因（null = 全链完整）
 * @param keyVersionStats 版本分布（"unsigned" 或版本号字符串 → 记录数）
 */
public record VerificationReport(
        long verifiedCount,
        int firstBreakIndex,
        String brokenRecordId,
        String breakReason,
        Map<String, Long> keyVersionStats) {

    /** 全链完整（无断点）。 */
    public boolean intact() {
        return firstBreakIndex < 0;
    }
}
