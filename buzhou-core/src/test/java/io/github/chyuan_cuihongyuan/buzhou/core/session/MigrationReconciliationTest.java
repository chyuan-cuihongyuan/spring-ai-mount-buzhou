package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 825 / T1152：迁移对账回归——全对/计数不符/轮次漂移/状态键缺失/
 * 重映射跳过 id 比对/明细封顶。
 */
class MigrationReconciliationTest {

    private static BuzhouMessage msg(String id, int turn) {
        return new BuzhouMessage(id, "s", turn, 0, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                "c", List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static SessionExport export(String sid, List<BuzhouMessage> messages, Map<String, StateEntry> state) {
        return new SessionExport(SessionExport.FORMAT, 1, sid, "app", "agent", 0L,
                messages, null, state, Map.of());
    }

    private static StateEntry entry(String value) {
        return new StateEntry("k", value, "producer", 1, null, Instant.now());
    }

    @Test
    void identicalExportsMatch() {
        SessionExport source = export("s1", List.of(msg("m1", 1), msg("m2", 1), msg("m3", 2)),
                Map.of("k", entry("v")));
        SessionExport target = export("s1", List.of(msg("m1", 1), msg("m2", 1), msg("m3", 2)),
                Map.of("k", entry("v")));

        var report = MigrationReconciliation.verify(source, target);
        assertThat(report.countsMatch()).isTrue();
        assertThat(report.mismatches()).isEmpty();
        assertThat(report.sourceMessages()).isEqualTo(3);
        assertThat(report.sourceTurns()).isEqualTo(2);
    }

    @Test
    void countMismatchDetected() {
        SessionExport source = export("s1", List.of(msg("m1", 1), msg("m2", 2)), Map.of());
        SessionExport target = export("s1", List.of(msg("m1", 1)), Map.of());

        var report = MigrationReconciliation.verify(source, target);
        assertThat(report.countsMatch()).isFalse();
        assertThat(report.mismatches().get(0)).contains("消息数不符").contains("源 2 vs 目标 1");
    }

    @Test
    void turnRangeDriftDetected() {
        // 同消息数但轮次边界不同
        SessionExport source = export("s1",
                List.of(msg("m1", 1), msg("m2", 3)), Map.of());
        SessionExport target = export("s1",
                List.of(msg("m1", 1), msg("m2x", 2)), Map.of());

        var report = MigrationReconciliation.verify(source, target);
        assertThat(report.mismatches()).anyMatch(m -> m.contains("轮次范围漂移"));
    }

    @Test
    void missingStateKeysListed() {
        Map<String, StateEntry> sourceState = new HashMap<>();
        sourceState.put("keep", entry("v"));
        sourceState.put("lost1", entry("v"));
        sourceState.put("lost2", entry("v"));
        SessionExport source = export("s1", List.of(msg("m1", 1)), sourceState);
        SessionExport target = export("s1", List.of(msg("m1", 1)), Map.of("keep", entry("v")));

        var report = MigrationReconciliation.verify(source, target);
        assertThat(report.mismatches()).anyMatch(m -> m.contains("状态键数不符") && m.contains("源 3 vs 目标 1"));
        assertThat(report.mismatches()).anyMatch(m -> m.contains("lost1") && m.contains("lost2"));
    }

    @Test
    void remappedIdsSkipBoundaryCheck() {
        // 目标 sessionId 重映射（keepIds=false）——首尾 id 漂移不报（重映射语义）
        SessionExport source = export("s1", List.of(msg("old-first", 1), msg("old-last", 2)), Map.of());
        SessionExport target = export("s1-remapped",
                List.of(msg("new-first", 1), msg("new-last", 2)), Map.of());

        var report = MigrationReconciliation.verify(source, target);
        assertThat(report.countsMatch()).isTrue();
        assertThat(report.mismatches()).isEmpty();
    }

    @Test
    void nullSafeEmptyExports() {
        SessionExport empty = export("s", List.of(), Map.of());
        var report = MigrationReconciliation.verify(empty, empty);
        assertThat(report.countsMatch()).isTrue();
        assertThat(report.sourceTurns()).isZero();
    }
}
