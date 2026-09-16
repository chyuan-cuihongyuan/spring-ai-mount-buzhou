package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.tools.http.RedirectBudget.Decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2030 / T3162：重定向预算合同——预算内跟随、跳数耗尽停、环在
 * 预算前识破、零预算直拒、锚定语义、畸形 fail-fast。
 */
class RedirectBudgetTest {

    @Test
    void withinBudgetShouldFollow() {
        RedirectBudget budget = new RedirectBudget(5);
        budget.startFrom("http://a/1");
        assertThat(budget.decide("http://a/2")).isEqualTo(Decision.FOLLOW);
        assertThat(budget.decide("http://a/3")).isEqualTo(Decision.FOLLOW);
        assertThat(budget.hops()).isEqualTo(2);
    }

    @Test
    void exhaustedBudgetShouldStop() {
        RedirectBudget budget = new RedirectBudget(2);
        budget.startFrom("http://a/1");
        assertThat(budget.decide("http://a/2")).isEqualTo(Decision.FOLLOW);
        assertThat(budget.decide("http://a/3")).isEqualTo(Decision.FOLLOW);
        assertThat(budget.decide("http://a/4")).isEqualTo(Decision.BUDGET_EXHAUSTED); // 第 3 跳无预算
        assertThat(budget.hops()).isEqualTo(2); // 记账不超预算
    }

    @Test
    void loopShouldBeDetectedBeforeBudgetExhausts() {
        RedirectBudget budget = new RedirectBudget(50);
        budget.startFrom("http://a/1");        // 起点
        assertThat(budget.decide("http://b/1")).isEqualTo(Decision.FOLLOW);
        assertThat(budget.decide("http://a/1")).isEqualTo(Decision.LOOP_DETECTED); // A→B→A 环识破
        assertThat(budget.hops()).isEqualTo(1); // 环跳不记账
    }

    @Test
    void zeroBudgetShouldRejectFirstRedirect() {
        RedirectBudget budget = new RedirectBudget(0);
        budget.startFrom("http://a/1");
        assertThat(budget.decide("http://a/2")).isEqualTo(Decision.BUDGET_EXHAUSTED); // 不跟随
    }

    @Test
    void visitedSetShouldAnchorFromStart() {
        RedirectBudget budget = new RedirectBudget(5);
        budget.startFrom("http://origin/");
        // 直接跳回起点也是环（起点在访问集）
        assertThat(budget.decide("http://mid/")).isEqualTo(Decision.FOLLOW);
        assertThat(budget.decide("http://origin/")).isEqualTo(Decision.LOOP_DETECTED);
        assertThat(budget.visitedCount()).isEqualTo(2); // 起点 + mid（环目标未入集）
    }

    @Test
    void selfRedirectShouldBeLoopImmediately() {
        RedirectBudget budget = new RedirectBudget(5);
        budget.startFrom("http://a/1");
        assertThat(budget.decide("http://a/1")).isEqualTo(Decision.LOOP_DETECTED); // 自指环
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new RedirectBudget(-1))
                .isInstanceOf(IllegalArgumentException.class);
        RedirectBudget budget = new RedirectBudget(3);
        assertThatThrownBy(() -> budget.startFrom(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> budget.startFrom(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> budget.decide(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> budget.decide(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
