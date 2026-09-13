package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 817 / T1136：多窗燃烧率判定回归——双窗共振/快窗毛刺/慢窗渗漏/
 * 样本不足不判/边界相等判定/参数 fail-fast。
 */
class SloMultiWindowBurnTest {

    private static final double FAST_T = 14.4; // SRE Workbook 经典 5m/14.4
    private static final double SLOW_T = 6.0;  // 1h/6.0
    private static final long MIN = 20;

    @Test
    void dualWindowResonanceIncidents() {
        var verdict = SloMultiWindowBurn.evaluate(15.0, 6.5, 100, 500, FAST_T, SLOW_T, MIN);
        assertThat(verdict.incident()).isTrue();
        assertThat(verdict.reason()).contains("双窗共振");

        // 边界相等（>= 语义）也判
        var boundary = SloMultiWindowBurn.evaluate(14.4, 6.0, 100, 500, FAST_T, SLOW_T, MIN);
        assertThat(boundary.incident()).isTrue();
    }

    @Test
    void fastOnlySpikeIsBlip() {
        var verdict = SloMultiWindowBurn.evaluate(20.0, 2.0, 100, 500, FAST_T, SLOW_T, MIN);
        assertThat(verdict.incident()).isFalse();
        assertThat(verdict.reason()).contains("毛刺");
    }

    @Test
    void slowOnlyIsChronicLeak() {
        var verdict = SloMultiWindowBurn.evaluate(1.0, 8.0, 100, 500, FAST_T, SLOW_T, MIN);
        assertThat(verdict.incident()).isFalse();
        assertThat(verdict.reason()).contains("慢性渗漏");
    }

    @Test
    void insufficientSamplesNeverIncidents() {
        var verdict = SloMultiWindowBurn.evaluate(50.0, 50.0, 19, 500, FAST_T, SLOW_T, MIN);
        assertThat(verdict.incident()).isFalse();
        assertThat(verdict.reason()).contains("样本不足");

        var slowShort = SloMultiWindowBurn.evaluate(50.0, 50.0, 100, 19, FAST_T, SLOW_T, MIN);
        assertThat(slowShort.incident()).isFalse();
    }

    @Test
    void bothCoolIsQuiet() {
        var verdict = SloMultiWindowBurn.evaluate(0.5, 0.2, 100, 500, FAST_T, SLOW_T, MIN);
        assertThat(verdict.incident()).isFalse();
        assertThat(verdict.reason()).contains("均未超阈");
    }

    @Test
    void failFastOnBadThresholdsAndMinSamples() {
        assertThatThrownBy(() -> SloMultiWindowBurn.evaluate(1, 1, 10, 10, 0, 6.0, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SloMultiWindowBurn.evaluate(1, 1, 10, 10, 14.4, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SloMultiWindowBurn.evaluate(1, 1, 10, 10, 14.4, 6.0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
