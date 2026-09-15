package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1837 / T2876：失败域配额——三态准入、域普查、借用账。 */
class FailureDomainQuotaTest {

    /** 三态准入：域桶先花、桶满借保留、皆尽拒。 */
    @Test
    void shouldAdmitInThreeTiers() {
        assertThat(FailureDomainQuota.admit(5, 10, 0, 5))
                .isEqualTo(FailureDomainQuota.Admission.FROM_BUCKET);
        assertThat(FailureDomainQuota.admit(10, 10, 2, 5))
                .isEqualTo(FailureDomainQuota.Admission.BORROW_RESERVE);
        assertThat(FailureDomainQuota.admit(12, 10, 5, 5))
                .isEqualTo(FailureDomainQuota.Admission.DENY);
    }

    /** 域普查：桶满/借用/最紧域（并列取首）+ 保留利用率。 */
    @Test
    void censusCountsAtCapBorrowingAndTightest() {
        FailureDomainQuota.DomainCensus census = FailureDomainQuota.census(2, 8, List.of(
                new FailureDomainQuota.DomainUsage("a", 8, 10),
                new FailureDomainQuota.DomainUsage("b", 12, 10),
                new FailureDomainQuota.DomainUsage("c", 3, 5)));
        assertThat(census.domains()).isEqualTo(3);
        assertThat(census.atCap()).isEqualTo(1);
        assertThat(census.borrowing()).isEqualTo(1);
        assertThat(census.tightest()).isEqualTo("b");
        assertThat(census.reserveUtilization()).isEqualTo(0.25d);

        // 并列最紧取首（a 与 c 同 0.6?——用 6/10 与 3/5 并列 0.6）
        FailureDomainQuota.DomainCensus tie = FailureDomainQuota.census(0, 8, List.of(
                new FailureDomainQuota.DomainUsage("x", 6, 10),
                new FailureDomainQuota.DomainUsage("y", 3, 5)));
        assertThat(tie.tightest()).isEqualTo("x");
    }

    /** 空表与 null 哨兵：无域无最紧、保留 0 利用率 -1。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (FailureDomainQuota.DomainCensus census : List.of(
                FailureDomainQuota.census(0, 8, List.of()),
                FailureDomainQuota.census(0, 8, null))) {
            assertThat(census.domains()).isZero();
            assertThat(census.tightest()).isNull();
            assertThat(census.atCap()).isZero();
        }
        assertThat(FailureDomainQuota.census(0, 0, List.of()).reserveUtilization())
                .isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：负计数、空白域。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> FailureDomainQuota.admit(-1, 10, 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("domainUsed 不能为负");
        assertThatThrownBy(() -> FailureDomainQuota.admit(0, -1, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FailureDomainQuota.DomainUsage(" ", 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FailureDomainQuota.DomainUsage("a", 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
