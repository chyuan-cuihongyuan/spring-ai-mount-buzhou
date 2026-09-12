package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 710 / T1020–T1021：全局 holdout 层——全排除/零变化/层语义（跨实验
 * 一致排除）/构造校验。
 */
class ExperimentHoldoutTest {

    @Test
    void fullHoldoutExcludesEverythingAndCountsSeparately() {
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("exp-a", Map.of("t", 100), "exp-b", Map.of("c", 100)),
                Map.of(), 100, Clock.systemUTC());
        assertThat(bucketer.assign("exp-a", "u1")).isNull();
        assertThat(bucketer.assign("exp-b", "u1")).isNull();
        assertThat(bucketer.snapshot().get("exp-a").get("__holdout__")).isEqualTo(1);
        assertThat(bucketer.snapshot().get("exp-b").get("__holdout__")).isEqualTo(1);
        assertThat(bucketer.holdoutPercent()).isEqualTo(100);
    }

    @Test
    void zeroHoldoutKeepsLegacyBehavior() {
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("exp-a", Map.of("t", 100)),
                Map.of(), 0, Clock.systemUTC());
        assertThat(bucketer.assign("exp-a", "u1")).isEqualTo("t");
        assertThat(bucketer.snapshot().get("exp-a").get("__holdout__")).isNull();
    }

    @Test
    void holdoutIsConsistentAcrossExperimentsForSameUnit() {
        // holdout=50：同 unit 跨实验要么全排除要么全不排除（层语义——哈希不含实验名）
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("e1", Map.of("v", 100), "e2", Map.of("v", 100),
                        "e3", Map.of("v", 100), "e4", Map.of("v", 100)),
                Map.of(), 50, Clock.systemUTC());
        boolean excludedInE1 = bucketer.assign("e1", "unit-x") == null;
        for (String experiment : new String[]{"e2", "e3", "e4"}) {
            boolean excluded = bucketer.assign(experiment, "unit-x") == null;
            assertThat(excluded).as("实验 " + experiment + " 的排除状态应与 e1 一致").isEqualTo(excludedInE1);
        }
    }

    @Test
    void invalidHoldoutPercentFailsFast() {
        assertThatThrownBy(() -> new ExperimentBucketer(Map.of(), Map.of(), 101, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExperimentBucketer(Map.of(), Map.of(), -1, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
