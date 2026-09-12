package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 归档 PDB yml 装配测试（spec 726 / T1003–T1004 / impl 529）：capped probe
 * 计数语义——limit=min+1 一页 size>min 即放行（不数全量）；min=0 缺省关。
 */
class SessionAvailabilityFloorAssemblyTest {

    private static SessionIndexStore indexWith(int sessions) {
        InMemorySessionIndexStore store = new InMemorySessionIndexStore();
        for (int i = 0; i < sessions; i++) {
            store.upsert(new SessionInfo("s-" + i, "app", "agent",
                    SessionInfo.STATUS_ACTIVE, 1000L, 2000L + i, 1, java.util.Map.of()));
        }
        return store;
    }

    private static SessionAvailabilityFloor floor(SessionIndexStore index, int min) {
        // 与 BuzhouCoreAutoConfiguration.buzhouSessionArchiver 同口径的 capped probe
        return new SessionAvailabilityFloor(min, () -> index.list(
                new SessionIndexQuery(null, null, null, null, null, 0, min + 1, null)).size());
    }

    @Test
    void cappedProbeAllowsWhenAboveMin() {
        SessionAvailabilityFloor floor = floor(indexWith(10), 2);
        assertThat(floor.allowsArchive()).isTrue(); // probe 返回 3 > 2
    }

    @Test
    void cappedProbeDeniesWhenAtOrBelowMin() {
        SessionAvailabilityFloor floor = floor(indexWith(2), 2);
        assertThat(floor.allowsArchive()).isFalse(); // probe 返回 2 ≤ 2
        assertThat(floor(indexWith(0), 2).allowsArchive()).isFalse();
    }

    @Test
    void minZeroStillProtectsLastSession() {
        // min=0 时装配面不建 floor（bean 条件）——但语义本身：0 会话时拒绝（保护最后一个）
        assertThat(floor(indexWith(0), 0).allowsArchive()).isFalse();
        assertThat(floor(indexWith(1), 0).allowsArchive()).isTrue();
    }

    @Test
    void floorSemanticsUnchangedFromSpec704() {
        SessionAvailabilityFloor floor = new SessionAvailabilityFloor(3, () -> 5);
        assertThat(floor.minAvailable()).isEqualTo(3);
        assertThat(floor.allowsArchive()).isTrue();
    }
}
