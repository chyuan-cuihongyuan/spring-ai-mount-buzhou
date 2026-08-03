package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DangerousToolMatcherTest {

    @Test
    void exactNameMatches() {
        DangerousToolMatcher matcher = new DangerousToolMatcher(List.of(
                new DangerousToolEntry("run_command", "confirm_run", "", null)));
        Optional<DangerousToolEntry> matched = matcher.match("run_command");
        assertThat(matched).isPresent();
        assertThat(matched.get().name()).isEqualTo("run_command");
    }

    @Test
    void nonMatchingReturnsEmpty() {
        DangerousToolMatcher matcher = new DangerousToolMatcher(List.of(
                new DangerousToolEntry("run_command", "confirm_run", "", null)));
        assertThat(matcher.match("read_file")).isEmpty();
    }

    @Test
    void globMatchesByPrefix() {
        DangerousToolMatcher matcher = new DangerousToolMatcher(List.of(
                new DangerousToolEntry("mcp:prod_*", "confirm_prod", "", null)));
        assertThat(matcher.match("mcp:prod_deploy")).isPresent();
        assertThat(matcher.match("mcp:dev_x")).isEmpty();
    }

    @Test
    void exactNameBeatsGlob() {
        DangerousToolMatcher matcher = new DangerousToolMatcher(List.of(
                new DangerousToolEntry("mcp:*", "confirm_glob", "", null),
                new DangerousToolEntry("mcp:prod_deploy", "confirm_exact", "", null)));
        Optional<DangerousToolEntry> matched = matcher.match("mcp:prod_deploy");
        assertThat(matched).isPresent();
        assertThat(matched.get().requiredState()).isEqualTo("confirm_exact");
    }

    @Test
    void longestPrefixWinsAmongGlobs() {
        DangerousToolMatcher matcher = new DangerousToolMatcher(List.of(
                new DangerousToolEntry("mcp:*", "confirm_short", "", null),
                new DangerousToolEntry("mcp:prod_*", "confirm_long", "", null)));
        Optional<DangerousToolEntry> matched = matcher.match("mcp:prod_deploy");
        assertThat(matched).isPresent();
        assertThat(matched.get().requiredState()).isEqualTo("confirm_long");
    }

    @Test
    void starOnlyMatchesEverything() {
        DangerousToolMatcher matcher = new DangerousToolMatcher(List.of(
                new DangerousToolEntry("*", "confirm_any", "", null)));
        assertThat(matcher.match("any_tool")).isPresent();
    }
}
