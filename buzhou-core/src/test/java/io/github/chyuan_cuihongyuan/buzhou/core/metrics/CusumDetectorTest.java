package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CusumDetectorTest {

    @Test
    void shouldDetectPositiveStepWithinFewFeeds() {
        CusumDetector detector = new CusumDetector(0.5, 10.0);
        int signalAt = -1;
        for (int i = 1; i <= 50; i++) {
            if (detector.feed(3.0) == CusumDetector.POSITIVE_SHIFT) {
                signalAt = i;
                break;
            }
        }
        assertThat(signalAt).isBetween(1, 5);
        assertThat(detector.feeds()).isGreaterThanOrEqualTo(signalAt);
    }

    @Test
    void shouldStayQuietOnBalancedNoise() {
        CusumDetector detector = new CusumDetector(0.0, 50.0);
        Random random = new Random(8028);
        int signals = 0;
        for (int i = 0; i < 5000; i++) {
            int state = detector.feed(random.nextInt(7) - 3);
            if (state != CusumDetector.NORMAL) {
                signals++;
            }
        }
        assertThat(signals).isLessThanOrEqualTo(40);
    }

    @Test
    void shouldDetectNegativeStepAndResetWork() {
        CusumDetector detector = new CusumDetector(0.5, 10.0);
        int signalAt = -1;
        for (int i = 1; i <= 50; i++) {
            if (detector.feed(-3.0) == CusumDetector.NEGATIVE_SHIFT) {
                signalAt = i;
                break;
            }
        }
        assertThat(signalAt).isBetween(1, 5);
        detector.reset();
        assertThat(detector.feeds()).isEqualTo(0);
        for (int i = 0; i < 10; i++) {
            assertThat(detector.feed(0.1)).isEqualTo(CusumDetector.NORMAL);
        }
    }

    @Test
    void shouldMakeHigherThresholdDelaySignal() {
        double[] thresholds = {5.0, 20.0};
        int[] signalFeeds = new int[2];
        for (int t = 0; t < 2; t++) {
            CusumDetector detector = new CusumDetector(0.0, thresholds[t]);
            for (int i = 1; i <= 100; i++) {
                if (detector.feed(2.0) != CusumDetector.NORMAL) {
                    signalFeeds[t] = i;
                    break;
                }
            }
        }
        assertThat(signalFeeds[1]).isGreaterThan(signalFeeds[0]);
    }

    @Test
    void shouldFailFastOnBadParameters() {
        assertThatThrownBy(() -> new CusumDetector(-0.1, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CusumDetector(0.5, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CusumDetector(0.5, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
