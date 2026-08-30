package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 196 §B / T558：双小方法红队——ErrorSignatures.top(kind,n) 只看该类
 * 错误族（前缀过滤 + 同序）+ 空白 kind fail-fast；TurnHeartbeat.stalledSince
 * 单查（超阈返回时长/未超或未注册 null）。
 */
class TopByKindAndStalledSinceTest {

    @AfterEach
    void cleanup() {
        ErrorSignatures.install(null);
    }

    @Test
    void topByKindFiltersPrefix() {
        ErrorSignatures signatures = ErrorSignatures.create();
        signatures.record("model", "timeout");
        signatures.record("model", "timeout");
        signatures.record("tool", "io refused");
        ErrorSignatures.install(signatures);

        assertThat(signatures.top("model", 5))
                .extracting(Map.Entry::getKey)
                .containsExactly("model:timeout");
        assertThat(signatures.top("tool", 5))
                .extracting(Map.Entry::getKey)
                .containsExactly("tool:io refused");
        assertThat(signatures.top("store", 5)).isEmpty();
        assertThatThrownBy(() -> signatures.top(" ", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stalledSinceSingleQuery() {
        io.github.chyuan_cuihongyuan.buzhou.core.runaway.TurnHeartbeat heartbeat =
                new io.github.chyuan_cuihongyuan.buzhou.core.runaway.TurnHeartbeat();
        Instant t0 = Instant.parse("2026-08-29T00:00:00Z");
        heartbeat.register("s1", t0);

        assertThat(heartbeat.stalledSince("s1", Duration.ofSeconds(10),
                t0.plusSeconds(30))).isEqualTo(Duration.ofSeconds(30));
        assertThat(heartbeat.stalledSince("s1", Duration.ofSeconds(60),
                t0.plusSeconds(30))).isNull(); // 未超阈
        assertThat(heartbeat.stalledSince("ghost", Duration.ofSeconds(1), t0))
                .isNull(); // 未注册
    }
}
