package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 847 / T1196：数据集近重复回归——精确重复成簇/近重复阈值/唯一率/
 * 截断诚实口径/脏条目/fail-fast。
 */
class DatasetNearDuplicateStatsTest {

    @Test
    void exactDuplicatesCluster() {
        var report = DatasetNearDuplicateStats.analyze(
                List.of("same query", "same query", "same query", "different"), 0.9);

        assertThat(report.itemsConsidered()).isEqualTo(4);
        assertThat(report.largestCluster()).isEqualTo(3);
        assertThat(report.duplicatePairs()).isEqualTo(3); // 3 选 2
        assertThat(report.uniqueRatio()).isCloseTo(0.5, within(1e-9));
        assertThat(report.truncated()).isFalse();
    }

    @Test
    void nearDuplicatesByTrigramThreshold() {
        List<String> items = List.of(
                "帮我查询订单 12345 的物流状态",
                "帮我查询订单 12345 的物流状态!",
                " completely unrelated content here");
        var report = DatasetNearDuplicateStats.analyze(items, 0.8);

        assertThat(report.duplicatePairs()).isEqualTo(1);
        assertThat(report.largestCluster()).isEqualTo(2);
        assertThat(report.uniqueRatio()).isCloseTo(2.0 / 3, within(1e-9));
    }

    @Test
    void allUniqueHighRatio() {
        var report = DatasetNearDuplicateStats.analyze(
                List.of("alpha beta gamma", "delta epsilon zeta", "eta theta iota"), 0.8);
        assertThat(report.duplicatePairs()).isZero();
        assertThat(report.uniqueRatio()).isCloseTo(1.0, within(1e-9));
        assertThat(report.largestCluster()).isEqualTo(1);
    }

    @Test
    void itemCapTruncatesHonestly() {
        List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < DatasetNearDuplicateStats.MAX_ITEMS + 30; i++) {
            items.add("item " + i);
        }
        var report = DatasetNearDuplicateStats.analyze(items, 0.5);
        assertThat(report.itemsTotal()).isEqualTo(DatasetNearDuplicateStats.MAX_ITEMS + 30);
        assertThat(report.itemsConsidered()).isEqualTo(DatasetNearDuplicateStats.MAX_ITEMS);
        assertThat(report.truncated()).isTrue();
    }

    @Test
    void dirtyEntriesSkippedButCounted() {
        var report = DatasetNearDuplicateStats.analyze(
                Arrays.asList(null, "", "  ", "dup", "dup"), 0.9);
        assertThat(report.itemsTotal()).isEqualTo(5);
        assertThat(report.itemsConsidered()).isEqualTo(2);
        assertThat(report.duplicatePairs()).isEqualTo(1);
    }

    @Test
    void failFastOnBadThreshold() {
        assertThatThrownBy(() -> DatasetNearDuplicateStats.analyze(List.of("x"), 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatasetNearDuplicateStats.analyze(List.of("x"), 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
