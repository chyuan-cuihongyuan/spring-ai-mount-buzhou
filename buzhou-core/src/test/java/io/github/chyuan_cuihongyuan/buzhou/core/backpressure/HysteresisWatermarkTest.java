package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2036 / T3174：迟滞水位合同——超 HIGH 停、泄到 ≤ LOW 才恢复、
 * 中间保持区不翻转（迟滞防抖）、泄出钳 0、翻转计数、畸形 fail-fast。
 */
class HysteresisWatermarkTest {

    @Test
    void exceedingHighShouldStopWrites() {
        HysteresisWatermark mark = new HysteresisWatermark(10, 100);
        mark.onWrite(100); // 恰 HIGH（不超）
        assertThat(mark.isWritable()).isTrue();
        mark.onWrite(1); // 101 > 100
        assertThat(mark.isWritable()).isFalse();
        assertThat(mark.toggleCount()).isEqualTo(1L);
    }

    @Test
    void drainingToLowShouldResume() {
        HysteresisWatermark mark = new HysteresisWatermark(10, 100);
        mark.onWrite(150); // 停
        mark.onDrain(40);  // 110——未到 LOW，保持停
        assertThat(mark.isWritable()).isFalse();
        mark.onDrain(100); // 钳到 0 ≤ 10——恢复
        assertThat(mark.isWritable()).isTrue();
        assertThat(mark.toggleCount()).isEqualTo(2L); // 停+续恰两次
    }

    @Test
    void holdBandShouldNotFlipState() {
        // 迟滞核心：HIGH 与 LOW 之间（10,100] 是保持区——不翻转
        HysteresisWatermark mark = new HysteresisWatermark(10, 100);
        mark.onWrite(60); // 中间带，仍可写
        assertThat(mark.isWritable()).isTrue();
        mark.onWrite(100); // 160 > 100 停
        mark.onDrain(80); // 80 ∈ (10,100] 保持停
        assertThat(mark.isWritable()).isFalse();
        mark.onWrite(5); // 85 仍停写态（已停后写入只是记账）
        assertThat(mark.isWritable()).isFalse();
        mark.onDrain(80); // 5 ≤ 10 恢复
        assertThat(mark.isWritable()).isTrue();
        assertThat(mark.toggleCount()).isEqualTo(2L); // 全程恰两次翻转——无抖动
    }

    @Test
    void boundaryAtExactlyLowShouldResume() {
        HysteresisWatermark mark = new HysteresisWatermark(10, 100);
        mark.onWrite(120); // 停
        mark.onDrain(110); // 恰 10 = LOW → 恢复（≤ 语义）
        assertThat(mark.isWritable()).isTrue();
    }

    @Test
    void drainBelowZeroShouldClamp() {
        HysteresisWatermark mark = new HysteresisWatermark(10, 100);
        mark.onWrite(50);
        mark.onDrain(999); // 超泄
        assertThat(mark.pendingBytes()).isZero(); // 钳 0
        assertThat(mark.isWritable()).isTrue();
    }

    @Test
    void pendingShouldTrackAccumulation() {
        HysteresisWatermark mark = new HysteresisWatermark(1000, 10_000);
        mark.onWrite(300);
        mark.onWrite(200);
        mark.onDrain(100);
        assertThat(mark.pendingBytes()).isEqualTo(400L);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new HysteresisWatermark(-1, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HysteresisWatermark(100, 100)) // 重合=单阈值抖动门
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HysteresisWatermark(200, 100)) // 倒置
                .isInstanceOf(IllegalArgumentException.class);
        HysteresisWatermark mark = new HysteresisWatermark(10, 100);
        assertThatThrownBy(() -> mark.onWrite(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mark.onDrain(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
