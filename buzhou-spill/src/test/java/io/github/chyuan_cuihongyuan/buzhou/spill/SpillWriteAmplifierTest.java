package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 815 / T1132：写放大读数回归——store 双文件放大/markLinked 重写放大/
 * 近窗滑动/零逻辑忽略/空真。
 */
class SpillWriteAmplifierTest {

    @Test
    void storeAndMarkLinkedAmplification() {
        SpillWriteAmplifier amp = new SpillWriteAmplifier();
        // store：内容 1000 字节，data 1000 + meta 200 = 1200 物理
        amp.recordWrite(1000, 1200);
        // markLinked：同内容再写 meta 200
        amp.recordWrite(1000, 1400);

        SpillWriteAmplifier.Stats stats = amp.stats();
        assertThat(stats.writes()).isEqualTo(2);
        assertThat(stats.logicalBytes()).isEqualTo(2000);
        assertThat(stats.physicalBytes()).isEqualTo(2600);
        assertThat(stats.amplificationRatio()).isCloseTo(1.3, within(1e-9));
    }

    @Test
    void recentWindowReflectsCurrentBehavior() {
        SpillWriteAmplifier amp = new SpillWriteAmplifier();
        amp.recordWrite(1000, 1000); // 早期 1.0（环被挤出）
        for (int i = 0; i < SpillWriteAmplifier.WINDOW + 10; i++) {
            amp.recordWrite(100, 300); // 近期全 3.0
        }
        SpillWriteAmplifier.Stats stats = amp.stats();
        assertThat(stats.writes()).isEqualTo(SpillWriteAmplifier.WINDOW + 11);
        // 总量被早期 1.0 拉低一点，近窗应为纯 3.0
        assertThat(stats.recentRatio()).isCloseTo(3.0, within(1e-9));
        assertThat(stats.recentP95Ratio()).isCloseTo(3.0, within(1e-9));
        assertThat(stats.amplificationRatio()).isLessThan(3.0); // 总均值被稀释
    }

    @Test
    void zeroLogicalAndNegativeIgnored() {
        SpillWriteAmplifier amp = new SpillWriteAmplifier();
        amp.recordWrite(0, 100);
        amp.recordWrite(-5, 100);
        amp.recordWrite(100, -1);
        assertThat(amp.stats().writes()).isZero();
        assertThat(amp.stats().amplificationRatio()).isZero();
    }

    @Test
    void emptyStatsAreAllZero() {
        SpillWriteAmplifier.Stats stats = new SpillWriteAmplifier().stats();
        assertThat(stats.writes()).isZero();
        assertThat(stats.logicalBytes()).isZero();
        assertThat(stats.physicalBytes()).isZero();
        assertThat(stats.amplificationRatio()).isZero();
        assertThat(stats.recentRatio()).isZero();
        assertThat(stats.recentP95Ratio()).isZero();
    }

    @Test
    void p95OfMixedRatios() {
        SpillWriteAmplifier amp = new SpillWriteAmplifier();
        for (int i = 0; i < 100; i++) {
            amp.recordWrite(100, i < 95 ? 100 : 500); // 95 次 1.0、5 次 5.0
        }
        // 近窗 64 个样本：前 95 次 1.0 的后 59 个 + 5 次 5.0 → (59+25)/64=1.3125
        SpillWriteAmplifier.Stats stats = amp.stats();
        assertThat(stats.recentP95Ratio()).isCloseTo(5.0, within(1e-9));
        assertThat(stats.recentRatio()).isCloseTo(1.3125, within(1e-9));
    }
}
