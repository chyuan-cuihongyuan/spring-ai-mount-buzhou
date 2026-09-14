package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1402 / T2106：MCP 工具入参 schema 破坏性变更分级——客户端守恒视角：
 * 加法演进 COMPATIBLE、删除/类型变更/新必填/枚举收窄 BREAKING、解析失败
 * fail-closed；原因清单稳定典序。
 */
class McpSchemaCompatGraderTest {

    private static final String BASE = """
            {"type":"object","properties":{
              "city":{"type":"string"},
              "days":{"type":"integer"},
              "unit":{"type":"string","enum":["celsius","fahrenheit"]}},
              "required":["city"]}
            """;

    @Test
    void identicalSchemaIsCompatible() {
        var verdict = McpSchemaCompatGrader.grade(BASE, BASE);
        assertThat(verdict.compatClass()).isEqualTo(McpSchemaCompatGrader.CompatClass.COMPATIBLE);
        assertThat(verdict.reasons()).isEmpty();
    }

    @Test
    void additiveOptionalPropertyIsCompatible() {
        String newer = """
                {"type":"object","properties":{
                  "city":{"type":"string"},
                  "days":{"type":"integer"},
                  "unit":{"type":"string","enum":["celsius","fahrenheit"]},
                  "lang":{"type":"string"}},
                  "required":["city"]}
                """;
        var verdict = McpSchemaCompatGrader.grade(BASE, newer);
        assertThat(verdict.compatClass()).isEqualTo(McpSchemaCompatGrader.CompatClass.COMPATIBLE);
    }

    @Test
    void removedPropertyIsBreaking() {
        String newer = """
                {"type":"object","properties":{
                  "city":{"type":"string"},
                  "days":{"type":"integer"}},
                  "required":["city"]}
                """;
        var verdict = McpSchemaCompatGrader.grade(BASE, newer);
        assertThat(verdict.compatClass()).isEqualTo(McpSchemaCompatGrader.CompatClass.BREAKING);
        assertThat(verdict.reasons()).containsExactly("removed_property:unit");
    }

    @Test
    void typeChangeIsBreaking() {
        String newer = """
                {"type":"object","properties":{
                  "city":{"type":"string"},
                  "days":{"type":"string"},
                  "unit":{"type":"string","enum":["celsius","fahrenheit"]}},
                  "required":["city"]}
                """;
        var verdict = McpSchemaCompatGrader.grade(BASE, newer);
        assertThat(verdict.reasons()).containsExactly("type_changed:days(integer->string)");
    }

    @Test
    void newlyRequiredExistingPropertyIsBreaking() {
        String newer = """
                {"type":"object","properties":{
                  "city":{"type":"string"},
                  "days":{"type":"integer"},
                  "unit":{"type":"string","enum":["celsius","fahrenheit"]}},
                  "required":["city","unit"]}
                """;
        var verdict = McpSchemaCompatGrader.grade(BASE, newer);
        assertThat(verdict.reasons()).containsExactly("newly_required:unit");
    }

    @Test
    void newPropertyRequiredFromStartIsBreaking() {
        String newer = """
                {"type":"object","properties":{
                  "city":{"type":"string"},
                  "days":{"type":"integer"},
                  "unit":{"type":"string","enum":["celsius","fahrenheit"]},
                  "key":{"type":"string"}},
                  "required":["city","key"]}
                """;
        var verdict = McpSchemaCompatGrader.grade(BASE, newer);
        assertThat(verdict.reasons()).containsExactly("newly_required:key");
    }

    @Test
    void enumNarrowedIsBreakingAndWidenedIsCompatible() {
        String narrowed = """
                {"type":"object","properties":{
                  "city":{"type":"string"},
                  "days":{"type":"integer"},
                  "unit":{"type":"string","enum":["celsius"]}},
                  "required":["city"]}
                """;
        assertThat(McpSchemaCompatGrader.grade(BASE, narrowed).reasons())
                .containsExactly("enum_narrowed:unit:fahrenheit");

        String widened = """
                {"type":"object","properties":{
                  "city":{"type":"string"},
                  "days":{"type":"integer"},
                  "unit":{"type":"string","enum":["celsius","fahrenheit","kelvin"]}},
                  "required":["city"]}
                """;
        assertThat(McpSchemaCompatGrader.grade(BASE, widened).compatClass())
                .isEqualTo(McpSchemaCompatGrader.CompatClass.COMPATIBLE);
    }

    @Test
    void unparseableSchemaFailsClosed() {
        var verdict = McpSchemaCompatGrader.grade(BASE, "{not-json");
        assertThat(verdict.compatClass()).isEqualTo(McpSchemaCompatGrader.CompatClass.BREAKING);
        assertThat(verdict.reasons()).containsExactly("unparseable_schema_fail_closed");
    }

    @Test
    void multipleReasonsAreSortedStable() {
        String newer = """
                {"type":"object","properties":{
                  "city":{"type":"integer"},
                  "days":{"type":"integer"}},
                  "required":["city","days"]}
                """;
        var verdict = McpSchemaCompatGrader.grade(BASE, newer);
        // unit 删除 + city 类型变更 + days 旧可选新必填——三原因典序
        assertThat(verdict.reasons()).containsExactly(
                "newly_required:days", "removed_property:unit",
                "type_changed:city(string->integer)");
    }
}
