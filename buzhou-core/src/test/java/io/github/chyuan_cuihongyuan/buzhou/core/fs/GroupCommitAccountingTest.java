package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1846 / T2894：组提交账——摊薄倍数、省刷数、净省与节省率。 */
class GroupCommitAccountingTest {

    /** 10 写 2 刷：摊薄 5 倍、省 8 刷、净省与比率。 */
    @Test
    void shouldAccountAmortizationAndSavings() {
        GroupCommitAccounting.Account a =
                new GroupCommitAccounting.Account(10, 2, 1000, 1500);
        assertThat(a.amortizationRatio()).isEqualTo(5.0d);
        assertThat(a.savedFlushes()).isEqualTo(8);
        // 净省 = 10×1000 − 2×1500 = 7000
        assertThat(a.savingsNanos()).isEqualTo(7000L);
        assertThat(a.savingsRatio()).isEqualTo(0.7d);
    }

    /** 合并不划算面：批刷成本高到净省为负——账面诚实可判。 */
    @Test
    void unprofitableMergeReadsNegative() {
        GroupCommitAccounting.Account a =
                new GroupCommitAccounting.Account(2, 1, 1000, 5000);
        assertThat(a.savingsNanos()).isEqualTo(-3000L);
        assertThat(a.savingsRatio()).isEqualTo(-1.5d);
    }

    /** 零写哨兵：摊薄与节省率 -1、省刷 0。 */
    @Test
    void zeroWritesYieldSentinels() {
        GroupCommitAccounting.Account a =
                new GroupCommitAccounting.Account(0, 0, 100, 150);
        assertThat(a.amortizationRatio()).isEqualTo(-1d);
        assertThat(a.savedFlushes()).isZero();
        assertThat(a.savingsRatio()).isEqualTo(-1d);
    }

    /** 畸形账 fail-fast：刷多于写、有写零刷、负数。 */
    @Test
    void malformedAccountFailsFast() {
        assertThatThrownBy(() -> new GroupCommitAccounting.Account(2, 3, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法组提交账");
        assertThatThrownBy(() -> new GroupCommitAccounting.Account(5, 0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupCommitAccounting.Account(-1, 0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupCommitAccounting.Account(1, 1, -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
