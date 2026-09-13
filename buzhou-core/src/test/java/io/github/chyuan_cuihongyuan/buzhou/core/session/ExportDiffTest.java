package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-665 / spec 912：会话导出 diff——相同导出 identical、标量差异、消息三桶、
 * state/extensions 键值差异、跨会话 fail-fast、时戳不参与。
 */
class ExportDiffTest {

    private static BuzhouMessage msg(String id, Role role, String content) {
        return new BuzhouMessage(id, "s1", 1, 1, role, content,
                null, null, null, null, Map.of(), Instant.EPOCH);
    }

    @Test
    void identicalExportsReportTrue() {
        SessionExport a = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", Role.USER, "hi")), null, Map.of());
        SessionExport b = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", Role.USER, "hi")), null, Map.of());
        SessionExportDiff.DiffReport report = SessionExportDiff.between(a, b);
        assertThat(report.identical()).isTrue();
        assertThat(report.fieldDiffs()).isEmpty();
        assertThat(report.messageDiffs()).isEmpty();
    }

    @Test
    void scalarFieldAndTimestampExcluded() {
        // exportedAtEpochMs 不同（of 内部取当前时钟，两次构造天然不同）但其余同 → identical
        SessionExport a = SessionExport.of("s1", "app", "agent", List.of(), null, Map.<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry>of());
        SessionExport b = SessionExport.of("s1", "app", "agent", List.of(), null, Map.<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry>of());
        // appId 差异显形
        SessionExport c = SessionExport.of("s1", "app2", "agent", List.of(), null, Map.<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry>of());
        SessionExportDiff.DiffReport report = SessionExportDiff.between(a, c);
        assertThat(report.identical()).isFalse();
        assertThat(report.fieldDiffs()).hasSize(1);
        assertThat(report.fieldDiffs().get(0).field()).isEqualTo("appId");
        assertThat(SessionExportDiff.between(a, b).identical()).isTrue();
    }

    @Test
    void messagesBucketedAddedRemovedChanged() {
        SessionExport a = SessionExport.of("s1", "app", "agent",
                List.of(msg("keep", Role.USER, "same"),
                        msg("gone", Role.USER, "bye"),
                        msg("chg", Role.USER, "old")), null, Map.of());
        SessionExport b = SessionExport.of("s1", "app", "agent",
                List.of(msg("keep", Role.USER, "same"),
                        msg("new", Role.USER, "welcome"),
                        msg("chg", Role.USER, "new")), null, Map.of());
        SessionExportDiff.DiffReport report = SessionExportDiff.between(a, b);
        assertThat(report.identical()).isFalse();
        assertThat(report.messageDiffs()).extracting(
                SessionExportDiff.MessageDiff::itemId, SessionExportDiff.MessageDiff::kind)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("gone", "removed"),
                        org.assertj.core.groups.Tuple.tuple("new", "added"),
                        org.assertj.core.groups.Tuple.tuple("chg", "changed"));
    }

    private static io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry entry(String key, String value) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(key, value, "test", 1, null, Instant.EPOCH);
    }

    @Test
    void stateAndExtensionDiffs() {
        SessionExport a = SessionExport.of("s1", "app", "agent", List.of(), null,
                Map.of("k1", entry("k1", "v1"), "k2", entry("k2", "old")));
        SessionExport b = SessionExport.of("s1", "app", "agent", List.of(), null,
                Map.of("k2", entry("k2", "new"), "k3", entry("k3", "v3")));
        SessionExportDiff.DiffReport report = SessionExportDiff.between(a, b);
        assertThat(report.stateDiffs()).hasSize(3);
        assertThat(report.stateDiffs()).extracting(SessionExportDiff.StateDiff::kind)
                .containsExactlyInAnyOrder("removed", "changed", "added");
    }

    @Test
    void crossSessionFailsFast() {
        SessionExport a = SessionExport.of("s1", "app", "agent", List.of(), null, Map.<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry>of());
        SessionExport b = SessionExport.of("s2", "app", "agent", List.of(), null, Map.<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry>of());
        assertThatThrownBy(() -> SessionExportDiff.between(a, b))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
