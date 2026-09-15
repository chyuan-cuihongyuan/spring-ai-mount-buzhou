package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1734 / T2670：SkillFunnelStats 直测——三级漏斗/转化率哨兵。
 */
class SkillFunnelStatsTest {

    @Test
    void emptyCarriesSentinels() {
        var stats = new SkillFunnelStats();
        var census = stats.census();
        assertThat(census.loadRate()).isEqualTo(-1d);
        assertThat(census.applyRate()).isEqualTo(-1d);
    }

    @Test
    void funnelRatesTally() {
        var stats = new SkillFunnelStats();
        stats.recordSearch();
        stats.recordSearch();
        stats.recordSearch();
        stats.recordSearch();
        stats.recordLoad();
        stats.recordLoad();
        stats.recordApply();
        var census = stats.census();
        assertThat(census.searched()).isEqualTo(4);
        assertThat(census.loaded()).isEqualTo(2);
        assertThat(census.applied()).isEqualTo(1);
        assertThat(census.loadRate()).isCloseTo(0.5d, within(1e-9));
        assertThat(census.applyRate()).isCloseTo(0.5d, within(1e-9));
    }

    @Test
    void loadsWithoutSearchStillRateApplied() {
        var stats = new SkillFunnelStats();
        stats.recordLoad();
        stats.recordApply();
        var census = stats.census();
        assertThat(census.loadRate()).isEqualTo(-1d);
        assertThat(census.applyRate()).isCloseTo(1d, within(1e-9));
    }
}
