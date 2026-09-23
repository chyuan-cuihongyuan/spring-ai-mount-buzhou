package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BanEscalation.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4046 / T6094：fail2ban 封禁递升合同——满限禁、窗滑动
 * 豁免、递升封顶、禁期语义、独立 offender、畸形 fail-fast。
 */
class BanEscalationTest {

    private static final int MAX_RETRY = 3;
    private static final long FIND_TIME = 100L;
    private static final long BASE_BAN = 50L;
    private static final double MULTIPLIER = 2.0;
    private static final long MAX_BAN = 400L;

    private static BanEscalation newBan() {
        return new BanEscalation(MAX_RETRY, FIND_TIME, BASE_BAN, MULTIPLIER, MAX_BAN);
    }

    @Test
    void windowFullShouldBanForBaseDuration() {
        BanEscalation ban = newBan();
        assertThat(ban.recordFailure("a", 10).banned()).isFalse();
        assertThat(ban.recordFailure("a", 20).banned()).isFalse();
        Verdict third = ban.recordFailure("a", 30);
        assertThat(third.banned()).isTrue();
        assertThat(third.banUntil()).isEqualTo(80L);   // 30 + base 50
        assertThat(third.banCount()).isEqualTo(1);
        assertThat(ban.isBanned("a", 79)).isTrue();
        assertThat(ban.isBanned("a", 80)).isFalse();   // 自然到期
    }

    @Test
    void slidingWindowShouldExpireOldFailures() {
        BanEscalation ban = newBan();
        ban.recordFailure("a", 0);
        ban.recordFailure("a", 10);
        Verdict stale = ban.recordFailure("a", 150);   // 前两次已出窗
        assertThat(stale.banned()).isFalse();
        assertThat(stale.windowSize()).isEqualTo(1);
    }

    @Test
    void repeatOffensesShouldEscalateWithCap() {
        BanEscalation ban = newBan();
        // 第一禁：0/1/2 三连 → duration 50（base），until 52
        ban.recordFailure("a", 0);
        ban.recordFailure("a", 1);
        Verdict first = ban.recordFailure("a", 2);
        assertThat(first.banDuration()).isEqualTo(50L);
        // 解禁后（81 > 52）三连再犯 → duration 100（×2）
        ban.recordFailure("a", 81);
        ban.recordFailure("a", 82);
        Verdict second = ban.recordFailure("a", 83);
        assertThat(second.banned()).isTrue();
        assertThat(second.banDuration()).isEqualTo(100L);
        assertThat(second.banCount()).isEqualTo(2);
        // 再解禁（184 > 183）三连 → duration 200（×4）
        ban.recordFailure("a", 184);
        ban.recordFailure("a", 185);
        Verdict third = ban.recordFailure("a", 186);
        assertThat(third.banDuration()).isEqualTo(200L);
        assertThat(third.banCount()).isEqualTo(3);
        // 第四犯 → 400 恰触 maxBan 封顶
        ban.recordFailure("a", 387);
        ban.recordFailure("a", 388);
        Verdict capped = ban.recordFailure("a", 389);
        assertThat(capped.banDuration()).isEqualTo(400L);
        // 第五犯 → 封顶不再增长
        ban.recordFailure("a", 790);
        ban.recordFailure("a", 791);
        Verdict beyond = ban.recordFailure("a", 792);
        assertThat(beyond.banDuration()).isEqualTo(400L);
        assertThat(beyond.banCount()).isEqualTo(5);
    }

    @Test
    void failuresDuringBanShouldNotEscalate() {
        BanEscalation ban = newBan();
        ban.recordFailure("a", 0);
        ban.recordFailure("a", 1);
        Verdict first = ban.recordFailure("a", 2);
        assertThat(first.banned()).isTrue();
        for (long t = 3; t < 50; t++) {
            Verdict during = ban.recordFailure("a", t);   // 禁期内连续失败
            assertThat(during.banned()).isTrue();
            assertThat(ban.banCountOf("a")).isEqualTo(1);  // 不复禁不涨前科
        }
    }

    @Test
    void offendersShouldBeIndependent() {
        BanEscalation ban = newBan();
        ban.recordFailure("a", 0);
        ban.recordFailure("a", 1);
        Verdict unrelated = ban.recordFailure("b", 2);
        assertThat(unrelated.banned()).isFalse();
        assertThat(unrelated.windowSize()).isEqualTo(1);
        assertThat(ban.banCountOf("b")).isZero();
    }

    @Test
    void invalidConstructionShouldFailFast() {
        assertThatThrownBy(() -> new BanEscalation(0, FIND_TIME, BASE_BAN, MULTIPLIER, MAX_BAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BanEscalation(MAX_RETRY, 0, BASE_BAN, MULTIPLIER, MAX_BAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BanEscalation(MAX_RETRY, FIND_TIME, 0, MULTIPLIER, MAX_BAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BanEscalation(MAX_RETRY, FIND_TIME, BASE_BAN, 0.5, MAX_BAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BanEscalation(MAX_RETRY, FIND_TIME, BASE_BAN, MULTIPLIER, 10))
                .isInstanceOf(IllegalArgumentException.class);   // maxBan < base
    }
}
