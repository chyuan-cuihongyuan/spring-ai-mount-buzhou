package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 2062 / T3226：回绕序号合同——普通序正确、回绕点正确（朴素比
 * 较反转处）、安全半域界、不可分诚实、等值口径、距离折算。
 */
class SequenceOrderTest {

    @Test
    void ordinaryOrderingShouldBeCorrect() {
        assertThat(SequenceOrder.isBefore(10, 20)).isTrue();
        assertThat(SequenceOrder.isAfter(20, 10)).isTrue();
        assertThat(SequenceOrder.isBefore(20, 10)).isFalse();
    }

    @Test
    void wraparoundPointShouldStayCorrect() {
        // 回绕点：MAX → MIN——朴素 a<b 在此反转（MIN < MAX 判「前」）
        long last = Long.MAX_VALUE;
        long first = Long.MIN_VALUE;
        // 朴素比较的病证：first < last 为 true——新序号 MIN 被判「在 MAX 前」
        assertThat(first < last).isTrue();
        assertThat(SequenceOrder.isBefore(last, first)).isTrue(); // 回绕安全：last 在前
        assertThat(SequenceOrder.isAfter(first, last)).isTrue();
        // 回绕邻域小步进
        assertThat(SequenceOrder.isBefore(-5, 5)).isTrue();
        assertThat(SequenceOrder.isBefore(Long.MAX_VALUE - 1, Long.MIN_VALUE + 1)).isTrue();
    }

    @Test
    void halfRangeBoundaryShouldBeSafe() {
        long a = 0;
        // 差恰在安全域内（< 2⁶²）
        assertThat(SequenceOrder.isBefore(a, SequenceOrder.SAFE_HALF_RANGE - 1)).isTrue();
        // 超半环——不可分（诚实）
        assertThat(SequenceOrder.compare(a, SequenceOrder.SAFE_HALF_RANGE + 1))
                .isEqualTo(SequenceOrder.Order.INCOMPARABLE);
        assertThat(SequenceOrder.compare(a, Long.MAX_VALUE / 2 + 100))
                .isEqualTo(SequenceOrder.Order.INCOMPARABLE);
    }

    @Test
    void equalValuesShouldBeIncomparable() {
        assertThat(SequenceOrder.compare(7, 7)).isEqualTo(SequenceOrder.Order.INCOMPARABLE);
    }

    @Test
    void forwardDistanceShouldFoldIntoNeighborhood() {
        assertThat(SequenceOrder.forwardDistance(10, 25)).isEqualTo(15L);
        // 回绕邻域：MAX → MIN 距离 1
        assertThat(SequenceOrder.forwardDistance(Long.MAX_VALUE, Long.MIN_VALUE)).isEqualTo(1L);
        // 不可分域哨兵
        assertThat(SequenceOrder.forwardDistance(0, Long.MAX_VALUE))
                .isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void negativeWrapShouldAlsoWork() {
        // 反向回绕邻域：MIN → MAX 是「回退」
        assertThat(SequenceOrder.isBefore(Long.MIN_VALUE, Long.MAX_VALUE)).isFalse();
        assertThat(SequenceOrder.isAfter(Long.MIN_VALUE, Long.MAX_VALUE)).isTrue();
    }
}
