package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 44 §B / T160 / impl-131：指标 tag 基数纪律——外部输入（模型名）进 tag 前统一截断。
 */
class MetricTagsTest {

    @Test
    void boundsExternalInputToThirtyTwo() {
        assertThat(MetricTags.bound("gpt-4o")).isEqualTo("gpt-4o");
        assertThat(MetricTags.bound("x".repeat(33))).hasSize(32).isEqualTo("x".repeat(32));
        assertThat(MetricTags.bound("x".repeat(100))).hasSize(32);
        assertThat(MetricTags.bound(null)).isEqualTo("unknown");
        assertThat(MetricTags.bound("  ")).isEqualTo("unknown");
    }
}
