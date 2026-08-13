package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import java.util.List;

/**
 * 内嵌可分析策略子集（wayfinder2 impl-23 / T52）：声明式规则——主体 × 工具（glob）×
 * label 谓词 → allow/deny/escalate；<b>默认拒</b>、决策附 reason；首条命中生效；
 * ESCALATE + 人工已审批 = allow（FIDES approver 通道语义）。
 *
 * <p>语义对齐 OPA「结构化 input → 决策 + reason」模型（OPA 为达标概念源 12,099★；
 * JVM 无成熟内嵌故自有子集——见 spec 12 §guard-24）。
 */
public final class EmbeddedPolicyEngine implements PolicyEngine {

    private final List<PolicyDecision.Rule> rules;

    public EmbeddedPolicyEngine(List<PolicyDecision.Rule> rules) {
        this.rules = rules == null ? List.of() : List.copyOf(rules);
    }

    @Override
    public PolicyDecision decide(PolicyDecision.Input input) {
        if (input == null || input.toolName() == null) {
            return PolicyDecision.deny("策略输入缺失（toolName 为空）——默认拒");
        }
        for (PolicyDecision.Rule rule : rules) {
            if (!matchesTool(rule.toolPattern(), input.toolName())) {
                continue;
            }
            boolean labelsMatch = rule.labelPredicates() == null
                    || rule.labelPredicates().stream()
                            .allMatch(predicate -> predicate.matches(input.labels()));
            if (!labelsMatch) {
                continue;
            }
            // ESCALATE 且人工已审批 → allow（approver 通道；与既有授权台账一致）
            if (rule.decision().action() == PolicyDecision.Action.ESCALATE
                    && input.humanApproved()) {
                return PolicyDecision.allow("规则 " + rule.id()
                        + " 升级后已获人工审批（escalate→approved）");
            }
            return new PolicyDecision(rule.decision().action(),
                    "规则 " + rule.id() + "：" + rule.decision().reason());
        }
        return PolicyDecision.deny("无策略规则命中工具「" + input.toolName()
                + "」——默认拒（deny by default）");
    }

    /** glob 匹配（* 任意段、? 单字符；空 pattern = 全部）。 */
    static boolean matchesTool(String pattern, String toolName) {
        if (pattern == null || pattern.isBlank() || "*".equals(pattern)) {
            return true;
        }
        return globToRegex(pattern).matcher(toolName).matches();
    }

    private static java.util.regex.Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                default -> {
                    if ("\\[]^$.|()+{}".indexOf(c) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(c);
                }
            }
        }
        return java.util.regex.Pattern.compile(regex.toString());
    }
}
