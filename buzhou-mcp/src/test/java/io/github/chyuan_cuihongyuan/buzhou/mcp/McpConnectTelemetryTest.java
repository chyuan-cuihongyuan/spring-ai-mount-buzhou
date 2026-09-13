package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 840 / T1182：建连遥测回归——成败计数/streak/近窗率/lastDuration/
 * worstFirst 排序/封顶/脏入参。
 */
class McpConnectTelemetryTest {

    @Test
    void countersStreakAndRate() {
        McpConnectTelemetry telemetry = new McpConnectTelemetry();
        telemetry.record("srv", true, 120);
        telemetry.record("srv", false, 5000);
        telemetry.record("srv", false, 5000);
        telemetry.record("srv", true, 90);

        McpConnectTelemetry.ServerTelemetry s = telemetry.stats("srv");
        assertThat(s.successes()).isEqualTo(2);
        assertThat(s.failures()).isEqualTo(2);
        assertThat(s.consecutiveFailures()).isZero();
        assertThat(s.lastDurationMillis()).isEqualTo(90);
        assertThat(s.recentSuccessRate()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void unknownDurationKeepsLastKnown() {
        McpConnectTelemetry telemetry = new McpConnectTelemetry();
        telemetry.record("srv", true, 100);
        telemetry.record("srv", true, -1); // 未知——保留 100
        assertThat(telemetry.stats("srv").lastDurationMillis()).isEqualTo(100);
    }

    @Test
    void worstFirstSortsByFailures() {
        McpConnectTelemetry telemetry = new McpConnectTelemetry();
        telemetry.record("healthy", true, 10);
        telemetry.record("flaky", false, 10);
        telemetry.record("flaky", false, 10);
        telemetry.record("stable", true, 10);

        var ranked = telemetry.worstFirst();
        assertThat(ranked.get(0).server()).isEqualTo("flaky");
        assertThat(ranked.get(0).failures()).isEqualTo(2);
    }

    @Test
    void serversCappedAndDirtyIgnored() {
        McpConnectTelemetry telemetry = new McpConnectTelemetry();
        for (int i = 0; i < McpConnectTelemetry.MAX_SERVERS + 2; i++) {
            telemetry.record("s" + i, true, 1);
        }
        assertThat(telemetry.truncated()).isTrue();
        assertThat(telemetry.stats("s0")).isNotNull();
        assertThat(telemetry.stats("s" + (McpConnectTelemetry.MAX_SERVERS + 1))).isNull();
        telemetry.record(null, true, 1);
        telemetry.record("  ", false, 1);
        assertThat(telemetry.stats("  ")).isNull();
    }
}
