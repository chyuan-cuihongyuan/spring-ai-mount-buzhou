package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 127 / T474：舱占用聚合回归——双实例心跳求和 / TTL 过期剔除 / Reporter
 * 快照双列（心跳和 + 本地实时）/ no-op 后端零影响 / 参数 fail-fast。
 */
class BulkheadClusterAggregationTest {

    @Test
    void twoInstancesAggregatePerAgent() {
        InMemoryBulkheadStateBackend backend = new InMemoryBulkheadStateBackend();
        backend.heartbeat(new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend
                .InstanceOccupancy("i-1", "alpha", 2, 5, Instant.now()), Duration.ofSeconds(30));
        backend.heartbeat(new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend
                .InstanceOccupancy("i-2", "alpha", 3, 5, Instant.now()), Duration.ofSeconds(30));
        backend.heartbeat(new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend
                .InstanceOccupancy("i-1", "beta", 1, 4, Instant.now()), Duration.ofSeconds(30));

        assertThat(backend.clusterOccupancy()).containsEntry("alpha", 5).containsEntry("beta", 1);
        assertThat(backend.clusterInstances()).containsEntry("alpha", 2).containsEntry("beta", 1);
    }

    @Test
    void expiredHeartbeatsDropFromAggregation() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-30T00:00:00Z"));
        InMemoryBulkheadStateBackend backend = new InMemoryBulkheadStateBackend(clock);
        backend.heartbeat(new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend
                .InstanceOccupancy("i-1", "alpha", 4, 5, clock.instant()), Duration.ofSeconds(10));

        clock.advanceSeconds(11);
        assertThat(backend.clusterOccupancy()).isEmpty();
        assertThat(backend.clusterInstances()).isEmpty();
    }

    @Test
    void sameInstanceOverwritesItsOwnBeat() {
        InMemoryBulkheadStateBackend backend = new InMemoryBulkheadStateBackend();
        Instant now = Instant.now();
        backend.heartbeat(new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend
                .InstanceOccupancy("i-1", "alpha", 4, 5, now), Duration.ofSeconds(30));
        backend.heartbeat(new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend
                .InstanceOccupancy("i-1", "alpha", 1, 5, now), Duration.ofSeconds(30));

        assertThat(backend.clusterOccupancy()).containsEntry("alpha", 1);
        assertThat(backend.clusterInstances()).containsEntry("alpha", 1);
    }

    @Test
    void reporterSnapshotMergesRemoteHeartbeatsAndLocalRealtime() {
        InMemoryBulkheadStateBackend backend = new InMemoryBulkheadStateBackend();
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("alpha", 5), Duration.ZERO);
        AgentBulkheadReporter reporter = new AgentBulkheadReporter(
                bulkhead, backend, "i-1", Duration.ofSeconds(30));

        // 远端实例心跳：alpha 在 i-2 占 3
        backend.heartbeat(new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend
                .InstanceOccupancy("i-2", "alpha", 3, 5, Instant.now()), Duration.ofSeconds(30));

        // 本地占 2 后发布心跳
        try (AgentBulkhead.Lease ignored = bulkhead.acquire("alpha");
             AgentBulkhead.Lease ignored2 = bulkhead.acquire("alpha")) {
            reporter.report();
            AgentBulkheadReporter.ClusterRow row = reporter.clusterSnapshot().get("alpha");
            assertThat(row).isNotNull();
            assertThat(row.clusterOccupied()).isEqualTo(5);   // 2（本地心跳）+ 3（远端）
            assertThat(row.instances()).isEqualTo(2);
            assertThat(row.localLimit()).isEqualTo(5);
            assertThat(row.localOccupied()).isEqualTo(2);     // 本地实时真值
        }
        // 释放后实时列归零（心跳列滞后一个窗——两列并列不合并，口径诚实）
        AgentBulkheadReporter.ClusterRow after = reporter.clusterSnapshot().get("alpha");
        assertThat(after.localOccupied()).isZero();
    }

    @Test
    void noopBackendKeepsZeroBehaviorAndParamsValidate() {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend noop =
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend() {
                };
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("alpha", 5), Duration.ZERO);
        AgentBulkheadReporter reporter = new AgentBulkheadReporter(
                bulkhead, noop, "i-1", Duration.ofSeconds(30));
        reporter.report();
        // no-op 后端：本地配置舱仍出现（实时列），心跳列为 0
        assertThat(reporter.clusterSnapshot())
                .containsKey("alpha")
                .extracting(map -> map.get("alpha").localOccupied())
                .isEqualTo(0);

        assertThatThrownBy(() -> new AgentBulkheadReporter(bulkhead, noop, " ", Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InMemoryBulkheadStateBackend().heartbeat(null, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 可推进时钟（TTL 过期断言用）。 */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
