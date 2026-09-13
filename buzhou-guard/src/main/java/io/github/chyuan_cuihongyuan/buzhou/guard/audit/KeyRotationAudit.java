package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 签名密钥轮换到期审计（spec 807 / T1115，cert-manager 证书到期监控借鉴）：
 * 对 (keyVersion → 激活时刻) 与策略（maxAge/warnBefore）做纯函数审计——
 * OVERDUE（超龄）/DUE_SOON（临期）/OK 三档发现，最坏者排前；无钥降级与
 * 「active 版本无激活记录」异常面如实呈现。
 *
 * <p>激活时刻由调用方提供（persister 侧记账；SigningKeyRing 本身是版本制——
 * 时间维不入环，保持其「只验不签」语义零侵入）。只读不轮换（rotate 是
 * SigningKeyRing 的写面——读数不改行为）。
 */
public final class KeyRotationAudit {

    /** 单钥发现（severity ∈ OVERDUE / DUE_SOON / OK / UNKNOWN_ACTIVE）。 */
    public record Finding(int version, long activatedAtMillis, long ageMillis, String severity) {
    }

    /** 审计报告（不可变）。 */
    public record Report(List<Finding> findings, Integer activeVersion, boolean hasSigningKey,
                         long maxAgeMillis, long warnBeforeMillis) {
    }

    private KeyRotationAudit() {
    }

    /**
     * 到期审计：severity 判据——age ≥ maxAge → OVERDUE；age ≥ maxAge −
     * warnBefore → DUE_SOON；否则 OK。UNKNOWN_ACTIVE：activeVersion 在激活
     * 记录中不存在（激活账本落后于环——异常面）。排序最坏在前
     * （OVERDUE → UNKNOWN_ACTIVE → DUE_SOON → OK，同级 age 降序）。
     */
    public static Report audit(Map<Integer, Long> activationTimes, Integer activeVersion,
                               boolean hasSigningKey, long nowMillis,
                               long maxAgeMillis, long warnBeforeMillis) {
        Objects.requireNonNull(activationTimes, "activationTimes");
        if (maxAgeMillis < 1 || warnBeforeMillis < 0) {
            throw new IllegalArgumentException("maxAgeMillis ≥ 1 且 warnBeforeMillis ≥ 0，实际 "
                    + maxAgeMillis + "/" + warnBeforeMillis);
        }
        if (warnBeforeMillis > maxAgeMillis) {
            throw new IllegalArgumentException("warnBeforeMillis 不得大于 maxAgeMillis（"
                    + warnBeforeMillis + " > " + maxAgeMillis + "）");
        }
        List<Finding> findings = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : activationTimes.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() < 0) {
                continue; // 脏账跳过
            }
            long age = nowMillis - e.getValue();
            String severity;
            if (age >= maxAgeMillis) {
                severity = "OVERDUE";
            } else if (age >= maxAgeMillis - warnBeforeMillis) {
                severity = "DUE_SOON";
            } else {
                severity = "OK";
            }
            findings.add(new Finding(e.getKey(), e.getValue(), age, severity));
        }
        if (hasSigningKey && activeVersion != null && activeVersion > 0
                && !activationTimes.containsKey(activeVersion)) {
            findings.add(new Finding(activeVersion, -1L, -1L, "UNKNOWN_ACTIVE"));
        }
        findings.sort(Comparator
                .comparingInt((Finding f) -> rank(f.severity()))
                .thenComparing(Comparator.comparingLong(Finding::ageMillis).reversed()));
        return new Report(List.copyOf(findings), activeVersion, hasSigningKey,
                maxAgeMillis, warnBeforeMillis);
    }

    private static int rank(String severity) {
        return switch (severity) {
            case "OVERDUE" -> 0;
            case "UNKNOWN_ACTIVE" -> 1;
            case "DUE_SOON" -> 2;
            default -> 3;
        };
    }
}
