package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-679 / spec 926：事实衰减预报——已衰出返回 0、floor=0 永不衰出、
 * 与 decayed 互逆（t=turnsUntilFloor → decayed(conf,t) ≥ floor 且 t-1 时 < floor）、
 * confidence 越界 fail-fast。
 */
class DecayForecastTest {

    private final FactDecayPolicy policy = new FactDecayPolicy(8, 0.25);

    @Test
    void alreadyBelowFloorReturnsZero() {
        assertThat(policy.turnsUntilFloor(0.2)).isZero(); // 0.2 < floor 0.25——已衰出
        assertThat(policy.turnsUntilFloor(0.25)).isZero(); // 恰等于 floor——injectable 仍真但 0 轮后衰出
    }

    @Test
    void zeroFloorNeverExpires() {
        FactDecayPolicy noFloor = new FactDecayPolicy(8, 0);
        assertThat(noFloor.turnsUntilFloor(0.5)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void inverseOfDecayed() {
        // ceil 语义：t = turnsUntilFloor(c) 是「衰出所需轮数」——
        // decayed(c, t) < floor（该轮已衰出），decayed(c, t−1) ≥ floor（前一轮仍在）
        double confidence = 0.8;
        long t = policy.turnsUntilFloor(confidence);
        assertThat(policy.decayed(confidence, (int) t)).isLessThan(0.25);
        assertThat(policy.decayed(confidence, (int) t - 1)).isGreaterThanOrEqualTo(0.25);
    }

    @Test
    void knownValue() {
        // h=8, conf=0.5, floor=0.25：t = 8 × log2(2) = 8
        assertThat(policy.turnsUntilFloor(0.5)).isEqualTo(8);
    }

    @Test
    void confidenceValidated() {
        assertThatThrownBy(() -> policy.turnsUntilFloor(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.turnsUntilFloor(1.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.turnsUntilFloor(-0.1)).isInstanceOf(IllegalArgumentException.class);
    }
}
