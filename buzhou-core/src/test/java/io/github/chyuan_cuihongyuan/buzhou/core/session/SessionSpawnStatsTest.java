package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1432 / T2166：会话 spawn 统计——成功/冲突/抢占三路计数、守恒式
 * attempts=successes+collisions、活跃峰值水位、reset；静态面前后归零防串扰。
 */
class SessionSpawnStatsTest {

    private io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime runtime;

    @BeforeEach
    void reset() {
        SessionSpawnStats.resetForTest();
        runtime = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(),
                new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                        null, List.of()));
    }

    @AfterEach
    void resetAfter() {
        SessionSpawnStats.resetForTest();
    }

    @Test
    void successfulSpawnsCountedWithPeakWatermark() {
        try (var s1 = runtime.spawn("app", "ag", "s-ss-1");
             var s2 = runtime.spawn("app", "ag", "s-ss-2")) {
            var st = SessionSpawnStats.stats();
            assertThat(st.attempts()).isEqualTo(2);
            assertThat(st.successes()).isEqualTo(2);
            assertThat(st.collisions()).isZero();
            assertThat(st.attempts()).isEqualTo(st.successes() + st.collisions());
            // spawn 时点观测的活跃峰值 ≥ 2（同 runtime 两会话并存）
            assertThat(st.activePeak()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void duplicateIdWithoutStealCountsCollision() {
        try (var s1 = runtime.spawn("app", "ag", "s-ss-dup")) {
            assertThatThrownBy(() -> runtime.spawn("app", "ag", "s-ss-dup"))
                    .isInstanceOf(SessionAlreadyActiveException.class);
        }
        var st = SessionSpawnStats.stats();
        assertThat(st.attempts()).isEqualTo(2);
        assertThat(st.collisions()).isEqualTo(1);
        assertThat(st.successes()).isEqualTo(1);
        assertThat(st.attempts()).isEqualTo(st.successes() + st.collisions());
    }

    @Test
    void stealPathCounted() {
        try (var s1 = runtime.spawn("app", "ag", "s-ss-st")) {
            // steal 抢占同 id 会话（SessionAlreadyActiveException 不抛）
            try (var s2 = runtime.spawn("app", "ag", "s-ss-st",
                    SpawnOptions.withSteal())) {
                var st = SessionSpawnStats.stats();
                assertThat(st.attempts()).isEqualTo(2);
                assertThat(st.steals()).isEqualTo(1);
                // steal 成功计入 successes（spawn 完成形态）
                assertThat(st.successes()).isEqualTo(2);
            }
        }
    }
}
