package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1872 / T2946：突发信用——蓄水封顶、透支、枯竭计数、余量换算。 */
class BurstCreditAccountTest {

    /** 蓄水封顶：基准 2/ms 补 100ms → +200 钳容量 300。 */
    @Test
    void refillAccruesAtBaseRateAndCaps() {
        BurstCreditAccount account = new BurstCreditAccount(300, 2, 50, 0);
        account.refill(100);
        assertThat(account.stats().credits()).isEqualTo(250L);
        account.refill(1000); // +1800 但封顶 300
        assertThat(account.stats().credits()).isEqualTo(300L);
        assertThat(account.stats().fillRatio()).isEqualTo(1.0d);
    }

    /** 透支与枯竭：水位见底即拒（降速语义），枯竭计数一次。 */
    @Test
    void spendDrainsAndCountsExhaustion() {
        BurstCreditAccount account = new BurstCreditAccount(100, 1, 100, 0);
        assertThat(account.trySpend(60)).isTrue();
        assertThat(account.trySpend(40)).isTrue();  // 见底
        assertThat(account.stats().exhaustions()).isEqualTo(1L);
        assertThat(account.trySpend(1)).isFalse();  // 降速回归基准
        assertThat(account.stats().credits()).isZero();
        account.refill(10); // +10 回血
        assertThat(account.trySpend(5)).isTrue();
    }

    /** 突发余量换算：水位 250 / 基准 2 = 满水还能全速 125ms。 */
    @Test
    void burstHeadroomConvertsToMillis() {
        BurstCreditAccount account = new BurstCreditAccount(300, 2, 50, 0);
        account.refill(100);
        assertThat(account.stats().burstHeadroomMillis()).isEqualTo(125L);
    }

    /** 畸形入参 fail-fast：容量/速率 < 1、负初始、负 cost、时钟回拨。 */
    @Test
    void malformedUsageFailsFast() {
        assertThatThrownBy(() -> new BurstCreditAccount(0, 1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法信用账户参");
        assertThatThrownBy(() -> new BurstCreditAccount(100, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        BurstCreditAccount account = new BurstCreditAccount(100, 1, 10, 100);
        assertThatThrownBy(() -> account.trySpend(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.refill(50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时钟回拨");
    }
}
