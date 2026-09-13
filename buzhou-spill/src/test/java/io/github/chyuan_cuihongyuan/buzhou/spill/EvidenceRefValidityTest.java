package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 843 / T1188：证据引用失效率回归——有效率对账/失效样本封顶/脏 URI/空真。
 */
class EvidenceRefValidityTest {

    @Test
    void invalidRatioAndSamples() {
        var report = EvidenceRefValidity.audit(
                Set.of("spill://a", "spill://b", "spill://gone"),
                uri -> !uri.contains("gone"));

        assertThat(report.totalRefs()).isEqualTo(3);
        assertThat(report.validRefs()).isEqualTo(2);
        assertThat(report.invalidRefs()).isEqualTo(1);
        assertThat(report.invalidRatio()).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(report.invalidSample()).containsExactly("spill://gone");
    }

    @Test
    void sampleCappedSorted() {
        List<String> refs = new java.util.ArrayList<>();
        for (int i = 0; i < EvidenceRefValidity.SAMPLE_LIMIT + 9; i++) {
            refs.add("spill://" + (EvidenceRefValidity.SAMPLE_LIMIT - i));
        }
        var report = EvidenceRefValidity.audit(refs, uri -> false);

        assertThat(report.invalidRefs()).isEqualTo(EvidenceRefValidity.SAMPLE_LIMIT + 9);
        assertThat(report.invalidSample()).hasSize(EvidenceRefValidity.SAMPLE_LIMIT);
        assertThat(report.invalidSample()).isSorted();
        assertThat(report.invalidRatio()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void dirtyUrisAndEmptyTruth() {
        var dirty = EvidenceRefValidity.audit(
                java.util.Arrays.asList(null, "", "  ", "spill://ok"),
                uri -> true);
        assertThat(dirty.totalRefs()).isEqualTo(1);
        assertThat(dirty.invalidRefs()).isZero();

        var empty = EvidenceRefValidity.audit(List.of(), uri -> true);
        assertThat(empty.totalRefs()).isZero();
        assertThat(empty.invalidRatio()).isZero();
    }
}
