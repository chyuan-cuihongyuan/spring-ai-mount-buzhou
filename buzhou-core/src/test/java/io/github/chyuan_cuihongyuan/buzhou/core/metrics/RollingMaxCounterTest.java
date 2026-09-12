package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 时间桶滚动 max 测试（spec 708 / T967–T968 / impl 511）：窗内保留、过窗归零、
 * 新样本覆盖、桶翻转重置、聚合接入（windowedMax 与生命周期 max 并存）。
 */
class RollingMaxCounterTest {

    @Test
    void keepsMaxWithinWindowAndDecaysAfter() {
        AtomicLong clock = new AtomicLong(1_000_000);
        // 4 桶 × 100ms = 400ms 窗
        RollingMaxCounter counter = new RollingMaxCounter(4, 100, clock::get);

        counter.record(500);
        counter.record(300);
        assertThat(counter.max()).isEqualTo(500); // 窗内保留峰值

        clock.addAndGet(150); // 仍在窗内（峰值桶未过期）
        assertThat(counter.max()).isEqualTo(500);

        clock.addAndGet(300); // 全部桶过期
        assertThat(counter.max()).isZero(); // 诚实口径：窗口内无样本 = 0

        counter.record(100); // 新低样本
        assertThat(counter.max()).isEqualTo(100); // 历史峰值不再污染
    }

    @Test
    void bucketFlipAgesOutStaleSlot() {
        AtomicLong clock = new AtomicLong(1_000_000);
        RollingMaxCounter counter = new RollingMaxCounter(2, 100, clock::get); // 200ms 窗

        counter.record(900);           // t=0 桶 idx0
        clock.addAndGet(100);          // t=100 桶 idx1
        counter.record(100);
        assertThat(counter.max()).isEqualTo(900); // 900 距今 100ms < 200ms 窗——仍在窗内

        clock.addAndGet(100);          // t=200——900 出窗；idx0 恰被 gen 环复用
        assertThat(counter.max()).isEqualTo(100); // 900 出窗，100 仍在

        clock.addAndGet(50);           // t=250：新低样本后旧峰值不回魂
        counter.record(50);
        counter.record(200);
        assertThat(counter.max()).isEqualTo(200);
        clock.addAndGet(250);          // 全部出窗
        assertThat(counter.max()).isZero();
    }

    @Test
    void sameSlotMaxUpdatesMonotonically() {
        AtomicLong clock = new AtomicLong(1_000_000);
        RollingMaxCounter counter = new RollingMaxCounter(4, 100, clock::get);

        counter.record(10);
        counter.record(70);
        counter.record(30); // 同代降值不覆盖
        assertThat(counter.max()).isEqualTo(70);
    }

    @Test
    void invalidConstructionRejected() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new RollingMaxCounter(0, 100, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new RollingMaxCounter(4, 0, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
