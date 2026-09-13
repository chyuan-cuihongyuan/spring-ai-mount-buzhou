package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPolicyEngineStatsTest {

    private static PolicyDecision.Rule rule(String id, String pattern, String action,
                                            PolicyDecision.LabelPredicate... predicates) {
        PolicyDecision decision = switch (action) {
            case "allow" -> PolicyDecision.allow("ok");
            case "deny" -> PolicyDecision.deny("no");
            case "escalate" -> PolicyDecision.escalate("需人工");
            default -> PolicyDecision.deny("no");
        };
        List<PolicyDecision.LabelPredicate> preds = List.of(predicates);
        return new PolicyDecision.Rule(id, pattern, preds, decision);
    }

    private static PolicyDecision.Input input(String tool, boolean approved) {
        return new PolicyDecision.Input("principal", tool, Map.of(), Map.of(), approved);
    }

    @Test
    void allowRuleCounted() {
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(
                List.of(rule("r1", "read_*", "allow")));

        assertThat(engine.decide(input("read_file", false)).action())
                .isEqualTo(PolicyDecision.Action.ALLOW);
        assertThat(engine.stats().allowCount()).isEqualTo(1);
    }

    @Test
    void defaultDenyCountedWhenNoRuleMatches() {
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(List.of());

        assertThat(engine.decide(input("anything", false)).action())
                .isEqualTo(PolicyDecision.Action.DENY);
        assertThat(engine.stats().denyCount()).isEqualTo(1);
    }

    @Test
    void escalateCountedSeparatelyFromApproved() {
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(
                List.of(rule("r1", "write_*", "escalate")));

        engine.decide(input("write_file", false));
        assertThat(engine.stats().escalateCount()).isEqualTo(1);

        engine.decide(input("write_file", true));
        assertThat(engine.stats().escalateApprovedCount()).isEqualTo(1);
    }

    @Test
    void labelPredicateMismatchFallsThroughToDefaultDeny() {
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(
                List.of(rule("r1", "*", "allow",
                        new PolicyDecision.LabelPredicate("env", "eq", "prod"))));

        PolicyDecision decision = engine.decide(input("any_tool", false));

        assertThat(decision.action()).isEqualTo(PolicyDecision.Action.DENY);
        assertThat(engine.stats().denyCount()).isEqualTo(1);
    }

    @Test
    void fourBucketsConservesAcrossDecisions() {
        EmbeddedPolicyEngine engine = new EmbeddedPolicyEngine(
                List.of(rule("allow_*", "*", "allow"), rule("esc_*", "*", "escalate")));

        engine.decide(input("allow_x", false));
        engine.decide(input("esc_x", false));
        engine.decide(input("esc_x", true));
        engine.decide(input("unknown", false));

        EmbeddedPolicyEngine.PolicyDecisionStats stats = engine.stats();
        long total = stats.allowCount() + stats.denyCount() + stats.escalateCount()
                + stats.escalateApprovedCount();
        assertThat(total).isEqualTo(4);
    }
}
