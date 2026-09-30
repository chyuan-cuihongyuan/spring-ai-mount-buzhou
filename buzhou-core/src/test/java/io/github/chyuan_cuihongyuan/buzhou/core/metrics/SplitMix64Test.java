package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitMix64Test {

    @Test
    void shouldMatchSteeleGoldenVectors() {
        // 金向量锚（标准常数交叉验证——Python 独立实现逐位对拍锁定）
        assertThat(SplitMix64.mixOutput(SplitMix64.GOLDEN_GAMMA + 0)).isEqualTo(0xe220a8397b1dcdafL);
        assertThat(SplitMix64.mixOutput(SplitMix64.GOLDEN_GAMMA + 1)).isEqualTo(0x910a2dec89025cc1L);
        // 定序步进
        assertThat(SplitMix64.nextState(0L)).isEqualTo(SplitMix64.GOLDEN_GAMMA);
        // next 便利面 = 两步组合
        assertThat(SplitMix64.next(1L)).isEqualTo(SplitMix64.mixOutput(SplitMix64.GOLDEN_GAMMA + 1));
    }

    @Test
    void shouldAvalancheAdjacentSeeds() {
        // 相邻种子雪崩圣像：seed 与 seed+1 输出平均翻位 ≈32（64 位半）
        long totalFlips = 0;
        int trials = 2000;
        for (int t = 1; t <= trials; t++) {
            long a = SplitMix64.next(t);
            long b = SplitMix64.next(t + 1);
            totalFlips += Long.bitCount(a ^ b);
        }
        double average = totalFlips / (double) trials;
        assertThat(average).isBetween(28.0, 36.0);
        // 分裂流独立：种子 1..1000 前十步值互异
        Set<Long> firstSteps = new HashSet<>();
        for (long seed = 1; seed <= 1000; seed++) {
            long state = seed;
            state = SplitMix64.nextState(state);
            firstSteps.add(SplitMix64.mixOutput(state));
        }
        assertThat(firstSteps).hasSize(1000);
        // 同种子确定性双跑
        assertThat(SplitMix64.next(123456789L)).isEqualTo(SplitMix64.next(123456789L));
    }

    @Test
    void shouldBeFailFast() {
        // nextState(0) 合法（状态可为零——γ 步进即逃逸）
        assertThat(SplitMix64.nextState(0)).isEqualTo(SplitMix64.GOLDEN_GAMMA);
        assertThatThrownBy(() -> SplitMix64.mixOutput(0)).hasMessageContaining("非 0");
    }
}
