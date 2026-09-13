package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 738 / T1076–T1077：导出体积去向——消息/状态/扩展归因+占比/空导出/null。
 */
class SessionExportSizeAuditTest {

    @Test
    void attributesCharsBySegment() {
        SessionExport export = SessionExport.of("s1", "app", "agent",
                List.of(
                        new BuzhouMessage("m1", "s1", 0, 0, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                                "x".repeat(100), null, null, null, null, null, Instant.EPOCH),
                        new BuzhouMessage("m2", "s1", 0, 1, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.ASSISTANT,
                                "y".repeat(50), null, null, null, null, null, Instant.EPOCH)),
                null,
                Map.of("st", new StateEntry("st", "v".repeat(30), "p", 0, null, Instant.EPOCH)),
                Map.of("memory.facts", "z".repeat(200)));
        SessionExportSizeAudit.Report report = SessionExportSizeAudit.analyze(export);
        assertThat(report.totalChars()).isEqualTo(100 + 50 + 32 + 200 + 12);
        assertThat(report.segments().get(0).segment()).isEqualTo("ext:memory.facts"); // 200 最大
        assertThat(report.segments().get(0).chars()).isEqualTo(212);
        // 总占比守恒
        double shares = report.segments().stream().mapToDouble(SessionExportSizeAudit.Segment::share).sum();
        assertThat(shares).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void emptyExportAndNull() {
        SessionExport empty = SessionExport.of("s", "a", "g", List.of(), null, Map.of(), Map.of());
        SessionExportSizeAudit.Report report = SessionExportSizeAudit.analyze(empty);
        assertThat(report.totalChars()).isZero();
        assertThat(report.segments()).isEmpty();
        assertThatThrownBy(() -> SessionExportSizeAudit.analyze(null))
                .isInstanceOf(NullPointerException.class);
    }
}
