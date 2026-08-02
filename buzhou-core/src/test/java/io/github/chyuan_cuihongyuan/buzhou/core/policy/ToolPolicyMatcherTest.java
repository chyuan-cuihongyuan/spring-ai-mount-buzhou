package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolPolicyMatcherTest {

    private final Map<String, Object> toolPolicies = Map.of(
            "write_file", Map.of("hitl", "required"),
            "mcp_prod_*", Map.of("hitl", "required", "spill-threshold-chars", 16000),
            "mcp_*", Map.of("spill-threshold-chars", 8000),
            "*", Map.of("micro-compaction", Map.of("max-age-turns", 3)));

    @Test
    void exactNameWins() {
        assertThat(ToolPolicyMatcher.match(toolPolicies, "write_file"))
                .containsEntry("hitl", "required")
                .doesNotContainKey("spill-threshold-chars");
    }

    @Test
    void longestWildcardPrefixWins() {
        assertThat(ToolPolicyMatcher.match(toolPolicies, "mcp_prod_db"))
                .containsEntry("spill-threshold-chars", 16000);
        assertThat(ToolPolicyMatcher.match(toolPolicies, "mcp_dev_fs"))
                .containsEntry("spill-threshold-chars", 8000);
    }

    @Test
    void starIsFallback() {
        assertThat(ToolPolicyMatcher.match(toolPolicies, "read_file"))
                .containsKey("micro-compaction");
    }

    @Test
    void noMatchReturnsEmpty() {
        assertThat(ToolPolicyMatcher.match(Map.of("write_file", Map.of("hitl", "required")), "read_file"))
                .isEmpty();
    }

    @Test
    void invalidEntriesAreSkippedSilently() {
        Map<String, Object> dirty = new java.util.HashMap<>();
        dirty.put("write_file", "not-a-map");
        dirty.put("*", Map.of("hitl", "required"));
        assertThat(ToolPolicyMatcher.match(dirty, "write_file")).containsEntry("hitl", "required");
        assertThat(ToolPolicyMatcher.match(dirty, "anything")).containsEntry("hitl", "required");
    }
}
