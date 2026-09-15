package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1735 / T2672：SkillRankAgreement 纯函数直测——τ 三态/哨兵/公共项。
 */
class SkillRankAgreementTest {

    @Test
    void identicalRanksHavePerfectAgreement() {
        var agreement = SkillRankAgreement.tau(
                List.of("a", "b", "c"), List.of("a", "b", "c"));
        assertThat(agreement.tau()).isCloseTo(1d, within(1e-9));
        assertThat(agreement.concordant()).isEqualTo(3);
        assertThat(agreement.discordant()).isZero();
    }

    @Test
    void reversedRanksHavePerfectDisagreement() {
        var agreement = SkillRankAgreement.tau(
                List.of("a", "b", "c"), List.of("c", "b", "a"));
        assertThat(agreement.tau()).isCloseTo(-1d, within(1e-9));
    }

    @Test
    void disjointAndDegenerateCarrySentinel() {
        assertThat(SkillRankAgreement.tau(
                List.of("a"), List.of("a")).tau()).isEqualTo(-1d);
        assertThat(SkillRankAgreement.tau(
                List.of("a", "b"), List.of("x", "y")).tau()).isEqualTo(-1d);
        assertThat(SkillRankAgreement.tau(null, null).commonItems()).isZero();
    }

    @Test
    void partialOverlapUsesCommonItemsOnly() {
        // 公共项 {b, c}：A 序 b<c，B 序 c 在 b 前 → 相反
        var agreement = SkillRankAgreement.tau(
                List.of("a", "b", "c"), List.of("c", "x", "b"));
        assertThat(agreement.commonItems()).isEqualTo(2);
        assertThat(agreement.tau()).isCloseTo(-1d, within(1e-9));
    }
}
