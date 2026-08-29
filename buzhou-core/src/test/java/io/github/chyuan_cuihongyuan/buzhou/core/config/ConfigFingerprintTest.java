package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 187 / T560：配置指纹回归——稳定 / 顺序无关 / 空白等价 / 三分类 /
 * 脏输入跳过 / 空 map 有锚。
 */
class ConfigFingerprintTest {

    @Test
    void sameMapBuildsStableSummary() {
        ConfigFingerprint one = ConfigFingerprint.of(Map.of(
                "buzhou.bulkhead.enabled", "true", "buzhou.leak.level", "SIMPLE"));
        ConfigFingerprint two = ConfigFingerprint.of(Map.of(
                "buzhou.leak.level", "SIMPLE", "buzhou.bulkhead.enabled", "true"));

        assertThat(one.summaryHex()).isEqualTo(two.summaryHex());
        assertThat(one.diff(two).isEmpty()).isTrue();
    }

    @Test
    void whitespaceDifferencesAreEquivalent() {
        ConfigFingerprint tight = ConfigFingerprint.of(Map.of("k", "v"));
        ConfigFingerprint spaced = ConfigFingerprint.of(Map.of(" k ", " v "));

        assertThat(tight.summaryHex()).isEqualTo(spaced.summaryHex()); // strip 归一
    }

    @Test
    void diffClassifiesThreeWays() {
        ConfigFingerprint before = ConfigFingerprint.of(Map.of(
                "keep", "1", "gone", "2", "changed", "a"));
        ConfigFingerprint after = ConfigFingerprint.of(Map.of(
                "keep", "1", "changed", "b", "fresh", "3"));

        ConfigFingerprint.Diff diff = before.diff(after);
        assertThat(diff.added()).containsExactly("fresh");
        assertThat(diff.removed()).containsExactly("gone");
        assertThat(diff.changed()).containsExactly("changed");
    }

    @Test
    void dirtyEntriesAreSkippedNotFatal() {
        Map<String, String> dirty = new HashMap<>();
        dirty.put("ok", "v");
        dirty.put(null, "x");
        dirty.put("  ", "y");
        dirty.put("bad", null);

        ConfigFingerprint fingerprint = ConfigFingerprint.of(dirty);
        assertThat(fingerprint.size()).isEqualTo(1);
        assertThat(fingerprint.valueOf("ok")).isEqualTo("v");
    }

    @Test
    void emptyMapHasAnchorAndNullOtherHandled() {
        ConfigFingerprint empty = ConfigFingerprint.of(null);
        assertThat(empty.summaryHex()).isNotBlank();
        assertThat(empty.diff(null).isEmpty()).isTrue();

        ConfigFingerprint full = ConfigFingerprint.of(Map.of("k", "v"));
        assertThat(empty.diff(full).added()).containsExactly("k");
        assertThat(full.diff(null).removed()).containsExactly("k");
    }

    @Test
    void valueOfReturnsNullForMissingKey() {
        ConfigFingerprint fingerprint = ConfigFingerprint.of(Map.of("a", "1"));
        assertThat(fingerprint.valueOf("a")).isEqualTo("1");
        assertThat(fingerprint.valueOf("ghost")).isNull();
    }
}
