package io.github.chyuan_cuihongyuan.buzhou.core.export;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 808 / T1118：导出去重统计回归——重复计数与节省字节/占比/Top 降序/
 * 空块口径/预览截断/fail-fast。
 */
class ExportDedupeStatsTest {

    @Test
    void countsDuplicatesAndSavings() {
        ExportDedupeStats.Report report = ExportDedupeStats.analyze(List.of(
                "hello", "world", "hello", "hello", "ok"));

        assertThat(report.totalItems()).isEqualTo(5);
        assertThat(report.uniqueItems()).isEqualTo(3);
        assertThat(report.totalChars()).isEqualTo(5 + 5 + 5 + 5 + 2);
        assertThat(report.uniqueChars()).isEqualTo(5 + 5 + 2);
        // 重复浪费：hello ×3 → 2×5=10；world/ok 各 1 次
        assertThat(report.duplicateChars()).isEqualTo(10);
        assertThat(report.savingsRatio()).isCloseTo(10.0 / 22, within(1e-9));
        assertThat(report.top()).hasSize(1);
        assertThat(report.top().get(0).preview()).isEqualTo("hello");
        assertThat(report.top().get(0).occurrences()).isEqualTo(3);
        assertThat(report.top().get(0).charsWasted()).isEqualTo(10);
    }

    @Test
    void allUniqueYieldsZeroSavings() {
        ExportDedupeStats.Report report = ExportDedupeStats.analyze(List.of("a", "b", "c"));
        assertThat(report.duplicateChars()).isZero();
        assertThat(report.savingsRatio()).isZero();
        assertThat(report.top()).isEmpty();
        assertThat(report.uniqueItems()).isEqualTo(3);
    }

    @Test
    void blankBlocksCountAsItemsButNotDuplicates() {
        ExportDedupeStats.Report report = ExportDedupeStats.analyze(
                java.util.Arrays.asList("x", "", "  ", null, "x"));
        assertThat(report.totalItems()).isEqualTo(5);
        assertThat(report.uniqueItems()).isEqualTo(1); // 只有 "x"
        assertThat(report.totalChars()).isEqualTo(2); // 两个 "x"
        assertThat(report.duplicateChars()).isEqualTo(1);
    }

    @Test
    void topSortedByWastedCharsAndCapped() {
        java.util.List<String> blocks = new java.util.ArrayList<>();
        // long-block 出现 5 次（浪费 4×20=80），mid 3 次（2×6=12），short 4 次（3×1=3）
        for (int i = 0; i < 5; i++) {
            blocks.add("0123456789abcdefghij");
        }
        for (int i = 0; i < 3; i++) {
            blocks.add("midblk");
        }
        for (int i = 0; i < 4; i++) {
            blocks.add("s");
        }
        ExportDedupeStats.Report report = ExportDedupeStats.analyze(blocks);
        assertThat(report.top().get(0).preview()).isEqualTo("0123456789abcdefghij");
        assertThat(report.top().get(0).charsWasted()).isEqualTo(80);
        assertThat(report.top().get(1).charsWasted()).isEqualTo(12);
        assertThat(report.top().get(2).charsWasted()).isEqualTo(3);
    }

    @Test
    void previewTruncatesLongBlocks() {
        String longBlock = "x".repeat(100);
        ExportDedupeStats.Report report = ExportDedupeStats.analyze(List.of(longBlock, longBlock));
        assertThat(report.top().get(0).preview()).hasSize(33); // 32 + "…"
        assertThat(report.top().get(0).preview().endsWith("…")).isTrue();
    }

    @Test
    void emptyListAndFailFast() {
        ExportDedupeStats.Report report = ExportDedupeStats.analyze(List.of());
        assertThat(report.totalItems()).isZero();
        assertThat(report.savingsRatio()).isZero();
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> ExportDedupeStats.analyze(null));
    }
}
