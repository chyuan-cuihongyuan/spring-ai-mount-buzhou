package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class JaroWinklerSimilarityTest {

    @Test
    void shouldMatchKnownVectorPairs() {
        assertThat(JaroWinklerSimilarity.jaroWinkler("MARTHA", "MARHTA", 0))
                .isCloseTo(0.944, within(0.001));
        assertThat(JaroWinklerSimilarity.jaroWinkler("DIXON", "DICKSONX", 0))
                .isCloseTo(0.767, within(0.001));
        assertThat(JaroWinklerSimilarity.jaroWinkler("MARTHA", "MARHTA", 0.1))
                .isCloseTo(0.961, within(0.001));
        assertThat(JaroWinklerSimilarity.jaroWinkler("DWAYNE", "DUANE", 0))
                .isCloseTo(0.822, within(0.001));
        assertThat(JaroWinklerSimilarity.similarity("ABC", "ABC")).isEqualTo(1.0);
        assertThat(JaroWinklerSimilarity.similarity("ABC", "XYZ")).isEqualTo(0.0);
    }

    @Test
    void shouldBeSymmetricAndPrefixBoostMonotonic() {
        assertThat(JaroWinklerSimilarity.jaroWinkler("MARTHA", "MARHTA", 0.1))
                .isCloseTo(JaroWinklerSimilarity.jaroWinkler("MARHTA", "MARTHA", 0.1), within(1e-12));
        double noBoost = JaroWinklerSimilarity.jaroWinkler("MARTHA", "MARTIAN", 0);
        double boosted = JaroWinklerSimilarity.jaroWinkler("MARTHA", "MARTIAN", 0.1);
        assertThat(boosted).isGreaterThan(noBoost);
        assertThat(JaroWinklerSimilarity.jaroWinkler("MARTHA", "MARTIAN", 0.1))
                .isBetween(0.0, 1.0);
    }

    @Test
    void shouldFailFastOnNull() {
        assertThatThrownBy(() -> JaroWinklerSimilarity.jaroWinkler(null, "a", 0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JaroWinklerSimilarity.jaroWinkler("a", null, 0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JaroWinklerSimilarity.similarity(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
