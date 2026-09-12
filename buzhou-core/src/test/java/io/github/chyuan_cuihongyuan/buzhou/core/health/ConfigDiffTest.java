package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 生效配置 diff 测试（spec 719 / T989–T990 / impl 522）：三分类、字典序稳定、
 * 掩码同值语义、null 拒绝、不可变。
 */
class ConfigDiffTest {

    @Test
    void classifiesAddedRemovedChanged() {
        List<ConfigDiff.Entry> diff = ConfigDiff.diff(
                Map.of("keep", "1", "gone", "2", "changed", "old"),
                Map.of("keep", "1", "changed", "new", "fresh", "3"));

        assertThat(diff).containsExactlyInAnyOrder(
                new ConfigDiff.Entry("fresh", null, "3", ConfigDiff.Kind.ADDED),
                new ConfigDiff.Entry("gone", "2", null, ConfigDiff.Kind.REMOVED),
                new ConfigDiff.Entry("changed", "old", "new", ConfigDiff.Kind.CHANGED));
        assertThat(diff).noneMatch(e -> e.key().equals("keep")); // 不变键不出现
    }

    @Test
    void outputSortedByKeyStable() {
        List<ConfigDiff.Entry> diff = ConfigDiff.diff(
                Map.of("zeta", "1", "alpha", "1", "mid", "1"),
                Map.of());

        assertThat(diff).extracting(ConfigDiff.Entry::key)
                .containsExactly("alpha", "mid", "zeta");
    }

    @Test
    void maskedEqualValuesCountAsUnchanged() {
        // 快照端点同源：敏感值已掩码——掩码相等 = 未变（掩码底变化不可见的诚实边界）
        List<ConfigDiff.Entry> diff = ConfigDiff.diff(
                Map.of("buzhou.vault.salt", "***"),
                Map.of("buzhou.vault.salt", "***"));

        assertThat(diff).isEmpty();
    }

    @Test
    void identicalMapsYieldEmptyDiff() {
        Map<String, String> same = Map.of("a", "1", "b", "2");
        assertThat(ConfigDiff.diff(same, same)).isEmpty();
    }

    @Test
    void nullMapsRejected() {
        assertThatThrownBy(() -> ConfigDiff.diff(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConfigDiff.diff(Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyVsNonEmptyAllAdded() {
        List<ConfigDiff.Entry> diff = ConfigDiff.diff(Map.of(), Map.of("a", "1", "b", "2"));

        assertThat(diff).hasSize(2).allMatch(e -> e.kind() == ConfigDiff.Kind.ADDED);
    }
}
