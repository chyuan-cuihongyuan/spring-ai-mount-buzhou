package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 830 / T1162：树形健康回归——深度/补位/满树判定的七点精确账。
 */
class AuditTreeHealthReadoutTest {

    @Test
    void boundaryLedger() {
        // 0：空树
        var empty = AuditTreeHealthReadout.analyze(0);
        assertThat(empty.depth()).isZero();
        assertThat(empty.perfectTree()).isFalse();
        assertThat(empty.paddingLeaves()).isEqualTo(1);

        // 1：深度 0 满树
        var one = AuditTreeHealthReadout.analyze(1);
        assertThat(one.depth()).isZero();
        assertThat(one.perfectTree()).isTrue();
        assertThat(one.paddingLeaves()).isZero();

        // 2：深度 1 满树
        assertThat(AuditTreeHealthReadout.analyze(2).depth()).isEqualTo(1);
        assertThat(AuditTreeHealthReadout.analyze(2).perfectTree()).isTrue();

        // 3：深度 2、补位 1、非满
        var three = AuditTreeHealthReadout.analyze(3);
        assertThat(three.depth()).isEqualTo(2);
        assertThat(three.nextPowerOfTwo()).isEqualTo(4);
        assertThat(three.paddingLeaves()).isEqualTo(1);
        assertThat(three.perfectTree()).isFalse();

        // 4：深度 2 满树；5：深度 3 补位 3；8：深度 3 满树；9：深度 4 补位 7
        assertThat(AuditTreeHealthReadout.analyze(4).perfectTree()).isTrue();
        var five = AuditTreeHealthReadout.analyze(5);
        assertThat(five.depth()).isEqualTo(3);
        assertThat(five.paddingLeaves()).isEqualTo(3);
        assertThat(AuditTreeHealthReadout.analyze(8).perfectTree()).isTrue();
        var nine = AuditTreeHealthReadout.analyze(9);
        assertThat(nine.depth()).isEqualTo(4);
        assertThat(nine.paddingLeaves()).isEqualTo(7);
    }

    @Test
    void negativeClampedToEmpty() {
        var health = AuditTreeHealthReadout.analyze(-7);
        assertThat(health.leafCount()).isZero();
        assertThat(health.depth()).isZero();
    }

    @Test
    void largeTreeShape() {
        var health = AuditTreeHealthReadout.analyze(1000);
        assertThat(health.depth()).isEqualTo(10); // 2^10=1024 ≥ 1000
        assertThat(health.nextPowerOfTwo()).isEqualTo(1024);
        assertThat(health.paddingLeaves()).isEqualTo(24);
        assertThat(health.perfectTree()).isFalse();
    }
}
