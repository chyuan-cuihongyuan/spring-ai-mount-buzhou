package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 138 §B / T463：轮次心跳红队——beat 刷新/未注册 beat no-op/clear 幂等；
 * 停滞检测只认候选且按 quiet 降序（最长停滞优先）；阈值边界（恰好等于不算）；
 * 在飞表封顶 fail-fast + clear 腾位；参数 fail-fast。借鉴：Temporal Activity
 * heartbeat。
 */
class TurnHeartbeatTest {

    private static final Instant T0 = Instant.parse("2026-08-29T00:00:00Z");

    @Test
    void beatUpdatesLastBeatAndUnknownBeatIsNoop() {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        heartbeat.beat("ghost", T0); // 未注册：no-op
        assertThat(heartbeat.lastBeat("ghost")).isNull();
        assertThat(heartbeat.inFlight()).isZero();

        heartbeat.register("s1", T0);
        heartbeat.beat("s1", T0.plusSeconds(3));
        assertThat(heartbeat.lastBeat("s1")).isEqualTo(T0.plusSeconds(3));

        heartbeat.clear("s1");
        heartbeat.clear("s1"); // 幂等
        assertThat(heartbeat.lastBeat("s1")).isNull();
    }

    @Test
    void stalledDetectsQuietCandidatesSortedByQuietDesc() {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        heartbeat.register("quiet-long", T0);
        heartbeat.register("quiet-short", T0.plusSeconds(30));
        heartbeat.register("fresh", T0.plusSeconds(59));
        heartbeat.register("not-candidate", T0); // 不在候选名单——不检

        List<TurnHeartbeat.Stalled> stalled = heartbeat.stalled(
                List.of("fresh", "quiet-long", "quiet-short"),
                Duration.ofSeconds(10), T0.plusSeconds(60));

        assertThat(stalled).hasSize(2);
        assertThat(stalled.get(0).sessionId()).isEqualTo("quiet-long");
        assertThat(stalled.get(0).quietFor()).isEqualTo(Duration.ofSeconds(60));
        assertThat(stalled.get(1).sessionId()).isEqualTo("quiet-short");
        assertThat(stalled.get(1).quietFor()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void exactlyAtThresholdIsNotStalled() {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        heartbeat.register("edge", T0);
        assertThat(heartbeat.stalled(List.of("edge"), Duration.ofSeconds(30),
                T0.plusSeconds(30))).isEmpty(); // 恰好等于：> 严格比较
        assertThat(heartbeat.stalled(List.of("edge"), Duration.ofSeconds(29),
                T0.plusSeconds(30))).hasSize(1);
    }

    @Test
    void registryBoundedFailsLoudAndClearFreesSlot() {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        for (int i = 0; i < TurnHeartbeat.MAX_IN_FLIGHT; i++) {
            heartbeat.register("s-" + i, T0);
        }
        assertThat(heartbeat.inFlight()).isEqualTo(TurnHeartbeat.MAX_IN_FLIGHT);
        assertThatThrownBy(() -> heartbeat.register("one-more", T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry full");
        assertThat(heartbeat.inFlight()).isEqualTo(TurnHeartbeat.MAX_IN_FLIGHT); // 拒插不留半态

        heartbeat.clear("s-0");
        heartbeat.register("one-more", T0);
        assertThat(heartbeat.lastBeat("one-more")).isEqualTo(T0);
    }

    @Test
    void argumentsValidatedFailFast() {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        assertThatThrownBy(() -> heartbeat.register(" ", T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heartbeat.register("s", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heartbeat.beat("s", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heartbeat.stalled(List.of(), Duration.ofSeconds(-1), T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heartbeat.stalled(List.of(), Duration.ofSeconds(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
