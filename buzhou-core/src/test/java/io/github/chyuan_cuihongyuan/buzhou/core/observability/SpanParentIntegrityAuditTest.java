package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 736 / T1072–T1073：span 父链完整性——悬空父引用发现/根计数/
 * 空表/null fail-fast。
 */
class SpanParentIntegrityAuditTest {

    private static SpanRecord span(String id, String parent) {
        return new SpanRecord(id, parent, "sess", 0, "TOOL", "op",
                Instant.parse("2026-09-13T00:00:00Z"), Instant.now(), "OK", null);
    }

    @Test
    void danglingParentReferencesAreReported() {
        SpanParentIntegrityAudit.Report report = SpanParentIntegrityAudit.audit(List.of(
                span("root", null),
                span("child-a", "root"),
                span("child-b", "vanished"),   // 父不在集合——悬空
                span("grandchild", "child-a")));
        assertThat(report.totalSpans()).isEqualTo(4);
        assertThat(report.rootSpans()).isEqualTo(1);
        assertThat(report.findings()).hasSize(1);
        assertThat(report.findings().get(0).spanId()).isEqualTo("child-b");
        assertThat(report.findings().get(0).parentSpanId()).isEqualTo("vanished");
    }

    @Test
    void emptyAndNull() {
        assertThat(SpanParentIntegrityAudit.audit(List.of()).findings()).isEmpty();
        assertThatThrownBy(() -> SpanParentIntegrityAudit.audit(null))
                .isInstanceOf(NullPointerException.class);
    }
}
