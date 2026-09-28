package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6047：PowerOfTwoChoices 合同——二择一放较轻。
 * 两桶严格放轻者（并列取小下标）；长尾压平（maxLoad 远低于
 * 均值多倍）；守恒与确定性；fail-fast。
 */
class PowerOfTwoChoicesTest {

    @Test
    void twoBinsAlwaysPickLighter() {
        PowerOfTwoChoices p2c = new PowerOfTwoChoices(2, 47L);
        p2c.place();
        long afterFirst0 = p2c.loadOf(0);
        long afterFirst1 = p2c.loadOf(1);
        long heavy = Math.max(afterFirst0, afterFirst1);
        for (int i = 0; i < 200; i++) {
            p2c.place();
            long l0 = p2c.loadOf(0);
            long l1 = p2c.loadOf(1);
            assertThat(Math.max(l0, l1) - Math.min(l0, l1))
                    .as("两桶差距始终 ≤1（每次都放轻者）").isLessThanOrEqualTo(1);
        }
        assertThat(heavy).isLessThanOrEqualTo(2);
    }

    @Test
    void longTailFlattenedAtMillionScale() {
        PowerOfTwoChoices p2c = new PowerOfTwoChoices(1 << 20, 6047L);
        int bins = p2c.binCount();
        int balls = 1_000_000;
        for (int i = 0; i < balls; i++) {
            p2c.place();
        }
        assertThat(p2c.placed()).isEqualTo(balls);
        assertThat(p2c.maxLoad())
                .as("二择一最大装载 ≤4（单次随机同规模典型 ≥6——非对称性显形；种子定死可复现）")
                .isLessThanOrEqualTo(4);
        long total = 0;
        for (int b = 0; b < bins; b++) {
            total += p2c.loadOf(b);
        }
        assertThat(total).isEqualTo(balls);
    }

    @Test
    void sameSeedSamePlacementSequence() {
        PowerOfTwoChoices a = new PowerOfTwoChoices(16, 99L);
        PowerOfTwoChoices b = new PowerOfTwoChoices(16, 99L);
        for (int i = 0; i < 200; i++) {
            assertThat(b.place()).isEqualTo(a.place());
        }
        assertThat(a.maxLoad()).isEqualTo(b.maxLoad());
    }

    @Test
    void singleBinAndFailFastContract() {
        PowerOfTwoChoices one = new PowerOfTwoChoices(1, 1L);
        for (int i = 0; i < 10; i++) {
            assertThat(one.place()).isZero();
        }
        assertThat(one.maxLoad()).isEqualTo(10);
        assertThatThrownBy(() -> new PowerOfTwoChoices(0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PowerOfTwoChoices(-3, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> one.loadOf(1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> one.loadOf(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
