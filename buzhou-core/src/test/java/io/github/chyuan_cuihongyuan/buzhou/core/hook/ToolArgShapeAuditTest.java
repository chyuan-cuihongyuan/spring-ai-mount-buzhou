package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1713 / T2628：ToolArgShapeAudit 直测——七态分类/计数/阈值。
 */
class ToolArgShapeAuditTest {

    @Test
    void classificationClosedSet() {
        var audit = new ToolArgShapeAudit();
        assertThat(audit.record(null)).isEqualTo(ToolArgShapeAudit.Shape.EMPTY);
        assertThat(audit.record("  ")).isEqualTo(ToolArgShapeAudit.Shape.EMPTY);
        assertThat(audit.record("{\"a\":1}")).isEqualTo(ToolArgShapeAudit.Shape.JSON_OBJECT);
        assertThat(audit.record("[1,2]")).isEqualTo(ToolArgShapeAudit.Shape.JSON_ARRAY);
        assertThat(audit.record("42")).isEqualTo(ToolArgShapeAudit.Shape.NUMERIC);
        assertThat(audit.record("-3.14e2")).isEqualTo(ToolArgShapeAudit.Shape.NUMERIC);
        assertThat(audit.record("TRUE")).isEqualTo(ToolArgShapeAudit.Shape.BOOLEAN);
        assertThat(audit.record("hello world")).isEqualTo(ToolArgShapeAudit.Shape.PLAIN_TEXT);
        assertThat(audit.total()).isEqualTo(8);
    }

    @Test
    void largeBlobOverridesOtherShapes() {
        var audit = new ToolArgShapeAudit(16);
        String blob = "{\"pad\":\"" + "x".repeat(32) + "\"}";
        assertThat(audit.record(blob)).isEqualTo(ToolArgShapeAudit.Shape.LARGE_BLOB);
        assertThat(audit.census()).containsEntry(ToolArgShapeAudit.Shape.LARGE_BLOB, 1L);
    }

    @Test
    void censusTalliesPerShape() {
        var audit = new ToolArgShapeAudit();
        audit.record("1");
        audit.record("2");
        audit.record("x");
        assertThat(audit.census()).containsEntry(ToolArgShapeAudit.Shape.NUMERIC, 2L)
                .containsEntry(ToolArgShapeAudit.Shape.PLAIN_TEXT, 1L);
        assertThat(audit.census()).containsEntry(ToolArgShapeAudit.Shape.EMPTY, 0L);
    }
}
