package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolTimeoutOverrideStatsTest {

    @Test
    void exactKeyHitCountedPerPattern() {
        ToolTimeoutOverrides overrides = new ToolTimeoutOverrides(Map.of("web_search", 5_000L));

        assertThat(overrides.timeoutMillisFor("web_search")).isEqualTo(5_000L);

        ToolTimeoutOverrideStats stats = overrides.stats();
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isZero();
        assertThat(stats.hitsByPattern().get("web_search")).isEqualTo(1);
    }

    @Test
    void unmatchedLookupCountedAsMiss() {
        ToolTimeoutOverrides overrides = new ToolTimeoutOverrides(Map.of("web_search", 5_000L));

        assertThat(overrides.timeoutMillisFor("other_tool")).isEqualTo(-1);

        ToolTimeoutOverrideStats stats = overrides.stats();
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.hits()).isZero();
        // 从未命中的模式直接显形为 0（幽灵覆盖）
        assertThat(stats.hitsByPattern().get("web_search")).isZero();
    }

    @Test
    void globPatternHitCounted() {
        ToolTimeoutOverrides overrides = new ToolTimeoutOverrides(Map.of("mcp_*", 100L));

        assertThat(overrides.timeoutMillisFor("mcp_fs")).isEqualTo(100L);
        assertThat(overrides.timeoutMillisFor("unrelated")).isEqualTo(-1);

        ToolTimeoutOverrideStats stats = overrides.stats();
        assertThat(stats.hitsByPattern().get("mcp_*")).isEqualTo(1);
        assertThat(stats.hits() + stats.misses()).isEqualTo(stats.lookups());
    }

    @Test
    void exactlyOnePatternCountedPerLookup() {
        // 既有实现 Map.copyOf 不保调用方顺序——「首中即胜」的『首』非确定（存量口径）；
        // 本轮只断言每次 lookup 恰有一个模式被计数，不依赖具体是谁
        Map<String, Long> both = new java.util.LinkedHashMap<>();
        both.put("mcp_x", 200L);
        both.put("mcp_*", 100L);
        ToolTimeoutOverrides overrides = new ToolTimeoutOverrides(both);

        overrides.timeoutMillisFor("mcp_x");

        ToolTimeoutOverrideStats stats = overrides.stats();
        assertThat(stats.hits()).isEqualTo(1);
        long counted = stats.hitsByPattern().values().stream()
                .mapToLong(Long::longValue).sum();
        assertThat(counted).isEqualTo(1);
    }

    @Test
    void conservationHoldsAcrossMixedLookups() {
        ToolTimeoutOverrides overrides = new ToolTimeoutOverrides(Map.of(
                "known", 1_000L,
                "glob_*", 2_000L));

        overrides.timeoutMillisFor("known");
        overrides.timeoutMillisFor("glob_a");
        overrides.timeoutMillisFor("unknown");

        ToolTimeoutOverrideStats stats = overrides.stats();
        assertThat(stats.lookups()).isEqualTo(3);
        assertThat(stats.hits() + stats.misses()).isEqualTo(stats.lookups());
    }

    @Test
    void disabledInstanceCountsMissesWithoutPatterns() {
        ToolTimeoutOverrides overrides = ToolTimeoutOverrides.disabled();

        assertThat(overrides.timeoutMillisFor("anything")).isEqualTo(-1);

        ToolTimeoutOverrideStats stats = overrides.stats();
        assertThat(stats.lookups()).isEqualTo(1);
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.hitsByPattern()).isEmpty();
    }
}
