package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1719 / T2640：HookErrorDistribution 直测——指纹分组/降序/并桶。
 */
class HookErrorDistributionTest {

    @Test
    void groupsByHookAndExceptionType() {
        var dist = new HookErrorDistribution();
        dist.record("auth", new IllegalStateException("x"));
        dist.record("auth", new IllegalStateException("y"));
        dist.record("auth", new NullPointerException("z"));
        dist.record("log", new IllegalStateException("w"));
        assertThat(dist.total()).isEqualTo(4);
        var census = dist.census();
        assertThat(census.get("auth:IllegalStateException")).isEqualTo(2L);
        assertThat(census.get("auth:NullPointerException")).isEqualTo(1L);
        assertThat(census.get("log:IllegalStateException")).isEqualTo(1L);
    }

    @Test
    void censusIsSortedDescending() {
        var dist = new HookErrorDistribution();
        dist.record("a", new Exception());
        dist.record("b", new Exception());
        dist.record("b", new Exception());
        var entries = dist.census().entrySet().stream().toList();
        assertThat(entries.get(0).getKey()).isEqualTo("b:Exception");
        assertThat(entries.get(0).getValue()).isEqualTo(2L);
    }

    @Test
    void overflowBucketsBeyondCapacity() {
        var dist = new HookErrorDistribution(2);
        dist.record("a", new Exception());
        dist.record("b", new Exception());
        dist.record("c", new Exception());
        assertThat(dist.census()).containsKey("_overflow_");
        assertThat(dist.census()).doesNotContainKey("c:Exception");
        assertThat(dist.total()).isEqualTo(3);
    }

    @Test
    void blankHookNameGoesAnonymous() {
        var dist = new HookErrorDistribution();
        dist.record(" ", new Exception("e"));
        assertThat(dist.census()).containsKey("_anonymous_:Exception");
    }
}
