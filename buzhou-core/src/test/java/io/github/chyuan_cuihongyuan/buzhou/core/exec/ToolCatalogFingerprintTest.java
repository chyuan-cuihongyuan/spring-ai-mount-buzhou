package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 175 / T544：目录指纹回归——稳定性 / 空白等价 / 三分类 / 描述不计 /
 * 空目录边界。
 */
class ToolCatalogFingerprintTest {

    private static ToolDefinition tool(String name, String schema) {
        return ToolDefinition.builder().name(name).description("d").inputSchema(schema).build();
    }

    @Test
    void sameCatalogBuildsIdenticalFingerprintAndSummary() {
        ToolCatalogFingerprint one = ToolCatalogFingerprint.of(List.of(
                tool("a", "{\"x\":1}"), tool("b", "{}")));
        ToolCatalogFingerprint two = ToolCatalogFingerprint.of(List.of(
                tool("b", "{}"), tool("a", "{\"x\":1}"))); // 顺序无关

        assertThat(one.summaryHex()).isEqualTo(two.summaryHex());
        assertThat(one.fingerprintOf("a")).isEqualTo(two.fingerprintOf("a"));
        assertThat(one.diff(two).isEmpty()).isTrue();
    }

    @Test
    void whitespaceSchemaDifferenceIsEquivalent() {
        ToolCatalogFingerprint tight = ToolCatalogFingerprint.of(
                List.of(tool("a", "{\"x\":1}")));
        ToolCatalogFingerprint spaced = ToolCatalogFingerprint.of(
                List.of(tool("a", "  {\"x\":1}  ")));

        assertThat(tight.summaryHex()).isEqualTo(spaced.summaryHex()); // strip 纪律
    }

    @Test
    void diffClassifiesAddedRemovedChanged() {
        ToolCatalogFingerprint before = ToolCatalogFingerprint.of(List.of(
                tool("keep", "{}"), tool("gone", "{}"), tool("mutated", "{\"v\":1}")));
        ToolCatalogFingerprint after = ToolCatalogFingerprint.of(List.of(
                tool("keep", "{}"), tool("mutated", "{\"v\":2}"), tool("fresh", "{}")));

        ToolCatalogFingerprint.Diff diff = before.diff(after);
        assertThat(diff.added()).containsExactly("fresh");
        assertThat(diff.removed()).containsExactly("gone");
        assertThat(diff.changed()).containsExactly("mutated");
        assertThat(diff.isEmpty()).isFalse();
    }

    @Test
    void descriptionChangeDoesNotCount() {
        // 描述是文档不是契约——diff 只比 name+schema
        ToolCatalogFingerprint before = ToolCatalogFingerprint.of(List.of(
                ToolDefinition.builder().name("a").description("旧描述")
                        .inputSchema("{}").build()));
        ToolCatalogFingerprint after = ToolCatalogFingerprint.of(List.of(
                ToolDefinition.builder().name("a").description("新描述")
                        .inputSchema("{}").build()));
        assertThat(before.diff(after).isEmpty()).isTrue();
        // 描述不进指纹——摘要也不变
        assertThat(before.summaryHex()).isEqualTo(after.summaryHex());
    }

    @Test
    void emptyAndNullCatalogs() {
        ToolCatalogFingerprint empty = ToolCatalogFingerprint.of(null);
        assertThat(empty.size()).isZero();
        assertThat(empty.summaryHex()).isNotBlank(); // 空目录也有锚
        assertThat(empty.diff(ToolCatalogFingerprint.of(List.of(tool("a", "{}"))))
                .added()).containsExactly("a");
        assertThat(ToolCatalogFingerprint.of(List.of(tool("a", "{}")))
                .diff(null).removed()).containsExactly("a");
    }

    @Test
    void fingerprintPerToolStableAcrossVersions() {
        String fp = ToolCatalogFingerprint.of(List.of(tool("a", "{}"))).fingerprintOf("a");
        assertThat(ToolCatalogFingerprint.of(List.of(tool("a", "{}"))).fingerprintOf("a"))
                .isEqualTo(fp);
        assertThat(ToolCatalogFingerprint.of(List.of()).fingerprintOf("a")).isNull();
    }
}
