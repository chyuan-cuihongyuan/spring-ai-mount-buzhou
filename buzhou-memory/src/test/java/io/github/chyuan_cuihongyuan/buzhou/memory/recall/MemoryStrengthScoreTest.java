package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.memory.recall.MemoryStrengthScore.Weights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 2003 / T3108：记忆强度三分量评分合同——半衰期衰减、对数饱和、
 * 重要度钳制、权重归一、单调性、畸形 fail-fast。
 */
class MemoryStrengthScoreTest {

    private static final long HALF_LIFE = MemoryStrengthScore.DEFAULT_HALF_LIFE_MILLIS;

    @Test
    void freshHotImportantMemoryShouldScoreNearTop() {
        // Δt=0（recency=1）、100 次访问（frequency 高）、importance=1
        double score = MemoryStrengthScore.score(0, 100, 1.0d,
                MemoryStrengthScore.DEFAULT_WEIGHTS);
        assertThat(score).isGreaterThan(0.9d);
        assertThat(score).isLessThanOrEqualTo(1.0d);
    }

    @Test
    void recencyShouldHalveAtHalfLife() {
        // 只用 recency 权重：一个半衰期后分量恰 0.5
        Weights onlyRecency = new Weights(1, 0, 0);
        double fresh = MemoryStrengthScore.score(0, 0, 0, onlyRecency, HALF_LIFE);
        double oneHalfLife = MemoryStrengthScore.score(HALF_LIFE, 0, 0, onlyRecency, HALF_LIFE);
        double twoHalfLives = MemoryStrengthScore.score(2 * HALF_LIFE, 0, 0, onlyRecency, HALF_LIFE);
        assertThat(fresh).isCloseTo(1.0d, within(1e-12));
        assertThat(oneHalfLife).isCloseTo(0.5d, within(1e-12));
        assertThat(twoHalfLives).isCloseTo(0.25d, within(1e-12)); // 指数衰减：半衰期叠加平方
    }

    @Test
    void frequencyShouldSaturateLogarithmically() {
        Weights onlyFrequency = new Weights(0, 1, 0);
        double zero = MemoryStrengthScore.score(0, 0, 0, onlyFrequency);
        double one = MemoryStrengthScore.score(0, 1, 0, onlyFrequency);
        double many = MemoryStrengthScore.score(0, 10_000, 0, onlyFrequency);
        double mega = MemoryStrengthScore.score(0, 1_000_000, 0, onlyFrequency);
        assertThat(zero).isZero(); // 零访问频度分量 0
        assertThat(one).isGreaterThan(zero);
        assertThat(many).isGreaterThan(one);
        assertThat(mega).isGreaterThan(many);
        assertThat(mega).isLessThan(1.0d); // 对数饱和：永不达 1
        // 边际递减：10000→1000000 的增益小于 0→10 的增益
        assertThat(mega - many).isLessThan(many - one);
    }

    @Test
    void importanceShouldClampIntoUnitInterval() {
        Weights onlyImportance = new Weights(0, 0, 1);
        assertThat(MemoryStrengthScore.score(0, 0, 0.7d, onlyImportance))
                .isCloseTo(0.7d, within(1e-12));
        assertThat(MemoryStrengthScore.score(0, 0, 5.0d, onlyImportance))
                .isCloseTo(1.0d, within(1e-12)); // 超界钳 1
        assertThat(MemoryStrengthScore.score(0, 0, -3.0d, onlyImportance))
                .isCloseTo(0.0d, within(1e-12)); // 负值钳 0
    }

    @Test
    void olderAccessShouldNeverScoreHigher() {
        double younger = MemoryStrengthScore.score(1000, 5, 0.5d,
                MemoryStrengthScore.DEFAULT_WEIGHTS);
        double older = MemoryStrengthScore.score(2000, 5, 0.5d,
                MemoryStrengthScore.DEFAULT_WEIGHTS);
        assertThat(older).isLessThan(younger); // 单调性：Δt 增分降
    }

    @Test
    void outputShouldAlwaysStayWithinUnitInterval() {
        assertThat(MemoryStrengthScore.score(0, 0, 0, MemoryStrengthScore.DEFAULT_WEIGHTS))
                .isGreaterThanOrEqualTo(0.0d);
        assertThat(MemoryStrengthScore.score(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, 1.0d,
                MemoryStrengthScore.DEFAULT_WEIGHTS)).isLessThanOrEqualTo(1.0d);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> MemoryStrengthScore.score(-1, 0, 0, new Weights(1, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemoryStrengthScore.score(0, -1, 0, new Weights(1, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Weights(-0.1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Weights(0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("全零");
        assertThatThrownBy(() -> MemoryStrengthScore.score(0, 0, 0, new Weights(1, 0, 0), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
