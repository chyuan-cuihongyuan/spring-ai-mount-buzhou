package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 529 / T811：per-tool 超时覆盖——glob 命中替换全局、未命中/无覆盖
 * 全局生效（-1 语义）、负值 fail-fast、Holder 默认 disabled 零行为。
 */
class ToolTimeoutOverridesTest {

    @AfterEach
    void resetHolder() {
        ToolTimeoutOverrides.Holder.set(ToolTimeoutOverrides.disabled());
    }

    @Test
    void disabledByDefault() {
        assertThat(ToolTimeoutOverrides.Holder.current().timeoutMillisFor("any")).isEqualTo(-1);
    }

    @Test
    void globOverrideWinsForMatchingTool() {
        ToolTimeoutOverrides overrides = new ToolTimeoutOverrides(Map.of(
                "slow_*", 300_000L, "fast_search", 1_500L));
        assertThat(overrides.timeoutMillisFor("slow_scraper")).isEqualTo(300_000);
        assertThat(overrides.timeoutMillisFor("fast_search")).isEqualTo(1_500);
        assertThat(overrides.timeoutMillisFor("other_tool")).isEqualTo(-1); // 未命中 = 全局
    }

    @Test
    void negativeOverrideValueFailsFast() {
        assertThatThrownBy(() -> new ToolTimeoutOverrides(Map.of("bad", -2L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void holderRoundTrip() {
        ToolTimeoutOverrides.Holder.set(new ToolTimeoutOverrides(Map.of("web_*", 60_000L)));
        assertThat(ToolTimeoutOverrides.Holder.current().timeoutMillisFor("web_fetch"))
                .isEqualTo(60_000);
        ToolTimeoutOverrides.Holder.set(null); // null 归 disabled
        assertThat(ToolTimeoutOverrides.Holder.current().timeoutMillisFor("web_fetch"))
                .isEqualTo(-1);
    }
}
