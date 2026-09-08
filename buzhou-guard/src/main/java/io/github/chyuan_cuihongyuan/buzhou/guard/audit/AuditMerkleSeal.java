package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

/**
 * 审计 Merkle 时点封印（spec 404 / T700，Certificate Transparency STH
 * 思想）：截至 sealedAt 的 recordCount 条记录构成根 rootHex——根可对外
 * 发布，单条记录凭包含证明+根即可验。快照式：链照常生长，印描述
 * 「截至此刻」。
 */
public record AuditMerkleSeal(long sealedAtEpochMs, int recordCount, String rootHex) {
}
