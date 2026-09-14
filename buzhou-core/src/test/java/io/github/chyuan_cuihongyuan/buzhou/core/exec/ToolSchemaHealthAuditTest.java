package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1435 / T2172：工具 schema 健康审计——四态分桶与校验器跳过条件
 * 严格同口径（非 object/三键全缺=裸奔）、findings 有界、bypassRatio 派生。
 */
class ToolSchemaHealthAuditTest {

    private static ToolCallback tool(String name, String schema) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                // 直接实现接口（builder 拒绝空 schema——而审计恰恰要能表达缺失态）
                return new ToolDefinition() {
                    @Override
                    public String name() {
                        return name;
                    }

                    @Override
                    public String description() {
                        return "d";
                    }

                    @Override
                    public String inputSchema() {
                        return schema;
                    }
                };
            }

            @Override
            public String call(String toolInput) {
                return "ok";
            }
        };
    }

    @Test
    void validSchemasCounted() {
        var r = ToolSchemaHealthAudit.analyze(List.of(
                tool("t1", "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}"),
                tool("t2", "{\"required\":[\"b\"]}"))); // 仅 required 有——校验器实际生效
        assertThat(r.totalTools()).isEqualTo(2);
        assertThat(r.valid()).isEqualTo(2);
        assertThat(r.bypassRatio()).isEqualTo(0.0d);
    }

    @Test
    void missingAndUnparseableBucketsed() {
        var r = ToolSchemaHealthAudit.analyze(List.of(
                tool("t-null", null),
                tool("t-blank", "  "),
                tool("t-bad", "{not json")));
        assertThat(r.missing()).isEqualTo(2);
        assertThat(r.unparseable()).isEqualTo(1);
        assertThat(r.findings()).hasSize(3);
        assertThat(r.bypassRatio()).isEqualTo(1.0d);
    }

    @Test
    void notObjectOrBareObjectIsBypass() {
        var r = ToolSchemaHealthAudit.analyze(List.of(
                tool("t-arr", "[1,2]"), // 非 object
                tool("t-bare", "{}"))); // 三键全缺——校验器跳过
        assertThat(r.notObject()).isEqualTo(2);
        assertThat(r.valid()).isZero();
    }

    @Test
    void findingsCappedAtCapacity() {
        var list = new java.util.ArrayList<ToolCallback>();
        for (int i = 0; i < ToolSchemaHealthAudit.FINDINGS_CAPACITY + 5; i++) {
            list.add(tool("t" + i, null));
        }
        var r = ToolSchemaHealthAudit.analyze(list);
        assertThat(r.missing()).isEqualTo(ToolSchemaHealthAudit.FINDINGS_CAPACITY + 5);
        assertThat(r.findings()).hasSize(ToolSchemaHealthAudit.FINDINGS_CAPACITY);
    }

    @Test
    void emptyInputYieldsSentinel() {
        var r = ToolSchemaHealthAudit.analyze(List.of());
        assertThat(r.totalTools()).isZero();
        assertThat(r.bypassRatio()).isEqualTo(-1d);
    }
}
