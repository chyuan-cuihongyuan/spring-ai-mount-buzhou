package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1807 / T2816：追限事件会话化——切段、乱序容忍、密度读数。 */
class ViolationEpisodeMergerTest {

    /** 相邻并入同段、跨容忍窗切段：散点抖动 vs 持续连击分开。 */
    @Test
    void shouldMergeWithinGapAndSplitAcross() {
        ViolationEpisodeMerger.MergeReport report = ViolationEpisodeMerger.merge(100L,
                List.of(1000L, 1050L, 1080L, 5000L, 5050L));
        assertThat(report.totalHits()).isEqualTo(5);
        assertThat(report.episodes()).isEqualTo(2);
        assertThat(report.longest().startMillis()).isEqualTo(1000L);
        assertThat(report.longest().endMillis()).isEqualTo(1080L);
        assertThat(report.longest().hits()).isEqualTo(3);
        assertThat(report.longest().spanMillis()).isEqualTo(80L);
        assertThat(report.mergedSpans()).isEqualTo(130L);
        assertThat(report.hitsPerEpisode()).isEqualTo(2.5d);
    }

    /** 乱序输入容忍：内部排序后同语义。 */
    @Test
    void unsortedInputYieldsSameSemantics() {
        ViolationEpisodeMerger.MergeReport sorted = ViolationEpisodeMerger.merge(10L,
                List.of(100L, 110L, 300L));
        ViolationEpisodeMerger.MergeReport shuffled = ViolationEpisodeMerger.merge(10L,
                List.of(300L, 100L, 110L));
        assertThat(shuffled.episodes()).isEqualTo(sorted.episodes()).isEqualTo(2);
        assertThat(shuffled.totalHits()).isEqualTo(sorted.totalHits()).isEqualTo(3);
        assertThat(shuffled.longest().spanMillis()).isEqualTo(10L);
    }

    /** 单点成段：span 0 也可成段；最长段并列取首个。 */
    @Test
    void singlePointsFormZeroSpanEpisodes() {
        ViolationEpisodeMerger.MergeReport report = ViolationEpisodeMerger.merge(0L,
                List.of(10L, 20L, 30L));
        assertThat(report.episodes()).isEqualTo(3);
        assertThat(report.mergedSpans()).isZero();
        assertThat(report.hitsPerEpisode()).isEqualTo(1.0d);
        assertThat(report.longest().startMillis()).isEqualTo(10L);
    }

    /** 空表与 null 同口径：零段零事件、密度 -1 哨兵、最长段 null。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (ViolationEpisodeMerger.MergeReport report : List.of(
                ViolationEpisodeMerger.merge(10L, List.of()),
                ViolationEpisodeMerger.merge(10L, null))) {
            assertThat(report.episodes()).isZero();
            assertThat(report.totalHits()).isZero();
            assertThat(report.longest()).isNull();
            assertThat(report.hitsPerEpisode()).isEqualTo(-1d);
        }
    }

    /** 畸形入参 fail-fast：负容忍窗与 null 时点。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ViolationEpisodeMerger.merge(-1L, List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gapToleranceMillis 不能为负");
        assertThatThrownBy(() -> ViolationEpisodeMerger.merge(10L,
                java.util.Arrays.asList(1L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("追限时点不能为 null");
    }
}
