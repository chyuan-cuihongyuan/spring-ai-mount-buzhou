package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolPolicyMatchDecisionTest {

    @BeforeEach
    @AfterEach
    void resetReadout() {
        ToolPolicyMatcher.resetStats();
    }

    @Test
    void exactHitRecordsExactOutcome() {
        Map<String, Object> policies = Map.of("write_file", Map.of("hitl", "required"));

        Map<String, Object> resolved = ToolPolicyMatcher.match(policies, "write_file");

        assertThat(resolved).containsEntry("hitl", "required");
        ToolPolicyMatchStats stats = ToolPolicyMatcher.stats();
        assertThat(stats.exactHits()).isEqualTo(1);
        assertThat(stats.globHits()).isZero();
        assertThat(stats.noneHits()).isZero();
        assertThat(stats.recent()).containsExactly(
                new ToolPolicyMatchDecision("write_file", ToolPolicyMatchDecision.Outcome.EXACT, "write_file"));
    }

    @Test
    void globHitRecordsWinningPattern() {
        Map<String, Object> policies = Map.of(
                "mcp_prod_*", Map.of("spill-threshold-chars", 16000),
                "mcp_*", Map.of("spill-threshold-chars", 8000));

        Map<String, Object> resolved = ToolPolicyMatcher.match(policies, "mcp_prod_db");

        assertThat(resolved).containsEntry("spill-threshold-chars", 16000);
        ToolPolicyMatchStats stats = ToolPolicyMatcher.stats();
        assertThat(stats.globHits()).isEqualTo(1);
        assertThat(stats.recent().get(0)).isEqualTo(
                new ToolPolicyMatchDecision("mcp_prod_db", ToolPolicyMatchDecision.Outcome.GLOB, "mcp_prod_*"));
    }

    @Test
    void noMatchRecordsNoneAndReturnsEmpty() {
        Map<String, Object> resolved =
                ToolPolicyMatcher.match(Map.of("write_file", Map.of("hitl", "required")), "read_file");

        assertThat(resolved).isEmpty();
        ToolPolicyMatchStats stats = ToolPolicyMatcher.stats();
        assertThat(stats.noneHits()).isEqualTo(1);
        assertThat(stats.recent().get(0)).isEqualTo(
                new ToolPolicyMatchDecision("read_file", ToolPolicyMatchDecision.Outcome.NONE, ""));
    }

    @Test
    void invalidExactEntryIsNotCountedExact() {
        Map<String, Object> dirty = new HashMap<>();
        dirty.put("write_file", "not-a-map");
        dirty.put("w*", Map.of("hitl", "required"));

        assertThat(ToolPolicyMatcher.match(dirty, "write_file")).containsEntry("hitl", "required");

        ToolPolicyMatchStats stats = ToolPolicyMatcher.stats();
        assertThat(stats.exactHits()).isZero();
        assertThat(stats.globHits()).isEqualTo(1);
        assertThat(stats.recent().get(0)).isEqualTo(
                new ToolPolicyMatchDecision("write_file", ToolPolicyMatchDecision.Outcome.GLOB, "w*"));
    }

    @Test
    void totalConservesAcrossOutcomes() {
        Map<String, Object> policies = Map.of(
                "write_file", Map.of("hitl", "required"),
                "mcp_*", Map.of("hitl", "required"));

        ToolPolicyMatcher.match(policies, "write_file");
        ToolPolicyMatcher.match(policies, "mcp_x");
        ToolPolicyMatcher.match(policies, "other");
        ToolPolicyMatcher.match(policies, "another");

        ToolPolicyMatchStats stats = ToolPolicyMatcher.stats();
        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.exactHits() + stats.globHits() + stats.noneHits()).isEqualTo(stats.total());
        assertThat(stats.exactHits()).isEqualTo(1);
        assertThat(stats.globHits()).isEqualTo(1);
        assertThat(stats.noneHits()).isEqualTo(2);
    }

    @Test
    void recentRingIsBoundedNewestFirst() {
        Map<String, Object> policies = Map.of("t_*", Map.of("hitl", "required"));
        for (int i = 0; i < 40; i++) {
            ToolPolicyMatcher.match(policies, "t_" + i);
        }

        ToolPolicyMatchStats stats = ToolPolicyMatcher.stats();
        assertThat(stats.total()).isEqualTo(40);
        assertThat(stats.recent()).hasSize(ToolPolicyMatcher.RECENT_CAPACITY);
        assertThat(stats.recent().get(0)).isEqualTo(
                new ToolPolicyMatchDecision("t_39", ToolPolicyMatchDecision.Outcome.GLOB, "t_*"));
    }

    @Test
    void resetStatsClearsCountsAndRing() {
        ToolPolicyMatcher.match(Map.of("a", Map.of("hitl", "required")), "a");
        assertThat(ToolPolicyMatcher.stats().total()).isEqualTo(1);

        ToolPolicyMatcher.resetStats();

        ToolPolicyMatchStats stats = ToolPolicyMatcher.stats();
        assertThat(stats.total()).isZero();
        assertThat(stats.recent()).isEmpty();
    }
}
