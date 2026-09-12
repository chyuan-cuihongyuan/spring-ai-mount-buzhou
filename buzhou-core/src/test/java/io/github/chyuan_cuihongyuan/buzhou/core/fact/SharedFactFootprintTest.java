package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 741 / T1084–T1085：共享事实足迹——owner 归因降序/永生计数/空表/null。
 */
class SharedFactFootprintTest {

    private static SharedFact fact(String owner, String key, Duration ttl) {
        return new SharedFact(key, "v", owner, Instant.EPOCH, ttl);
    }

    @Test
    void ownerAttributionSortedByFactsDescending() {
        SharedFactFootprint.Report report = SharedFactFootprint.analyze(List.of(
                fact("agent-a", "k1", null),
                fact("agent-a", "k2", Duration.ofHours(1)),
                fact("agent-b", "k3", null),
                fact("agent-b", "k4", null),
                fact("agent-c", "k5", Duration.ofMinutes(5))));
        assertThat(report.totalFacts()).isEqualTo(5);
        assertThat(report.eternalFacts()).isEqualTo(3);
        assertThat(report.rows()).extracting(SharedFactFootprint.Row::owner)
                .containsExactly("agent-a", "agent-b", "agent-c"); // facts 2/2/1 降序，同数字典序 a 在前
        assertThat(report.rows().get(1).eternal()).isEqualTo(2); // agent-b 两条均永生
    }

    @Test
    void emptyAndNull() {
        assertThat(SharedFactFootprint.analyze(List.of()).totalFacts()).isZero();
        assertThatThrownBy(() -> SharedFactFootprint.analyze(null))
                .isInstanceOf(NullPointerException.class);
    }
}
