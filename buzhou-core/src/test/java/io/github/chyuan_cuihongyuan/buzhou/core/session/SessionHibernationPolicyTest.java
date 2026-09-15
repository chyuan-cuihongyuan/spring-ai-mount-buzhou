package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1825 / T2852：休眠分级——三档判、画像读数、足迹节省率。 */
class SessionHibernationPolicyTest {

    /** 三档判定与边界含上：达软阈进 DROWSY、达硬阈进 HIBERNATED。 */
    @Test
    void shouldClassifyThreeBandsWithInclusiveBoundaries() {
        SessionHibernationPolicy.Policy policy =
                new SessionHibernationPolicy.Policy(600_000L, 3_600_000L);
        assertThat(SessionHibernationPolicy.band(599_999L, policy))
                .isEqualTo(SessionHibernationPolicy.Band.ACTIVE);
        assertThat(SessionHibernationPolicy.band(600_000L, policy))
                .isEqualTo(SessionHibernationPolicy.Band.DROWSY);
        assertThat(SessionHibernationPolicy.band(3_599_999L, policy))
                .isEqualTo(SessionHibernationPolicy.Band.DROWSY);
        assertThat(SessionHibernationPolicy.band(3_600_000L, policy))
                .isEqualTo(SessionHibernationPolicy.Band.HIBERNATED);
    }

    /** 档位画像：唤醒税与足迹比随档位单调（省得多醒得慢）。 */
    @Test
    void profileScalesWakeTaxAndFootprint() {
        SessionHibernationPolicy.Policy policy =
                new SessionHibernationPolicy.Policy(100L, 1000L);
        SessionHibernationPolicy.BandProfile active =
                SessionHibernationPolicy.profile(50L, policy);
        assertThat(active.wakeTaxMillis()).isZero();
        assertThat(active.footprintRatio()).isEqualTo(1.0d);

        SessionHibernationPolicy.BandProfile drowsy =
                SessionHibernationPolicy.profile(200L, policy);
        assertThat(drowsy.wakeTaxMillis()).isEqualTo(SessionHibernationPolicy.DROWSY_WAKE_TAX_MILLIS);
        assertThat(drowsy.footprintRatio()).isEqualTo(0.5d);

        SessionHibernationPolicy.BandProfile hibernated =
                SessionHibernationPolicy.profile(2000L, policy);
        assertThat(hibernated.wakeTaxMillis())
                .isEqualTo(SessionHibernationPolicy.HIBERNATED_WAKE_TAX_MILLIS);
        assertThat(hibernated.footprintRatio()).isEqualTo(0.1d);
    }

    /** 普查：三档计数 + 足迹节省率（全活跃 0、全降冷 0.9）。 */
    @Test
    void censusCountsAndFootprintReduction() {
        SessionHibernationPolicy.Policy policy =
                new SessionHibernationPolicy.Policy(100L, 1000L);
        SessionHibernationPolicy.Census mixed = SessionHibernationPolicy.census(policy,
                List.of(50L, 50L, 200L, 2000L, 2000L));
        assertThat(mixed.sessions()).isEqualTo(5);
        assertThat(mixed.active()).isEqualTo(2);
        assertThat(mixed.drowsy()).isEqualTo(1);
        assertThat(mixed.hibernated()).isEqualTo(2);
        // 实际足迹 = 2×1.0 + 1×0.5 + 2×0.1 = 2.7；节省 = 1 − 2.7/5 = 0.46
        assertThat(mixed.footprintReduction())
                .isCloseTo(0.46d, org.assertj.core.data.Offset.offset(1e-9));

        SessionHibernationPolicy.Census allActive =
                SessionHibernationPolicy.census(policy, List.of(0L, 0L));
        assertThat(allActive.footprintReduction()).isZero();

        SessionHibernationPolicy.Census allCold =
                SessionHibernationPolicy.census(policy, List.of(9999L));
        assertThat(allCold.footprintReduction()).isEqualTo(0.9d);

        assertThat(SessionHibernationPolicy.census(policy, null).footprintReduction())
                .isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：负闲置、阈值倒挂、null 元素。 */
    @Test
    void malformedInputFailsFast() {
        SessionHibernationPolicy.Policy policy =
                new SessionHibernationPolicy.Policy(100L, 1000L);
        assertThatThrownBy(() -> SessionHibernationPolicy.band(-1, policy))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionHibernationPolicy.Policy(2000L, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ≤ 软阈 ≤ 硬阈");
        assertThatThrownBy(() -> SessionHibernationPolicy.census(policy,
                java.util.Arrays.asList(1L, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
