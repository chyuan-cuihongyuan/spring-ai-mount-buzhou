package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5046 / T6194：TinyLFU 准入合同——频次估计单调到
 * 饱和、准入裁决方向、老化减半、fail-fast。
 */
class TinyLfuAdmissionTest {

    private static final int SMALL_EXPECTED = 16;

    private static final int LARGE_EXPECTED = 1024;

    private static final int REPEAT_TO_SATURATION = 30;

    private static final int SATURATION_VALUE = 15;

    private static final int HOT_TIMES = 6;

    private static final int RESET_EVERY = 8;

    @Test
    void estimateShouldRiseMonotonicallyAndSaturate() {
        TinyLfuAdmission<String> sketch = new TinyLfuAdmission<>(LARGE_EXPECTED);
        assertThat(sketch.estimate("k")).isZero();
        sketch.record("k");
        assertThat(sketch.estimate("k")).isEqualTo(1);
        for (int i = 0; i < REPEAT_TO_SATURATION; i++) {
            sketch.record("k");
        }
        assertThat(sketch.estimate("k")).isEqualTo(SATURATION_VALUE);
        assertThat(sketch.estimate("other")).isZero();
    }

    @Test
    void admissionShouldPreferHotCandidateOverColdVictim() {
        TinyLfuAdmission<String> sketch = new TinyLfuAdmission<>(LARGE_EXPECTED);
        for (int i = 0; i < HOT_TIMES; i++) {
            sketch.record("hot");
        }
        sketch.record("cold");
        assertThat(sketch.admit("hot", "cold")).isTrue();
        assertThat(sketch.admit("cold", "hot")).isFalse();
        while (sketch.estimate("cold") < sketch.estimate("hot")) {
            sketch.record("cold");
        }
        assertThat(sketch.admit("cold", "hot")).isTrue();
    }

    @Test
    void agingShouldHalveEstimatesAndCountResets() {
        TinyLfuAdmission<String> sketch = new TinyLfuAdmission<>(SMALL_EXPECTED);
        assertThat(sketch.resetEvery()).isEqualTo(RESET_EVERY);
        for (int i = 0; i < 5; i++) {
            sketch.record("k");
        }
        assertThat(sketch.estimate("k")).isEqualTo(5);
        for (int i = 0; i < 3; i++) {
            sketch.record("k");
        }
        assertThat(sketch.resetCount()).isEqualTo(1);
        assertThat(sketch.estimate("k")).isEqualTo(4);
        assertThat(sketch.sinceReset()).isEqualTo(0);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new TinyLfuAdmission<>(0)).isInstanceOf(IllegalArgumentException.class);
        TinyLfuAdmission<String> sketch = new TinyLfuAdmission<>(SMALL_EXPECTED);
        assertThatThrownBy(() -> sketch.record(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.estimate(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.admit(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.admit("c", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
