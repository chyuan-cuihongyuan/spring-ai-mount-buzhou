package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-658 / spec 905：健康聚合评分——全 UP=100 healthy、DOWN 拖均且清单精确、
 * UNKNOWN=50 中性、分档边界 90/70、空集约定、null fail-fast。
 */
class BuzhouHealthScoreTest {

    private static BuzhouHealth mech(String name, BuzhouHealth.Status status) {
        return new BuzhouHealth() {
            @Override
            public String mechanism() {
                return name;
            }

            @Override
            public Status status() {
                return status;
            }
        };
    }

    @Test
    void allUpScoresHundred() {
        BuzhouHealthScore.ScoreReport report = BuzhouHealthScore.compute(List.of(
                mech("a", BuzhouHealth.Status.UP),
                mech("b", BuzhouHealth.Status.UP)));
        assertThat(report.score()).isEqualTo(100);
        assertThat(report.tier()).isEqualTo("healthy");
        assertThat(report.upCount()).isEqualTo(2);
        assertThat(report.downMechanisms()).isEmpty();
    }

    @Test
    void downDragsAverageAndListsMechanisms() {
        BuzhouHealthScore.ScoreReport report = BuzhouHealthScore.compute(List.of(
                mech("ok1", BuzhouHealth.Status.UP),
                mech("broken", BuzhouHealth.Status.DOWN),
                mech("off", BuzhouHealth.Status.UNKNOWN),
                mech("ok2", BuzhouHealth.Status.UP)));
        // (100+0+50+100)/4 = 62.5 → 63（unhealthy）
        assertThat(report.score()).isEqualTo(63);
        assertThat(report.tier()).isEqualTo("unhealthy");
        assertThat(report.downCount()).isEqualTo(1);
        assertThat(report.unknownCount()).isEqualTo(1);
        assertThat(report.downMechanisms()).containsExactly("broken");
    }

    @Test
    void unknownIsNeutral() {
        // 1 UP + 1 UNKNOWN = 75 → degraded（未启用拉低但不致命）
        BuzhouHealthScore.ScoreReport report = BuzhouHealthScore.compute(List.of(
                mech("on", BuzhouHealth.Status.UP),
                mech("off", BuzhouHealth.Status.UNKNOWN)));
        assertThat(report.score()).isEqualTo(75);
        assertThat(report.tier()).isEqualTo("degraded");
    }

    @Test
    void tierBoundariesAtFloors() {
        // 恰 90：18 UP + 2 UNKNOWN（1800/20=90）→ healthy（含下限）
        List<BuzhouHealth> exactly90 = new java.util.ArrayList<>();
        for (int i = 0; i < 18; i++) {
            exactly90.add(mech("up" + i, BuzhouHealth.Status.UP));
        }
        exactly90.add(mech("u1", BuzhouHealth.Status.UNKNOWN));
        exactly90.add(mech("u2", BuzhouHealth.Status.UNKNOWN));
        assertThat(BuzhouHealthScore.compute(exactly90).tier()).isEqualTo("healthy");

        // 恰 70：7 UP + 3 UNKNOWN（700/10=70）→ degraded（含下限）
        List<BuzhouHealth> exactly70 = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            exactly70.add(mech("up" + i, BuzhouHealth.Status.UP));
        }
        for (int i = 0; i < 3; i++) {
            exactly70.add(mech("unk" + i, BuzhouHealth.Status.UNKNOWN));
        }
        assertThat(BuzhouHealthScore.compute(exactly70).tier()).isEqualTo("degraded");
    }

    @Test
    void emptyContributorsConventionHealthy() {
        BuzhouHealthScore.ScoreReport report = BuzhouHealthScore.compute(List.of());
        assertThat(report.score()).isEqualTo(100);
        assertThat(report.tier()).isEqualTo("healthy");
    }

    @Test
    void nullArgsFailFast() {
        assertThatThrownBy(() -> BuzhouHealthScore.compute(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BuzhouHealthScore.compute(
                java.util.Arrays.asList(null, mech("a", BuzhouHealth.Status.UP))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
