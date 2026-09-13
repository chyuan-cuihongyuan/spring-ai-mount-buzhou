package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 837 / T1178：限流键热点回归——top 排序/额度累计精度/溢出桶/脏入参/空真。
 */
class RateLimitKeyHotspotTest {

    @Test
    void topSortsAndAccumulatesAmounts() {
        RateLimitKeyHotspot hotspot = new RateLimitKeyHotspot();
        hotspot.record("m1|RPM", 10, 100);
        hotspot.record("m1|RPM", 20, 200);
        hotspot.record("m2|RPM", 5, 300);
        hotspot.record("m2|TPM", 1, 400);

        var top = hotspot.top(10);
        assertThat(top.get(0).key()).isEqualTo("m1|RPM");
        assertThat(top.get(0).requests()).isEqualTo(2);
        assertThat(top.get(0).amountSum()).isCloseTo(30.0, within(1e-6)); // double 累计经毫伏账
        assertThat(top.get(0).lastSeenMillis()).isEqualTo(200);
        assertThat(top.get(1).key()).isEqualTo("m2|RPM");
        assertThat(hotspot.totalRequests()).isEqualTo(4);
        assertThat(hotspot.distinctKeys()).isEqualTo(3);
    }

    @Test
    void overflowBucketBeyondCap() {
        RateLimitKeyHotspot hotspot = new RateLimitKeyHotspot();
        for (int i = 0; i < RateLimitKeyHotspot.MAX_KEYS; i++) {
            hotspot.record("key" + i, 1, i);
        }
        hotspot.record("hot-new", 42, 999);

        assertThat(hotspot.distinctKeys()).isEqualTo(RateLimitKeyHotspot.MAX_KEYS + 1);
        var overflow = hotspot.top(RateLimitKeyHotspot.MAX_KEYS + 1).stream()
                .filter(k -> k.key().equals(RateLimitKeyHotspot.OVERFLOW))
                .findFirst().orElseThrow();
        assertThat(overflow.requests()).isEqualTo(1);
        assertThat(overflow.amountSum()).isCloseTo(42.0, within(1e-6));
    }

    @Test
    void dirtyInputsAndTopEdges() {
        RateLimitKeyHotspot hotspot = new RateLimitKeyHotspot();
        hotspot.record(null, 1, 0);
        hotspot.record("  ", 1, 0);
        hotspot.record("k", -5, 0);
        assertThat(hotspot.totalRequests()).isZero();
        assertThat(hotspot.top(5)).isEmpty();
        assertThat(hotspot.top(0)).isEmpty();
    }
}
