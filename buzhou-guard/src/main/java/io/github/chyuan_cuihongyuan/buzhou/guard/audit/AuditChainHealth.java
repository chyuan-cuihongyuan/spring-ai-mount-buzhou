package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计链完整性巡检（spec 344 / T679，Certificate Transparency / 区块链
 * 全节点验证借鉴）：status() 时 loadAll → {@link AuditChainVerifier}——
 * <b>断链 DOWN</b>（可读 ≠ 完整：审计被篡改是最高级事故，不被「存储
 * 可读」的绿掩盖，GuardHealth 管可读、本面管完整）。
 *
 * <ul>
 *   <li><b>UP</b>：全链校验干净（details verifiedCount/headHash）；空链
 *       也 UP（无事发生不是病）；</li>
 *   <li><b>DOWN</b>：断链/签名失效——details 定位首个断点
 *       （firstBreakIndex/brokenRecordId/breakReason）；</li>
 *   <li><b>UNKNOWN</b>：审计关闭/无存储/无签名密钥（降级运行不是 DOWN
 *       ——GuardHealth 同口径）；<b>链长超巡检窗</b>（默认
 *       {@value #DEFAULT_VERIFY_WINDOW} 条，防健康探针变全表扫描）——带
 *       修法诚实降级（全量校验归宿主手动），宁 UNKNOWN 不做段校验
 *       （段首位断链语义模糊）。</li>
 * </ul>
 *
 * <p>只读探针（verify 不改链）；DOWN 自动流入 312 告警与 332 探针裁决
 * （健康面聚合既有管道零新管道）。
 */
public final class AuditChainHealth implements BuzhouHealth {

    /** 巡检窗默认（防探针全表扫描）。 */
    public static final int DEFAULT_VERIFY_WINDOW = 10_000;

    private final boolean auditEnabled;
    private final AuditRecordStore store;
    private final SigningKeyRing keyRing;
    private final int verifyWindow;

    public AuditChainHealth(boolean auditEnabled, AuditRecordStore store,
            SigningKeyRing keyRing) {
        this(auditEnabled, store, keyRing, DEFAULT_VERIFY_WINDOW);
    }

    public AuditChainHealth(boolean auditEnabled, AuditRecordStore store,
            SigningKeyRing keyRing, int verifyWindow) {
        this.auditEnabled = auditEnabled;
        this.store = store;
        this.keyRing = keyRing;
        this.verifyWindow = Math.max(1, verifyWindow);
    }

    @Override
    public String mechanism() {
        return "guard-audit-chain";
    }

    @Override
    public Status status() {
        if (!auditEnabled || store == null) {
            return Status.UNKNOWN;
        }
        List<AgentAuditRecord> records = store.loadAll();
        if (records.isEmpty()) {
            return Status.UP; // 空链健康——无事发生不是病
        }
        if (records.size() > verifyWindow) {
            return Status.UNKNOWN; // 超窗诚实降级（details 带修法）
        }
        VerificationReport report = AuditChainVerifier.verify(records, keyRing);
        return report.firstBreakIndex() >= 0 ? Status.DOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (!auditEnabled || store == null) {
            details.put("reason", auditEnabled ? "no-store" : "audit-disabled");
            return details;
        }
        List<AgentAuditRecord> records = store.loadAll();
        details.put("records", (long) records.size());
        if (records.isEmpty()) {
            details.put("empty", true);
            return details;
        }
        if (records.size() > verifyWindow) {
            details.put("reason", "chain-longer-than-verify-window");
            details.put("verifyWindow", (long) verifyWindow);
            details.put("action", "宿主手动全量校验：loadAll() + AuditChainVerifier.verify");
            return details;
        }
        VerificationReport report = AuditChainVerifier.verify(records, keyRing);
        details.put("verifiedCount", report.verifiedCount());
        if (report.headHash() != null) {
            details.put("headHash", report.headHash());
        }
        if (!report.keyVersionStats().isEmpty()) {
            details.put("keyVersions", report.keyVersionStats());
        }
        if (report.firstBreakIndex() >= 0) {
            details.put("firstBreakIndex", (long) report.firstBreakIndex());
            details.put("brokenRecordId", String.valueOf(report.brokenRecordId()));
            details.put("breakReason", String.valueOf(report.breakReason()));
        }
        return details;
    }
}
