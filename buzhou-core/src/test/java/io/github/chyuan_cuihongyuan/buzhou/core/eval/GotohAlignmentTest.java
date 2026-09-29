package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GotohAlignmentTest {

    @Test
    void shouldDegradeToLinearAndMatchNeedlemanWunsch() {
        String first = "GATTACA";
        String second = "GCATGCU";
        assertThat(GotohAlignment.score(first, second, 1, -1, 0, -1)).isEqualTo(0);
        assertThat(GotohAlignment.score("ACGT", "ACGT", 1, -1, 0, -1)).isEqualTo(4);
        assertThat(GotohAlignment.score("AAAA", "AA", 1, -1, 0, -1)).isEqualTo(1);
    }

    @Test
    void shouldPenalizeLongGapOnceForAffineSemantics() {
        int gentler = GotohAlignment.score("ACGT", "AT", 1, -1, -2, -1);
        int harsher = GotohAlignment.score("ACGT", "AT", 1, -1, -3, -1);
        assertThat(gentler).isEqualTo(-2);
        assertThat(harsher).isEqualTo(-3);
        assertThat(GotohAlignment.score("ACGT", "AT", 1, -1, -3, -1))
                .isEqualTo(GotohAlignment.score("ACGT", "AT", 1, -1, -4, -1) + 1);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        assertThat(GotohAlignment.score("ACG", "AG", 1, -1, -2, -1))
                .isEqualTo(GotohAlignment.score("ACG", "AG", 1, -1, -2, -1));
        assertThatThrownBy(() -> GotohAlignment.score(null, "a", 1, -1, -2, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GotohAlignment.score("a", null, 1, -1, -2, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GotohAlignment.score("a", "a", 1, -1, 1, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GotohAlignment.score("a", "a", 1, -1, -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
