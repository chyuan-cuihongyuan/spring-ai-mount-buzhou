package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * guard 机制健康（impl-41 / spec 13 §T66）：核心职能 = 审计链在线可读。
 * 探针 = {@link AuditRecordStore#count()}（读路径一次真实往返——JDBC 断连即抛）；
 * <b>DOWN 仅当审计存储不可读</b>（无签名密钥/未配策略是降级运行，不是 DOWN）；
 * 审计关闭报 UNKNOWN。详情含策略 revision（provenance 可观测）。
 */
public final class GuardHealth implements BuzhouHealth {

    private final boolean auditEnabled;
    private final AuditRecordStore auditStore;
    private final PolicyRefresher policyRefresher;

    public GuardHealth(boolean auditEnabled, AuditRecordStore auditStore,
            PolicyRefresher policyRefresher) {
        this.auditEnabled = auditEnabled;
        this.auditStore = auditStore;
        this.policyRefresher = policyRefresher;
    }

    @Override
    public String mechanism() {
        return "guard";
    }

    @Override
    public Status status() {
        if (!auditEnabled || auditStore == null) {
            return Status.UNKNOWN;
        }
        try {
            auditStore.count();
            return Status.UP;
        } catch (RuntimeException e) {
            return Status.DOWN;
        }
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("auditEnabled", auditEnabled);
        if (auditStore != null) {
            details.put("auditStore", auditStore.getClass().getSimpleName());
        }
        if (policyRefresher != null) {
            details.put("policyRevision", policyRefresher.revision());
            details.put("policyRules", policyRefresher.ruleCount());
        }
        return details;
    }
}
