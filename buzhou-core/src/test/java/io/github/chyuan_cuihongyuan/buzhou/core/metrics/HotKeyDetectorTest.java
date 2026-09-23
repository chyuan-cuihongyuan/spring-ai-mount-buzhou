package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5019 / T6140：热点探测合同——采样确定性、阈值告警、
 * 字典序热点集、畸形 fail-fast、同序列回放。
 */
class HotKeyDetectorTest {

    @Test
    void samplingShouldHitExactlyEveryNthRecord() {
        HotKeyDetector detector = new HotKeyDetector(10, 3);
        for (int i = 1; i <= 30; i++) {
            detector.record("key-a");   // 30 次 → 恰 3 次采样命中
        }
        assertThat(detector.globalSequence()).isEqualTo(30L);
        assertThat(detector.sampledCountOf("key-a")).isEqualTo(3L);
    }

    @Test
    void hotThresholdShouldSeparateHotAndCold() {
        HotKeyDetector detector = new HotKeyDetector(10, 3);
        for (int i = 0; i < 35; i++) {
            detector.record("hot");
        }
        for (int i = 0; i < 24; i++) {
            detector.record("cold");
        }
        assertThat(detector.isHot("hot")).isTrue();     // 3 次采样 ≥ 3
        assertThat(detector.isHot("cold")).isFalse();   // 2 次采样 < 3
        assertThat(detector.hotKeys()).containsExactly("hot");
    }

    @Test
    void hotKeysShouldBeDeterministicAndSorted() {
        HotKeyDetector detector = new HotKeyDetector(1, 2);
        detector.record("zeta");
        detector.record("alpha");
        detector.record("zeta");
        detector.record("alpha");
        assertThat(detector.hotKeys()).containsExactly("alpha", "zeta");   // 字典序
        List<String> replay = detector.hotKeys();
        assertThat(replay).isEqualTo(detector.hotKeys());
    }

    @Test
    void invalidInputsShouldFailFast() {
        assertThatThrownBy(() -> new HotKeyDetector(0, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HotKeyDetector(10, 0)).isInstanceOf(IllegalArgumentException.class);
        HotKeyDetector detector = new HotKeyDetector(1, 1);
        assertThatThrownBy(() -> detector.record(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> detector.isHot("")).isInstanceOf(IllegalArgumentException.class);
    }
}
