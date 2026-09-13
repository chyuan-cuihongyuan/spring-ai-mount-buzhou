package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 733 / T1064–T1065：提示词使用缺口——零使用差集/孤儿统计/null fail-fast。
 */
class PromptUsageGapsTest {

    private static PromptUsageStats.Row row(String name, int version, long count) {
        try {
            var ctor = PromptUsageStats.Row.class.getDeclaredConstructor(String.class, int.class, long.class);
            ctor.setAccessible(true);
            return ctor.newInstance(name, version, count);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void zeroUsageAndOrphansAreReported() {
        PromptUsageGaps.Report report = PromptUsageGaps.analyze(
                Set.of("greeting", "farewell", "summary"),
                List.of(row("greeting", 1, 5), row("ghost", 1, 2)));
        assertThat(report.unused()).containsExactly("farewell", "summary");
        assertThat(report.orphans()).containsExactly("ghost");
    }

    @Test
    void nullArgsFailFast() {
        assertThatThrownBy(() -> PromptUsageGaps.analyze(null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PromptUsageGaps.analyze(Set.of(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
