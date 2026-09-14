package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-694 续 / spec 952：摘要版本链缺口审计——无缺口空表、缺口精确定位
 * （1,2,5 → 缺 3,4）、乱序输入排序后判定、version ≤ 0/重复 fail-fast。
 */
class SummaryVersionAuditTest {

    private static StructuredSummary summary(long version) {
        return new StructuredSummary("s1", version,
                java.util.Map.of("s", "内容"), 100, Instant.EPOCH);
    }

    @Test
    void contiguousChainHasNoGaps() {
        assertThat(SummaryVersionAudit.gaps(List.of(
                summary(1), summary(2), summary(3)))).isEmpty();
    }

    @Test
    void gapsLocatedPrecisely() {
        List<Long> gaps = SummaryVersionAudit.gaps(List.of(
                summary(1), summary(2), summary(5)));
        assertThat(gaps).containsExactly(3L, 4L);
    }

    @Test
    void outOfOrderInputStillSorted() {
        List<Long> gaps = SummaryVersionAudit.gaps(List.of(
                summary(5), summary(1), summary(2)));
        assertThat(gaps).containsExactly(3L, 4L); // 排序后判定（结果与输入序无关）
    }

    @Test
    void duplicateAndNonPositiveFailFast() {
        assertThatThrownBy(() -> SummaryVersionAudit.gaps(List.of(
                summary(1), summary(1)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SummaryVersionAudit.gaps(List.of(
                summary(0)))).isInstanceOf(IllegalArgumentException.class);
    }
}
