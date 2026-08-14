package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 授权策略裁决（wayfinder2 impl-23 / T52 / docs/spec/12 §guard-24）：
 * allow / deny / escalate（→ HITL 人工审批），附 reason（可分析、可观测）。
 *
 * <p>impl-40 / spec 13 §T64：新增 <b>provenance</b>（{@code revision} 快照版本 +
 * {@code activatedAt} 激活时刻）——热加载后每个决策可溯源到规则版本（决策审计面）。
 * 旧两参构造保留（revision 空 = 静态策略无版本）。
 */
public record PolicyDecision(Action action, String reason, String revision,
                             Instant activatedAt) {

    public enum Action {
        ALLOW,
        DENY,
        /** 升级为人工审批（FIDES approver / 既有授权台账通道）。 */
        ESCALATE
    }

    public PolicyDecision(Action action, String reason) {
        this(action, reason, "", null);
    }

    public static PolicyDecision allow(String reason) {
        return new PolicyDecision(Action.ALLOW, reason);
    }

    public static PolicyDecision deny(String reason) {
        return new PolicyDecision(Action.DENY, reason);
    }

    public static PolicyDecision escalate(String reason) {
        return new PolicyDecision(Action.ESCALATE, reason);
    }

    /** 策略引擎输入：主体 × 工具 × 资源 × label 谓词（对齐 OPA「结构化 input→决策+reason」模型）。 */
    public record Input(String principal, String toolName, Map<String, Object> arguments,
                        Map<String, String> labels, boolean humanApproved) {
        public Input {
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
            labels = labels == null ? Map.of() : Map.copyOf(labels);
        }
    }

    /** 声明式规则（内嵌可分析子集；默认拒——无 allow 命中即 deny）。 */
    public record Rule(String id, String toolPattern, List<LabelPredicate> labelPredicates,
                       PolicyDecision decision) {
    }

    /** label 谓词：label 值的等于/存在/包含判定（如 taint=UNTRUSTED）。 */
    public record LabelPredicate(String key, String op, String value) {
        boolean matches(Map<String, String> labels) {
            String actual = labels.get(key);
            return switch (op) {
                case "eq" -> value != null && value.equals(actual);
                case "exists" -> actual != null;
                case "contains" -> actual != null && value != null && actual.contains(value);
                default -> false;
            };
        }
    }
}
