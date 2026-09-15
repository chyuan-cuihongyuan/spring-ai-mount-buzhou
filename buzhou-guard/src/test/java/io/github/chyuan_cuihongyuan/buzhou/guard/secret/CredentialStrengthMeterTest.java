package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1863 / T2928：强度计——类别计数、双条件阶梯、边界含上。 */
class CredentialStrengthMeterTest {

    /** 类别计数：四类齐全=4；单类=1。 */
    @Test
    void shouldCountCharacterClasses() {
        assertThat(CredentialStrengthMeter.charClasses("Ab3!x")).isEqualTo(4);
        assertThat(CredentialStrengthMeter.charClasses("abcdef")).isEqualTo(1);
        assertThat(CredentialStrengthMeter.charClasses("ABC123")).isEqualTo(2);
        assertThat(CredentialStrengthMeter.charClasses("--------")).isEqualTo(1);
    }

    /** 双条件阶梯：短而杂=WEAK、12 位三类=FAIR、16 位三类=STRONG。 */
    @Test
    void shouldBandWithDualConditions() {
        assertThat(CredentialStrengthMeter.band("Ab3!"))
                .isEqualTo(CredentialStrengthMeter.Band.WEAK);
        assertThat(CredentialStrengthMeter.band("onlylowercase12"))
                .isEqualTo(CredentialStrengthMeter.Band.WEAK);
        assertThat(CredentialStrengthMeter.band("Abcdefghijk12"))
                .isEqualTo(CredentialStrengthMeter.Band.FAIR);
        assertThat(CredentialStrengthMeter.band("Abcdefghijklmnop12"))
                .isEqualTo(CredentialStrengthMeter.Band.STRONG);
    }

    /** 边界含上：12/16 位恰入 FAIR/STRONG。 */
    @Test
    void lengthBoundariesAreInclusive() {
        String fairEdge = "Ab1!defghij2";
        assertThat(fairEdge).hasSize(12);
        assertThat(CredentialStrengthMeter.band(fairEdge))
                .isEqualTo(CredentialStrengthMeter.Band.FAIR);
        String strongEdge = "Ab1!defghijklmn6";
        assertThat(strongEdge).hasSize(16);
        assertThat(CredentialStrengthMeter.band(strongEdge))
                .isEqualTo(CredentialStrengthMeter.Band.STRONG);
    }

    /** 畸形入参 fail-fast：空白凭据。 */
    @Test
    void malformedCredentialFailsFast() {
        assertThatThrownBy(() -> CredentialStrengthMeter.charClasses(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("凭据不能为空白");
        assertThatThrownBy(() -> CredentialStrengthMeter.band(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
