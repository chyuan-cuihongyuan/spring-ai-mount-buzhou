package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-23 / T52 内嵌策略子集：默认拒 / 首条命中 / glob 工具模式 / label 谓词（含 taint 衔接）/
 * escalate+审批=allow。
 */
class EmbeddedPolicyEngineTest {

    private static PolicyDecision.Rule rule(String id, String pattern,
                                            List<PolicyDecision.LabelPredicate> predicates,
                                            PolicyDecision decision) {
        return new PolicyDecision.Rule(id, pattern, predicates, decision);
    }

    @Test
    void denyByDefaultWhenNoRuleMatches() {
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(List.of(
                rule("r1", "read_*", null, PolicyDecision.allow("只读放行"))));
        PolicyDecision decision = engine.decide(new PolicyDecision.Input(
                "agent", "delete_records", java.util.Map.of(), java.util.Map.of(), false));
        assertThat(decision.action()).isEqualTo(PolicyDecision.Action.DENY);
        assertThat(decision.reason()).contains("默认拒");
    }

    @Test
    void globAndFirstMatchWin() {
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(List.of(
                rule("r1", "query_*", null, PolicyDecision.allow("查询放行")),
                rule("r2", "query_secret", null, PolicyDecision.deny("机密查询例外")),
                rule("r3", "write_*", null, PolicyDecision.escalate("写类须人工"))));
        assertThat(engine.decide(new PolicyDecision.Input("agent", "query_order",
                java.util.Map.of(), java.util.Map.of(), false)).action())
                .isEqualTo(PolicyDecision.Action.ALLOW);
        // 首条命中优先：query_secret 命中 r1 即 allow（r2 不再评估——顺序即语义）
        assertThat(engine.decide(new PolicyDecision.Input("agent", "query_secret",
                java.util.Map.of(), java.util.Map.of(), false)).action())
                .isEqualTo(PolicyDecision.Action.ALLOW);
        assertThat(engine.decide(new PolicyDecision.Input("agent", "write_config",
                java.util.Map.of(), java.util.Map.of(), false)).action())
                .isEqualTo(PolicyDecision.Action.ESCALATE);
    }

    @Test
    void labelPredicatesGateWritesInUntrustedContext() {
        // 危险工具门配置的泛化特例：taint=UNTRUSTED 时写类 escalate
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(List.of(
                rule("rw", "read_*", null, PolicyDecision.allow("只读恒放行")),
                rule("ww", "write_*",
                        List.of(new PolicyDecision.LabelPredicate("taint", "eq", "TRUSTED")),
                        PolicyDecision.allow("可信上下文写放行")),
                rule("wu", "write_*",
                        List.of(new PolicyDecision.LabelPredicate("taint", "eq", "UNTRUSTED")),
                        PolicyDecision.escalate("不可信上下文写须人工")),
                rule("catch-all", "*", null, PolicyDecision.deny("其余默认拒"))));
        // trusted 上下文：写放行
        assertThat(engine.decide(new PolicyDecision.Input("agent", "write_config",
                java.util.Map.of(), java.util.Map.of("taint", "TRUSTED"), false)).action())
                .isEqualTo(PolicyDecision.Action.ALLOW);
        // untrusted 上下文：写 escalate；审批后 allow
        PolicyDecision escalated = engine.decide(new PolicyDecision.Input("agent", "write_config",
                java.util.Map.of(), java.util.Map.of("taint", "UNTRUSTED"), false));
        assertThat(escalated.action()).isEqualTo(PolicyDecision.Action.ESCALATE);
        PolicyDecision approved = engine.decide(new PolicyDecision.Input("agent", "write_config",
                java.util.Map.of(), java.util.Map.of("taint", "UNTRUSTED"), true));
        assertThat(approved.action()).isEqualTo(PolicyDecision.Action.ALLOW);
        assertThat(approved.reason()).contains("escalate→approved");
        // 只读不受 taint 影响；未知工具默认拒
        assertThat(engine.decide(new PolicyDecision.Input("agent", "read_page",
                java.util.Map.of(), java.util.Map.of("taint", "UNTRUSTED"), false)).action())
                .isEqualTo(PolicyDecision.Action.ALLOW);
        assertThat(engine.decide(new PolicyDecision.Input("agent", "unknown_tool",
                java.util.Map.of(), java.util.Map.of(), false)).action())
                .isEqualTo(PolicyDecision.Action.DENY);
        // 决策均附 reason（可分析性）
        assertThat(approved.reason()).isNotBlank();
    }
}
