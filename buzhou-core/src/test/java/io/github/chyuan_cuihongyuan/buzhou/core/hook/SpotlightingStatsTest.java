package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpotlightingStatsTest {

    @BeforeEach
    @AfterEach
    void resetReadout() {
        Spotlighting.resetForTest();
    }

    @Test
    void roundTripCountsWrappedAndUnwrapped() {
        String wrappedText = Spotlighting.wrap("tool-x", Spotlighting.DEFAULT_MARK_CHAR, 8, "外部内容");

        String restored = Spotlighting.unwrap(wrappedText);

        assertThat(restored).isEqualTo("外部内容");
        assertThat(Spotlighting.stats()).isEqualTo(new SpotlightingStats(1, 1, 0));
    }

    @Test
    void plainTextCountsNothing() {
        assertThat(Spotlighting.unwrap("plain")).isEqualTo("plain");
        assertThat(Spotlighting.stats()).isEqualTo(new SpotlightingStats(0, 0, 0));
    }

    @Test
    void malformedWrappedCountedAsAttemptAndFailure() {
        String headOnly = Spotlighting.BEGIN_HEAD + "tool-x"; // 含头无 -BEGIN>>> 无 END

        assertThat(Spotlighting.unwrap(headOnly)).isSameAs(headOnly);

        assertThat(Spotlighting.stats()).isEqualTo(new SpotlightingStats(1, 0, 1));
    }

    @Test
    void conservationHoldsAcrossMixedOutcomes() {
        String good = Spotlighting.wrap("t", Spotlighting.DEFAULT_MARK_CHAR, 4, "内容");
        Spotlighting.unwrap(good);
        Spotlighting.unwrap(Spotlighting.BEGIN_HEAD + "broken");
        Spotlighting.unwrap(good);

        SpotlightingStats stats = Spotlighting.stats();
        assertThat(stats.wrapped()).isEqualTo(3);
        assertThat(stats.unwrapped() + stats.malformed()).isEqualTo(stats.wrapped());
        assertThat(stats.unwrapped()).isEqualTo(2);
        assertThat(stats.malformed()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesAllCounters() {
        Spotlighting.wrap("t", Spotlighting.DEFAULT_MARK_CHAR, 4, "x");
        Spotlighting.resetForTest();

        assertThat(Spotlighting.stats()).isEqualTo(new SpotlightingStats(0, 0, 0));
    }
}
