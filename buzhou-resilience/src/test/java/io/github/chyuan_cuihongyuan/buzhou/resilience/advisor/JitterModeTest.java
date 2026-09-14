package io.github.chyuan_cuihongyuan.buzhou.resilience.advisor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 退避抖动模式测试（spec 1631 / T2413–T2414 / impl 1184）：EQUAL（既有 ±j 对称）
 * / FULL（[0,cap] 全随机）/ DECORRELATED（[base, min(cap, prev×3)]）三模式值域
 * 钉住 + 解析 fail-fast。AWS Exponential Backoff and Jitter 思想。
 */
class JitterModeTest {

    private static ResilienceAdvisor advisor(JitterMode mode) {
        io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties props =
                new io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties(
                        null, 3, Duration.ofMillis(100), Duration.ofSeconds(10), 2.0, 0.2,
                        null, null, null, null, null, null, null, null, null, null, null, null);
        return new ResilienceAdvisor(props, new io.github.chyuan_cuihongyuan.buzhou.resilience
                .DefaultErrorClassifier(), e -> { }, null, null, null, null, "m", null, null,
                null, null)
                .withJitterMode(mode);
    }

    private static List<Long> samples(ResilienceAdvisor a, int attempt, int n) throws Exception {
        List<Long> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            var m = ResilienceAdvisor.class.getDeclaredMethod("computeBackoff", int.class);
            m.setAccessible(true);
            out.add(((Duration) m.invoke(a, attempt)).toMillis());
            Thread.sleep(1);
        }
        return out;
    }

    @Test
    void equalModeStaysWithinSymmetricBand() throws Exception {
        List<Long> s = samples(advisor(JitterMode.EQUAL), 2, 200); // capped=200ms
        assertThat(s).allSatisfy(v -> assertThat(v).isBetween(1L, 240L)); // ±20%
    }

    @Test
    void fullModeSpreadsAcrossWholeRange() throws Exception {
        List<Long> s = samples(advisor(JitterMode.FULL), 2, 300); // capped=200ms
        assertThat(s).allSatisfy(v -> assertThat(v).isBetween(1L, 200L));
        assertThat(s.stream().filter(v -> v < 40).count()).isPositive(); // 低区有落点（全随机特征）
        assertThat(s.stream().filter(v -> v > 160).count()).isPositive(); // 高区有落点
    }

    @Test
    void decorrelatedModeBoundedByPreviousTriple() throws Exception {
        ResilienceAdvisor a = advisor(JitterMode.DECORRELATED);
        List<Long> s = samples(a, 2, 200); // capped=200，prev 起点 100 → ceiling min(200, 300)=200
        assertThat(s).allSatisfy(v -> assertThat(v).isBetween(100L, 200L)); // floor=base 100
    }

    @Test
    void parseFailsFastOnUnknownValue() {
        assertThat(JitterMode.parse(null)).isEqualTo(JitterMode.EQUAL);
        assertThat(JitterMode.parse(" full ")).isEqualTo(JitterMode.FULL);
        assertThatThrownBy(() -> JitterMode.parse("bogus"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
