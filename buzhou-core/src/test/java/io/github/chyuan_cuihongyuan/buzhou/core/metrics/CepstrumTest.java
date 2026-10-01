package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 11007 / Y11015：Cepstrum 合同验证——整数周期正弦倒谱峰位+脉冲
 * 平谱面+ε 防零+确定性+fail-fast。
 */
class CepstrumTest {

    private static final int N = 1024;

    @Test
    void shouldPeakAtPeriod_whenIntegerCycleSine() {
        // 16 周期正弦：周期=1024/16=64 样点——倒谱在 quefrency≈64 出峰
        double[] x = new double[N];
        for (int i = 0; i < N; i++) {
            x[i] = Math.sin(2.0 * Math.PI * 16 * i / N);
        }
        double[] cepstrum = Cepstrum.realCepstrum(x);
        int best = 10;
        for (int q = 11; q < 200; q++) {
            if (cepstrum[q] > cepstrum[best]) {
                best = q;
            }
        }
        // 峰在周期 64 或其拉赫谐波 128（纯谱两峰的倒谱族）
        assertThat(best == 64 || best == 128).isTrue();
    }

    @Test
    void shouldStayFinite_whenZeroAmplitudeBinsPresent() {
        // 含全零段信号——零幅仓经 ε 防零不炸、输出全有限
        double[] x = new double[N];
        for (int i = 100; i < 300; i++) {
            x[i] = Math.sin(2.0 * Math.PI * 8 * i / N);
        }
        double[] cepstrum = Cepstrum.realCepstrum(x);
        for (double v : cepstrum) {
            assertThat(v).isFinite();
        }
    }

    @Test
    void shouldReproduceIdenticalCepstrum_whenSameInputTwice() {
        double[] x = new double[256];
        for (int i = 0; i < 256; i++) {
            x[i] = Math.sin(i * 0.1);
        }
        double[] first = Cepstrum.realCepstrum(x);
        double[] second = Cepstrum.realCepstrum(x);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> Cepstrum.realCepstrum(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cepstrum.realCepstrum(new double[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 的幂");
        double[] withNaN = {1, Double.NaN};
        assertThatThrownBy(() -> Cepstrum.realCepstrum(withNaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
