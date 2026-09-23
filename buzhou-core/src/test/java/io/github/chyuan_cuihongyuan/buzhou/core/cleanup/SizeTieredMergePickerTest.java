package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SizeTieredMergePicker.Candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4013 / T6028：尺寸分层合并合同——同量级成组、阈值门、
 * 多桶竞选、尺寸序输出、畸形 fail-fast。
 */
class SizeTieredMergePickerTest {

    @Test
    void similarSizedBucketShouldFormGroupExcludingOutlier() {
        SizeTieredMergePicker picker = new SizeTieredMergePicker(4, 32, 1.5);
        List<String> group = picker.pickMergeGroup(List.of(
                new Candidate("s1", 100), new Candidate("s2", 105),
                new Candidate("s3", 110), new Candidate("s4", 115),
                new Candidate("giant", 100_000)));   // 巨块异层不混压
        assertThat(group).containsExactly("s1", "s2", "s3", "s4");   // 尺寸升序
    }

    @Test
    void thresholdsShouldGateAndCap() {
        SizeTieredMergePicker picker = new SizeTieredMergePicker(4, 4, 1.5);
        assertThat(picker.pickMergeGroup(List.of(   // 桶员 3 < min——不成组
                new Candidate("a", 100), new Candidate("b", 105), new Candidate("c", 110))))
                .isEmpty();
        List<String> capped = picker.pickMergeGroup(List.of(   // 桶员 6 > max——取最小 4
                new Candidate("a", 100), new Candidate("b", 101), new Candidate("c", 102),
                new Candidate("d", 103), new Candidate("e", 104), new Candidate("f", 105)));
        assertThat(capped).containsExactly("a", "b", "c", "d");
        assertThat(picker.minThreshold()).isEqualTo(4);
        assertThat(picker.maxThreshold()).isEqualTo(4);
    }

    @Test
    void competingBucketsShouldPickMostMembers() {
        SizeTieredMergePicker picker = new SizeTieredMergePicker(4, 32, 1.5);
        List<String> picked = picker.pickMergeGroup(List.of(
                new Candidate("small1", 100), new Candidate("small2", 110),
                new Candidate("small3", 120), new Candidate("small4", 130),
                new Candidate("big1", 10_000), new Candidate("big2", 10_100),
                new Candidate("big3", 10_200), new Candidate("big4", 10_300),
                new Candidate("big5", 10_400)));   // 大桶 5 员胜出
        assertThat(picked).containsExactly("big1", "big2", "big3", "big4", "big5");
    }

    @Test
    void noCandidatesShouldReturnEmpty() {
        SizeTieredMergePicker picker = new SizeTieredMergePicker(4, 32, 1.5);
        assertThat(picker.pickMergeGroup(List.of())).isEmpty();
        assertThat(picker.pickMergeGroup(List.of(new Candidate("lonely", 50)))).isEmpty();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SizeTieredMergePicker(1, 4, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SizeTieredMergePicker(8, 4, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SizeTieredMergePicker(4, 8, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        SizeTieredMergePicker picker = new SizeTieredMergePicker(4, 8, 1.5);
        assertThatThrownBy(() -> picker.pickMergeGroup(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> picker.pickMergeGroup(List.of(new Candidate("x", -1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> picker.pickMergeGroup(List.of(new Candidate(null, 10))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
