package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1066 / impl 818：Deno 沙箱探测读面——重探测成功、TTL 缓存命中、
 * launcher 异常置不可用、双守恒恒等式、resetForTest 归零。
 */
class DenoProbeStatsTest {

    @BeforeEach
    void reset() {
        DenoSandbox.resetForTest();
    }

    @Test
    void successfulProbeCountsBuckets() {
        DenoSandbox sandbox = DenoSandbox.builder(
                (argv, env, workDir, timeout) -> successResult()).build();

        assertThat(sandbox.available()).isTrue();

        DenoSandbox.DenoProbeStats stats = DenoSandbox.stats();
        assertThat(stats.availableCalls()).isEqualTo(1);
        assertThat(stats.probes()).isEqualTo(1);
        assertThat(stats.probeSuccesses()).isEqualTo(1);
        assertThat(stats.probeUnavailables()).isZero();
    }

    @Test
    void ttlCacheHitSkipsProbe() {
        DenoSandbox sandbox = DenoSandbox.builder(
                (argv, env, workDir, timeout) -> successResult()).build();
        sandbox.available(); // 首次重探测
        sandbox.available(); // TTL 内 → 缓存命中

        DenoSandbox.DenoProbeStats stats = DenoSandbox.stats();
        assertThat(stats.availableCalls()).isEqualTo(2);
        assertThat(stats.probeCacheHits()).isEqualTo(1);
        assertThat(stats.probes()).isEqualTo(1);
    }

    @Test
    void launcherFailureCountsUnavailable() {
        DenoSandbox sandbox = DenoSandbox.builder(
                (argv, env, workDir, timeout) -> {
                    throw new IllegalStateException("no deno binary");
                }).build();

        assertThat(sandbox.available()).isFalse();

        DenoSandbox.DenoProbeStats stats = DenoSandbox.stats();
        assertThat(stats.probeUnavailables()).isEqualTo(1);
        assertThat(stats.probeSuccesses()).isZero();
    }

    @Test
    void dualConservationIdentitiesHold() {
        DenoSandbox ok = DenoSandbox.builder(
                (argv, env, workDir, timeout) -> successResult()).build();
        ok.available();
        ok.available(); // 缓存命中

        DenoSandbox bad = DenoSandbox.builder(
                (argv, env, workDir, timeout) -> {
                    throw new IllegalStateException("x");
                }).build();
        bad.available();
        bad.available(); // 失败也写缓存 → 不可用缓存命中

        DenoSandbox.DenoProbeStats stats = DenoSandbox.stats();
        assertThat(stats.availableCalls()).isEqualTo(stats.probeCacheHits() + stats.probes());
        assertThat(stats.probes())
                .isEqualTo(stats.probeSuccesses() + stats.probeUnavailables());
    }

    @Test
    void resetForTestZeroesCounters() {
        DenoSandbox sandbox = DenoSandbox.builder(
                (argv, env, workDir, timeout) -> successResult()).build();
        sandbox.available();
        assertThat(DenoSandbox.stats().availableCalls()).isEqualTo(1);

        DenoSandbox.resetForTest();

        DenoSandbox.DenoProbeStats stats = DenoSandbox.stats();
        assertThat(stats.availableCalls()).isZero();
        assertThat(stats.probes()).isZero();
        assertThat(stats.probeSuccesses()).isZero();
        assertThat(stats.probeUnavailables()).isZero();
    }

    private static CommandSandbox.CommandResult successResult() {
        return new CommandSandbox.CommandResult(0, "Deno 1.0", "", false);
    }
}
