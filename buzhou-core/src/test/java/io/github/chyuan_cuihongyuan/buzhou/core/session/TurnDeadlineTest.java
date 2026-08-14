package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-28 / spec 13 §core-2：{@link TurnDeadline} 数学——remaining 负值归零、min 组合器、
 * 到期边界与 none 哨兵语义。
 */
class TurnDeadlineTest {

    @Test
    void noneSentinelIsNeverExpiredAndEffectivelyUnbounded() {
        TurnDeadline none = TurnDeadline.none();

        assertThat(none.isNone()).isTrue();
        assertThat(none.isExpired()).isFalse();
        // 哨兵剩余量足够大：大于任何实际可配置的预算（min 组合中恒退位给有限值）
        assertThat(none.remaining()).isGreaterThan(Duration.ofDays(365L));
        assertThat(none.remainingMillis()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void remainingIsPositiveBeforeDeadlineAndClampedToZeroAfter() {
        TurnDeadline future = TurnDeadline.at(Instant.now().plusSeconds(10L));

        assertThat(future.isNone()).isFalse();
        assertThat(future.isExpired()).isFalse();
        Duration remaining = future.remaining();
        assertThat(remaining.isNegative()).isFalse();
        // Windows 时钟粒度下两次 now() 可能同刻：允许恰好等于预算
        assertThat(remaining).isLessThanOrEqualTo(Duration.ofSeconds(10L));
        assertThat(future.remainingMillis()).isPositive();

        TurnDeadline past = TurnDeadline.at(Instant.now().minusSeconds(1L));
        assertThat(past.remaining()).isEqualTo(Duration.ZERO);
        assertThat(past.remainingMillis()).isZero();
        assertThat(past.isExpired()).isTrue();
    }

    @Test
    void inFactoryScalesBudgetFromNow() {
        TurnDeadline deadline = TurnDeadline.in(Duration.ofMillis(500L));

        assertThat(deadline.isExpired()).isFalse();
        assertThat(deadline.remainingMillis()).isLessThanOrEqualTo(500L);
        assertThat(deadline.remainingMillis()).isGreaterThan(400L);

        // 负/零预算：构造即到期
        assertThat(TurnDeadline.in(Duration.ZERO).isExpired()).isTrue();
        assertThat(TurnDeadline.in(Duration.ofMillis(-1L)).isExpired()).isTrue();
    }

    @Test
    void minCombinatorPicksEarlierInstant() {
        Instant now = Instant.now();
        TurnDeadline earlier = TurnDeadline.at(now.plusSeconds(10L));
        TurnDeadline later = TurnDeadline.at(now.plusSeconds(60L));

        assertThat(earlier.min(later)).isEqualTo(earlier);
        assertThat(later.min(earlier)).isEqualTo(earlier);
        assertThat(earlier.min(earlier)).isEqualTo(earlier);

        // 哨兵退位：任一有限即以有限为准，双向皆然
        assertThat(TurnDeadline.none().min(later)).isEqualTo(later);
        assertThat(later.min(TurnDeadline.none())).isEqualTo(later);
        assertThat(TurnDeadline.none().min(TurnDeadline.none())).isEqualTo(TurnDeadline.none());

        // null 视作无界：返回另一个自身
        assertThat(earlier.min(null)).isEqualTo(earlier);
    }

    @Test
    void atFactoryRejectsNullAndRecordEqualityHolds() {
        assertThatThrownBy(() -> TurnDeadline.at(null)).isInstanceOf(NullPointerException.class);

        Instant instant = Instant.now().plusSeconds(5L);
        assertThat(TurnDeadline.at(instant)).isEqualTo(TurnDeadline.at(instant));
        assertThat(TurnDeadline.none()).isEqualTo(new TurnDeadline(null));
    }

    @Test
    void turnLoopPolicyPicksTighterOfDeadlineAndLoopTimeout() {
        // 两者都配置：取更紧者
        TurnLoopPolicy both = new TurnLoopPolicy(null, Duration.ofSeconds(10L), java.util.List.of(),
                null, null, Duration.ofSeconds(5L));
        assertThat(both.effectiveTurnBudget()).isEqualTo(Duration.ofSeconds(5L));
        assertThat(both.effectiveTurnDeadline().isNone()).isFalse();

        TurnLoopPolicy reversed = new TurnLoopPolicy(null, Duration.ofSeconds(3L), java.util.List.of(),
                null, null, Duration.ofSeconds(30L));
        assertThat(reversed.effectiveTurnBudget()).isEqualTo(Duration.ofSeconds(3L));

        // 仅其一：用之；都无：null + none 哨兵
        assertThat(new TurnLoopPolicy(null, Duration.ofSeconds(7L), java.util.List.of(), null, null, null)
                .effectiveTurnBudget()).isEqualTo(Duration.ofSeconds(7L));
        assertThat(new TurnLoopPolicy(null, null, java.util.List.of(), null, null, Duration.ofSeconds(7L))
                .effectiveTurnBudget()).isEqualTo(Duration.ofSeconds(7L));
        assertThat(TurnLoopPolicy.defaults().effectiveTurnBudget()).isNull();
        assertThat(TurnLoopPolicy.defaults().effectiveTurnDeadline()).isEqualTo(TurnDeadline.none());

        // fluent 入口
        assertThat(TurnLoopPolicy.of(3).withTurnDeadline(Duration.ofMillis(200L))
                .effectiveTurnBudget()).isEqualTo(Duration.ofMillis(200L));
    }
}
