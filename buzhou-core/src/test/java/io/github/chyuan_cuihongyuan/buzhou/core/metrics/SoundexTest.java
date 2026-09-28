package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7018：Soundex 合同——NARA 规范语音编码。官方钉子
 * 向量（含 H/W 规则 Ashcraft、PF 折叠 Pfister）；同音归并；
 * fail-fast。
 */
class SoundexTest {

    @Test
    void naraCanonicalVectors() {
        assertThat(Soundex.soundex("Robert")).isEqualTo("R163");
        assertThat(Soundex.soundex("Rupert")).isEqualTo("R163");
        assertThat(Soundex.soundex("Ashcraft")).isEqualTo("A261");
        assertThat(Soundex.soundex("Ashcroft")).isEqualTo("A261");
        assertThat(Soundex.soundex("Tymczak")).isEqualTo("T522");
        assertThat(Soundex.soundex("Pfister")).isEqualTo("P236");
        assertThat(Soundex.soundex("Jackson")).isEqualTo("J250");
        assertThat(Soundex.soundex("Judy")).isEqualTo("J300");
    }

    @Test
    void phoneticMergingAndNormalization() {
        assertThat(Soundex.soundex("robert")).isEqualTo("R163");
        assertThat(Soundex.soundex("ROBERT")).isEqualTo("R163");
        assertThat(Soundex.soundex("O'Brien")).isEqualTo("O165");
        assertThat(Soundex.soundex("Washington")).isEqualTo("W252");
    }

    @Test
    void paddingAndTruncation() {
        assertThat(Soundex.soundex("Judy")).isEqualTo("J300");
        assertThat(Soundex.soundex("Lee")).isEqualTo("L000");
        assertThat(Soundex.soundex("Washingtonxyzq")).isEqualTo("W252");
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> Soundex.soundex(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Soundex.soundex("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Soundex.soundex("123!!")).isInstanceOf(IllegalArgumentException.class);
    }
}
