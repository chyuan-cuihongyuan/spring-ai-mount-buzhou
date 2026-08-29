package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 214 §B / T578：双小方法红队——TurnHeartbeat.clearAll 全清幂等（重启/
 * 复位语义）；VirtualKeys.usageAll 与 topUsage 同序全量。
 */
class ClearAllAndUsageAllTest {

    @Test
    void heartbeatClearAllResetsRegistry() {
        var heartbeat = new io.github.chyuan_cuihongyuan.buzhou.core.runaway.TurnHeartbeat();
        Instant t0 = Instant.parse("2026-08-29T00:00:00Z");
        heartbeat.register("a", t0);
        heartbeat.register("b", t0);
        assertThat(heartbeat.inFlight()).isEqualTo(2);

        heartbeat.clearAll();
        heartbeat.clearAll(); // 幂等
        assertThat(heartbeat.inFlight()).isZero();
        assertThat(heartbeat.lastBeat("a")).isNull();
    }

    @Test
    void usageAllMatchesTopUsageOrder() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("k1", 100);
        keys.register("k2", 100);
        keys.trySpend("k1", 40);
        keys.trySpend("k2", 10);

        List<VirtualKeys.KeyUsage> all = keys.usageAll();
        assertThat(all).hasSize(2);
        assertThat(all).isEqualTo(keys.topUsage(10)); // 同序全量
        assertThat(all.get(0).key()).isEqualTo("k1");
    }
}
