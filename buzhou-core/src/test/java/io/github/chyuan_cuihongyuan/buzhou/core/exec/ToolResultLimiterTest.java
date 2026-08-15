package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具结果尺寸防护单测（spec 31 / T110 / impl-85）：截断+提示尾、glob 豁免、
 * 默认档 read_range 豁免、禁用档、glob 匹配语义。
 */
class ToolResultLimiterTest {

    private static ToolResponseMessage.ToolResponse response(String tool, String data) {
        return new ToolResponseMessage.ToolResponse("call-1", tool, data);
    }

    /** 超限截断：保留头部 + 提示尾（模型可感知原始尺寸）。 */
    @Test
    void truncatesWithMarker() {
        ToolResultLimiter limiter = new ToolResultLimiter(10, Map.of());
        String big = "a".repeat(100);

        ToolResponseMessage.ToolResponse result = limiter.apply(response("run_query", big));

        assertThat(result.responseData()).startsWith("a".repeat(10));
        assertThat(result.responseData()).contains("结果已截断：原始 100 字符");
        assertThat(result.responseData()).contains("上限 10");
    }

    /** 限内原样（零拷贝语义——同一实例直接返回）。 */
    @Test
    void passesThroughWithinLimit() {
        ToolResultLimiter limiter = new ToolResultLimiter(100, Map.of());
        ToolResponseMessage.ToolResponse original = response("t", "short");

        assertThat(limiter.apply(original)).isSameAs(original);
    }

    /** glob 豁免与覆盖：read_range 默认豁免；前缀通配覆盖优先于默认。 */
    @Test
    void globOverridesAndDefaultExemptions() {
        ToolResultLimiter defaults = ToolResultLimiter.withDefaults();
        assertThat(defaults.limitFor("read_range")).isEqualTo(-1); // 默认豁免
        assertThat(defaults.limitFor("run_query"))
                .isEqualTo(ToolResultLimiter.DEFAULT_LIMIT_CHARS);

        ToolResultLimiter overridden = new ToolResultLimiter(1000,
                Map.of("mcp_*", 5000, "read_range", 100));
        assertThat(overridden.limitFor("mcp_db_query")).isEqualTo(5000);
        assertThat(overridden.limitFor("read_range")).isEqualTo(100); // 同键改值
        assertThat(overridden.limitFor("other")).isEqualTo(1000);
    }

    /** 禁用档：全部原样。 */
    @Test
    void disabledPassesEverything() {
        ToolResultLimiter disabled = ToolResultLimiter.disabled();
        ToolResponseMessage.ToolResponse original = response("t", "a".repeat(100));
        assertThat(disabled.apply(original)).isSameAs(original);
    }

    /** glob 语义：中缀/后缀通配与字面键。 */
    @Test
    void globMatchingSemantics() {
        assertThat(ToolResultLimiter.globMatch("read_*", "read_range")).isTrue();
        assertThat(ToolResultLimiter.globMatch("*_query", "mcp_db_query")).isTrue();
        assertThat(ToolResultLimiter.globMatch("mcp_*", "remote_query")).isFalse();
        assertThat(ToolResultLimiter.globMatch("read_range", "read_range")).isTrue();
        assertThat(ToolResultLimiter.globMatch("read_range", "read_range2")).isFalse();
    }
}
