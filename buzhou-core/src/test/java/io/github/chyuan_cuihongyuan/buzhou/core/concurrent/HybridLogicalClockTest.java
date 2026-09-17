package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.HybridLogicalClock.Hlc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 3007 / T5016：HLC 合同——同墙计数严格递增、墙跳清零仍单调、
 * 远端超前吸收、三路同墙 max+1、远端落后无感、物理回拨不倒、
 * 因果传递（tick→observe→tick 严格小于）、双钟互投单调、序比较。
 */
class HybridLogicalClockTest {

    private static final class MutableClock implements LongSupplier {
        private final AtomicLong now = new AtomicLong();

        MutableClock(long initial) {
            now.set(initial);
        }

        void set(long value) {
            now.set(value);
        }

        @Override
        public long getAsLong() {
            return now.get();
        }
    }

    @Test
    void sameWallShouldTickCounterStrictly() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock clock = new HybridLogicalClock(pt);
        Hlc previous = null;
        for (int i = 0; i < 100; i++) {
            Hlc ts = clock.tick();
            assertThat(ts.wall()).isEqualTo(100);
            if (previous != null) {
                assertThat(ts.compareTo(previous)).isPositive();
            }
            previous = ts;
        }
        assertThat(clock.peek().counter()).isEqualTo(99);
    }

    @Test
    void wallAdvanceShouldResetCounterButStayMonotonic() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock clock = new HybridLogicalClock(pt);
        Hlc a = clock.tick();
        assertThat(a).isEqualTo(new Hlc(100, 0));
        Hlc b = clock.tick();
        assertThat(b).isEqualTo(new Hlc(100, 1));
        pt.set(200);
        Hlc c = clock.tick();
        assertThat(c).isEqualTo(new Hlc(200, 0));
        assertThat(c.compareTo(b)).isPositive();
    }

    @Test
    void remoteAheadShouldBeAbsorbedAsNewBaseline() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock clock = new HybridLogicalClock(pt);
        clock.tick();
        Hlc merged = clock.observe(new Hlc(500, 3));
        assertThat(merged).isEqualTo(new Hlc(500, 4));
        // 后续本地事件贴新基线继续单调
        assertThat(clock.tick()).isEqualTo(new Hlc(500, 5));
    }

    @Test
    void sameWallRemoteShouldTakeMaxCounterPlusOne() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock clock = new HybridLogicalClock(pt);
        clock.tick();
        clock.tick();
        clock.tick();  // 本地 (100, 2)
        Hlc merged = clock.observe(new Hlc(100, 7));
        assertThat(merged).isEqualTo(new Hlc(100, 8));
        // 反向：本地计数更高同样取 max
        Hlc mergedAgain = clock.observe(new Hlc(100, 2));
        assertThat(mergedAgain).isEqualTo(new Hlc(100, 9));
    }

    @Test
    void remoteBehindShouldBeIgnoredGracefully() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock clock = new HybridLogicalClock(pt);
        clock.tick();
        clock.tick();  // (100, 1)
        Hlc merged = clock.observe(new Hlc(50, 99));
        assertThat(merged).isEqualTo(new Hlc(100, 2));
    }

    @Test
    void physicalClockGoingBackwardsShouldNotRegress() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock clock = new HybridLogicalClock(pt);
        Hlc a = clock.tick();
        pt.set(40);  // 回拨
        Hlc b = clock.tick();
        assertThat(b).isEqualTo(new Hlc(a.wall(), a.counter() + 1));
        assertThat(b.compareTo(a)).isPositive();
    }

    @Test
    void causalityShouldTransferAcrossClocks() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock left = new HybridLogicalClock(pt);
        HybridLogicalClock right = new HybridLogicalClock(pt);
        Hlc cause = left.tick();
        Hlc observed = right.observe(cause);
        Hlc effect = right.tick();
        assertThat(cause.compareTo(observed)).isNegative();
        assertThat(observed.compareTo(effect)).isNegative();
    }

    @Test
    void interleavedExchangeShouldNeverGoBackwards() {
        MutableClock pt = new MutableClock(100);
        HybridLogicalClock a = new HybridLogicalClock(pt);
        HybridLogicalClock b = new HybridLogicalClock(pt);
        Hlc lastA = a.tick();
        Hlc lastB = b.tick();
        for (int i = 0; i < 50; i++) {
            Hlc aToB = a.tick();
            lastA = a.tick();
            lastB = b.observe(aToB);
            assertThat(lastB.compareTo(aToB)).isPositive();
            assertThat(lastA.compareTo(aToB)).isPositive();
            Hlc bToA = b.tick();
            lastB = b.tick();
            lastA = a.observe(bToA);
            assertThat(lastA.compareTo(bToA)).isPositive();
        }
    }

    @Test
    void hlcOrderShouldCompareWallFirstThenCounter() {
        assertThat(new Hlc(100, 5).compareTo(new Hlc(101, 0))).isNegative();
        assertThat(new Hlc(100, 5).compareTo(new Hlc(100, 6))).isNegative();
        assertThat(new Hlc(100, 5).compareTo(new Hlc(100, 5))).isZero();
        assertThat(new Hlc(100, 6).compareTo(new Hlc(100, 5))).isPositive();
    }
}
